package ru.coolsoft.vkfriends.loaders;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import java.util.HashMap;
import java.util.Map;

import ru.coolsoft.vkfriends.common.FriendsData;
import ru.coolsoft.vkfriends.R;
import ru.coolsoft.vkfriends.db.FriendsContract;
import ru.coolsoft.vkfriends.loaders.sources.ILoaderSource;

/**
 * Loads a list of friends for the user specified at construction
 */
public class FriendListLoader extends CursorLoader {
    public interface ICursorProvider{
        Cursor getCursor(String usersTableAlias, String... usersTableProjection);
    }
    public interface IProgressListener{
        void onProgressUpdate(int loaderId, int stageResourceId, long progress, long total);
        void onError(int loaderId, String errorMsg);
    }

    private ICursorProvider mCursorProvider;
    private ILoaderSource mSource;
    private final String[] mProjection;
    private final String mAlias;

    private IProgressListener mProgressListener;

    public FriendListLoader(Context context
            , String usersTableAlias, String... usersTableProjection) {
        super(context);
        mProjection = usersTableProjection;
        mAlias = usersTableAlias;
    }

    public void setProviders(ICursorProvider cursorProvider, ILoaderSource source){
        mCursorProvider = cursorProvider;
        mSource = source;
    }
    public void setProgressListener(IProgressListener progressListener){
        mProgressListener = progressListener;
    }

    private void publishProgress(int stageResourceId, long progress, long total){
        if (mProgressListener != null){
            mProgressListener.onProgressUpdate(getId(), stageResourceId, progress, total);
        }
    }

    private void publishError(String errorMsg) {
        if (mProgressListener != null){
            mProgressListener.onError(getId(), errorMsg);
        }
    }

    @Override
    public Cursor loadInBackground() {
        final String userId = mSource == null ? null : mSource.value();

        /*=== DEBUG DELAY ===*
        final int delayMax = 5;
        for (int i = 0; i < delayMax; i++){
            try {
                publishProgress(R.string.stage_delay, i, delayMax);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /*=== End of DELAY ===*/

        if (userId != null) {
            VKParameters params = (VKParameters) FriendsData.PARAMS_USER_DETAILS.clone();
            params.put(VKApiConst.USER_ID, userId);
            VKRequest getFriends = VKApi.friends().get(params);

            final VKUsersArray[] friends = new VKUsersArray[]{null};
            publishProgress(R.string.stage_downloading, 0, 1);

            getFriends.executeSyncWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    if (response.parsedModel instanceof VKUsersArray) {
                        friends[0] = (VKUsersArray) response.parsedModel;
                    }
                }

                @Override
                public void onProgress(VKRequest.VKProgressType progressType, long bytesLoaded, long bytesTotal) {
                    publishProgress(R.string.stage_downloading, bytesLoaded, bytesTotal);
                }

                @Override
                public void onError(VKError error) {
                    publishError(error.toString());
                }
            });

            //query current users
            publishProgress(R.string.stage_collecting, 0, 1);
            final String uAlias = "u";
            Cursor curFriends = FriendsData.getFriendsOf(userId, uAlias
                    , uAlias + "." + FriendsContract.Friends._ID
                    , uAlias + "." + FriendsContract.Users.COLUMN_USER_NAME + " || "
                            + uAlias + "." + FriendsContract.Users.COLUMN_USER_PHOTO200
            );
            Map<String, String> mapCurrentFriends = new HashMap<>();
            int index = 0;
            if (curFriends != null) {
                try {
                    final int count = curFriends.getCount();
                    if (count > 0) {
                        curFriends.moveToFirst();
                        do {
                            mapCurrentFriends.put(curFriends.getString(0), curFriends.getString(1));
                            publishProgress(R.string.stage_collecting, ++index, count);
                            curFriends.moveToNext();
                        } while (!curFriends.isLast());
                    }
                } finally {
                    curFriends.close();
                }
            }

            if (friends[0] != null) {
                //put changed friends' information into DB
                index = 0;
                //ToDo: update activity flag
                try {
                    FriendsData.beginTran();
                    for (VKApiUserFull user : friends[0]) {
                        publishProgress(R.string.stage_organizing, index++, friends[0].size());

                        final String strUserId = String.valueOf(user.id);
                        final boolean matches;
                        if (!mapCurrentFriends.containsKey(strUserId)) {
                            FriendsData.updateFriendData(userId, strUserId);
                            matches = false;
                        } else {
                            matches = mapCurrentFriends.get(strUserId).equals(user.toString() + user.photo_200);
                        }

                        if (!matches) {
                            FriendsData.updateUserData(user);
                        }
                    }
                    FriendsData.setTranSuccessful();
                } finally{
                    FriendsData.endTran();
                }
            }
        }

        return mCursorProvider == null ? null : mCursorProvider.getCursor(mAlias, mProjection);
    }
}
