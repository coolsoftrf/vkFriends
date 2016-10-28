package ru.coolsoft.vkfriends.fragments;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vk.sdk.api.model.VKApiUser;

import java.io.File;

import ru.coolsoft.vkfriends.FriendsData;
import ru.coolsoft.vkfriends.R;
import ru.coolsoft.vkfriends.db.FriendsContract;
import ru.coolsoft.vkfriends.loaders.FriendListLoader;
import ru.coolsoft.vkfriends.loaders.ImageLoader;
import ru.coolsoft.vkfriends.loaders.sources.ILoaderSource;
import ru.coolsoft.vkfriends.widget.SimpleRecyclerViewCursorAdapter;

/**
 * A fragment representing a list of friends to compare in main activity
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class FriendListFragment extends Fragment {
    private static final String ARG_ROOT_USER = "root-user";
    private static final String USERS_TABLE_ALIAS = "u";
    private OnListFragmentInteractionListener mListener;

    private VKApiUser mCurrentUser;
    Handler mHandler = new Handler();

    private ProgressBar mPhotoWaiter;
    private ImageView mAvatar;
    private Toolbar mToolbar;

    private TextView mStageName;
    private ProgressBar mStageProgress;
    private RelativeLayout mStageLayout;

    private SimpleRecyclerViewCursorAdapter mCursorAdapter;

    LoaderManager.LoaderCallbacks<String> mUserPhotoLoaderCallback = new LoaderManager.LoaderCallbacks<String>() {
        //ToDo: extract to a common Delegate for all the loadable images
        @Override
        public Loader<String> onCreateLoader(int id, Bundle args) {
            ImageLoader il = new ImageLoader(getActivity()
                    , new ILoaderSource() {
                        @Override
                        public String value() {
                            return FriendsData.getCurrentUser().photo_200;
                        }
                    }
            );
            il.setOnDownloadStartedListener(new ImageLoader.OnDownloadStartedListener() {
                @Override
                public void onDownloadStarted() {
                    mHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                mPhotoWaiter.setVisibility(View.VISIBLE);
                            }
                        }
                    );
                }
            });
            return  il;
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String photoFileName) {
            if (mAvatar != null) {
                if (photoFileName != null){
                    mAvatar.setImageURI(Uri.fromFile(new File(photoFileName)));
                } else {
                    mAvatar.setImageResource(R.drawable.blue_user_icon);
                }
            }
            mPhotoWaiter.setVisibility(View.GONE);
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
        }
    };

    LoaderManager.LoaderCallbacks<Cursor> mFriendlistLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new FriendListLoader(
                    getActivity()
                    , new ILoaderSource() {
                        @Override
                        public String value() {
                            return String.valueOf(mCurrentUser.id);
                        }
                    }
                    , new FriendListLoader.ICursorProvider() {
                        @Override
                        public Cursor getCursor(String userId, String[] projection) {
                            return FriendsData.getFriendsOf(userId, USERS_TABLE_ALIAS, projection);
                        }
                    }
                    , new FriendListLoader.IProgressListener() {
                        @Override
                        public void onProgressUpdate(final int stageResourceId, final long progress, final long total) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mStageName.setText(stageResourceId);

                                    mStageProgress.setProgress((int) (progress * mStageProgress.getMax() / total));
                                    //FixMe: progress layout doesn't show up at second and further fragment invocations
                                    mStageLayout.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }

                    , USERS_TABLE_ALIAS + "." + FriendsContract.Users._ID + " AS " + FriendsContract.Users._ID
                    , FriendsContract.Users.COLUMN_USER_NAME
                    , FriendsContract.Users.COLUMN_USER_PHOTO200
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            mCursorAdapter.changeCursor(cursor);
            mStageLayout.setVisibility(View.GONE);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FriendListFragment() {
    }

    @SuppressWarnings("unused")
    public static FriendListFragment newInstance(VKApiUser user) {
        FriendListFragment fragment = new FriendListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ROOT_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mCurrentUser = getArguments().getParcelable(ARG_ROOT_USER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        mPhotoWaiter = (ProgressBar) view.findViewById(R.id.user_photo_waiter);
        mAvatar = (ImageView) view.findViewById(R.id.user_image);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        if (mToolbar != null) {
            if (getActivity() instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.setSupportActionBar(mToolbar);

                ActionBar bar = activity.getSupportActionBar();
                if (bar != null) {
                    bar.setDisplayHomeAsUpEnabled(true);
                }
            }
        }

        mStageName = (TextView) view.findViewById(R.id.stage_name);
        mStageProgress = (ProgressBar) view.findViewById(R.id.stage_progress);
        mStageLayout = (RelativeLayout) view.findViewById(R.id.stage_layout);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        // Set the adapter
        if (recyclerView != null) {
            Context context = view.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            final String[][] from = {{
                    FriendsContract.Users._ID
                    , FriendsContract.Users.COLUMN_USER_NAME
                    , FriendsContract.Users.COLUMN_USER_PHOTO200
            }};
            final int[][] to = {{
                    R.id.friend_layout, R.id.friend_name, R.id.friend_photo
            }};

            mCursorAdapter = new SimpleRecyclerViewCursorAdapter(
                    null
                    , from, to
                    , new int[]{R.layout.fragment_user}
                    , new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Object tag = v.getTag();
                            if (tag != null && tag instanceof String) {
                                mListener.onListFragmentInteraction(getTag(), (String) tag);
                                getFragmentManager().popBackStack();
                            }
                        }
                    }
            );
            recyclerView.setAdapter(mCursorAdapter);
        }

        refreshTitle();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void refreshTitle() {
        if (mCurrentUser != null) {
            mToolbar.setSubtitle(mCurrentUser.fields.optString(FriendsData.FIELDS_NAME_GEN));

            //ToDo: extract to a common Delegate
            final LoaderManager lm = getActivity().getSupportLoaderManager();
            Loader ldr = lm.getLoader(FriendsData.LOADER_ID_WHOSE_PHOTO);

            //regardless whether we have the loader or not - reinitialize callbacks
            lm.initLoader(FriendsData.LOADER_ID_WHOSE_PHOTO, null, mUserPhotoLoaderCallback);

            //but restart loading if only we already had it before (have NOT just run it at initialization)
            if (ldr != null) {
                ldr.onContentChanged();
            }

            //start friend list loader
            ldr = lm.getLoader(FriendsData.LOADER_ID_FRIEND_LIST);
            lm.initLoader(FriendsData.LOADER_ID_FRIEND_LIST, null, mFriendlistLoaderCallback);
            if (ldr != null){
                ldr.onContentChanged();
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(String fragmentTag, String itemId);
    }
}
