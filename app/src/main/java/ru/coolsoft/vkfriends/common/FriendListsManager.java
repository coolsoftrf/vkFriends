package ru.coolsoft.vkfriends.common;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import ru.coolsoft.vkfriends.R;
import ru.coolsoft.vkfriends.db.FriendsContract;
import ru.coolsoft.vkfriends.loaders.FriendListLoader;
import ru.coolsoft.vkfriends.loaders.sources.ILoaderSource;

/**
 * Created by Bobby© on 06.11.2016.
 * Manages friendlist callback methods execution on different subscribers
 */
public class FriendListsManager
implements FriendListLoader.IProgressListener
, LoaderManager.LoaderCallbacks<Cursor>{

    public interface IViewProvider extends ILoaderSource, FriendListLoader.ICursorProvider{
        int getLoaderId();

        @Nullable Activity activity();
        @NonNull LoaderManager supportLoaderManager();
        @NonNull TextView stageName();
        @NonNull ProgressBar stageProgress();
        @NonNull View stageViewsParent();
        void doChangeCursor(Cursor cursor);
    }

    public static final int PROGRESS_TOTAL = 100;
    public static final String[][] FIELDS_FROM = {{
            FriendsContract.Users._ID //should go first for the {@link #updateImageView} to work properly
            , FriendsContract.Users.COLUMN_USER_NAME
            , FriendsContract.Users.COLUMN_USER_PHOTO200
    }};
    public static final int[][] VEWS_TO = {{
            R.id.friend_layout, R.id.friend_name, R.id.friend_photo
    }};
    public static final int[] SEARCH_FIELDS = {0 /*_id*/, 1/*name*/};

    private static FriendListsManager _instance;
    private static final String USERS_TABLE_ALIAS = "u";
    private SparseArray<Integer> mLastStageId;
    private SparseArray<Integer> mLastStagePercentage;

    private SparseArray<IViewProvider> mViewProviders = new SparseArray<>();

    private FriendListsManager(){
        mLastStageId = new SparseArray<>();
        mLastStagePercentage = new SparseArray<>();
    }

    public static FriendListsManager getInstance (@NonNull IViewProvider viewProvider){
        if (_instance == null){
            _instance = new FriendListsManager();
        }

        _instance.mViewProviders.put(viewProvider.getLoaderId(), viewProvider);
        return _instance;
    }

    public void handleStage(int loaderId){
        if (mLastStageId.get(loaderId) != null){
            onProgressUpdate(loaderId, mLastStageId.get(loaderId), mLastStagePercentage.get(loaderId), PROGRESS_TOTAL);
        }
    }

    public void updateList(int loaderId, boolean fullReload){
        final LoaderManager lm = mViewProviders.get(loaderId).supportLoaderManager();
        Loader ldr = lm.getLoader(loaderId);
        lm.initLoader(loaderId, null, this);
        if (ldr != null){
            ((FriendListLoader)ldr).registerProgressListener(this);
            if (fullReload) {
                ldr.onContentChanged();
            }
        }
    }

//IProgressListener implementation
    @Override
    public void onProgressUpdate(final int loaderId, final int stageResourceId, long progress, long total) {
        final int percentage = (int) (progress * PROGRESS_TOTAL / total);

        if (Integer.valueOf(percentage).equals(mLastStagePercentage.get(loaderId))
                && Integer.valueOf(stageResourceId).equals(mLastStageId.get(loaderId))) {
            return;
        }

        mLastStageId.put(loaderId, stageResourceId);
        mLastStagePercentage.put(loaderId, percentage);

        //ToDo: Extract into a parametrized handler
        /*^*/
        final Activity activity = mViewProviders.get(loaderId).activity();
        if (activity != null)
            activity.runOnUiThread(
        /*/
            mHandler.post(
        /*$*/
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                CharSequence text = activity.getText(stageResourceId);
                                mViewProviders.get(loaderId).stageName().setText(text);

                                mViewProviders.get(loaderId).stageProgress().setProgress(percentage);
                                mViewProviders.get(loaderId).stageViewsParent().setVisibility(View.VISIBLE);
                            } catch (IllegalStateException e) {
                                Log.w("FLF:onProgressUpdate", "Activity detached unexpectedly", e);
                            }
                        }
                    }
            );
    }

    @Override
    public void onError(int loaderId, final String errorMsg) {
        final Activity activity = mViewProviders.get(loaderId).activity();
        if (activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
    }

//LoaderCallbacks
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        FriendListLoader fll = new FriendListLoader(
                mViewProviders.get(id).activity()
                , mViewProviders.get(id)
                , mViewProviders.get(id)

                , USERS_TABLE_ALIAS
                , USERS_TABLE_ALIAS + "." + FriendsContract.Users._ID + " AS " + FriendsContract.Users._ID
                , FriendsContract.Users.COLUMN_USER_NAME
                , FriendsContract.Users.COLUMN_USER_PHOTO200
        );
        fll.registerProgressListener(this);
        return fll;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int id = loader.getId();
        mLastStageId.remove(id);
        mLastStagePercentage.remove(id);

        mViewProviders.get(id).doChangeCursor(cursor);
        mViewProviders.get(id).stageViewsParent().setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

}
