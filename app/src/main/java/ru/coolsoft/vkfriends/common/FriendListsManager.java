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
import ru.coolsoft.vkfriends.VKFApplication;
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
        @Nullable TextView amount();
        void doChangeCursor(Cursor cursor);
        void resetScroll();
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

    public void updateList(int loaderId, boolean fullReload){
        final LoaderManager lm = mViewProviders.get(loaderId).supportLoaderManager();

        final Loader ldr = lm.getLoader(loaderId);
        lm.initLoader(loaderId, null, this);
        if (ldr != null){
            final IViewProvider provider = mViewProviders.get(loaderId);
            ((FriendListLoader)ldr).setProviders(provider, provider);
            if (fullReload) {
                ldr.onContentChanged();
            }
        }
    }

//IProgressListener implementation
    @Override
    public void onProgressUpdate(final int loaderId, final int stageResourceId, long progress, final long total) {
        final int percentage;
        if (total != 0) {
            percentage = (int) (progress * PROGRESS_TOTAL / total);
        } else {
            percentage = 0;
        }
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
                                if (stageResourceId == 0){
                                    //hide progress and reset recycler view
                                    mViewProviders.get(loaderId).stageViewsParent().setVisibility(View.GONE);
                                    mViewProviders.get(loaderId).resetScroll();

                                    final TextView amount = mViewProviders.get(loaderId).amount();
                                    if (amount != null && total > 0) {
                                        amount.setText(String.format(
                                                VKFApplication.app().getString(R.string.amount_found)
                                                , total));
                                    }
                                } else {
                                    CharSequence text = activity.getText(stageResourceId);
                                    mViewProviders.get(loaderId).stageName().setText(text);

                                    mViewProviders.get(loaderId).stageProgress().setProgress(percentage);
                                    mViewProviders.get(loaderId).stageViewsParent().setVisibility(View.VISIBLE);
                                }
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
        final FriendListLoader fll = new FriendListLoader(
                mViewProviders.get(id).activity()
                , USERS_TABLE_ALIAS
                , USERS_TABLE_ALIAS + "." + FriendsContract.Users._ID + " AS " + FriendsContract.Users._ID
                , FriendsContract.Users.COLUMN_USER_NAME
                , FriendsContract.Users.COLUMN_USER_PHOTO200
        );

        setLoaderProviders(id, fll);
        fll.setProgressListener(this);
        return fll;
    }

    private void setLoaderProviders(int id, FriendListLoader fll) {
        final IViewProvider provider = mViewProviders.get(id);
        fll.setProviders(provider, provider);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int id = loader.getId();
        mLastStageId.remove(id);
        mLastStagePercentage.remove(id);

        mViewProviders.get(id).doChangeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

}
