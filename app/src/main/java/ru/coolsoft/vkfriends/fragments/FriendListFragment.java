package ru.coolsoft.vkfriends.fragments;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
//import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vk.sdk.api.model.VKApiUser;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Stack;

import ru.coolsoft.vkfriends.VKFApplication;
import ru.coolsoft.vkfriends.common.AdapterImageManagementDelegate;
import ru.coolsoft.vkfriends.common.FriendListsManager;
import ru.coolsoft.vkfriends.common.FriendsData;
import ru.coolsoft.vkfriends.R;
import ru.coolsoft.vkfriends.loaders.ImageLoader;
import ru.coolsoft.vkfriends.loaders.sources.ILoaderSource;
import ru.coolsoft.vkfriends.widget.FilterableRecyclerViewCursorAdapter;

/**
 * A fragment representing a list of friends to compare in main activity
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class FriendListFragment
extends Fragment
implements SearchView.OnQueryTextListener{
    private static final String ARG_ROOT_USER = "root-user";

    //Instance state bundle keys
    private static final String KEY_PHOTO_PROGRESS = "photo-progress";
    private static final String KEY_USER_STACK = "user-stack";

    //loader argument keys
    private static final String KEY_PHOTO = "key_photo";

    //current state fields
    private VKApiUser mCurrentUser;
    private Stack<VKApiUser> mUserStack;
    private boolean mLastPhotoProgress = false;
    private SparseArray<WeakReference<ImageView>> mFriendlistPhotos = new SparseArray<>();

    //private Handler mHandler = new Handler();

    //view references
    private ProgressBar mPhotoWaiter;
    private ImageView mAvatar;
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;

    private TextView mStageName;
    private ProgressBar mStageProgress;
    private View mStageLayout;
    private TextView mAmount;

    //references to worker objects
    private FilterableRecyclerViewCursorAdapter mCursorAdapter;
    private OnListFragmentInteractionListener mListener;
    private boolean mFullReload = true;

    /////////////////////// CALLBACKS AND LISTENERS ///////////////////////
    private final FriendListsManager.IViewProvider mViewProvider = new FriendListsManager.IViewProvider() {

        @Override
        public Cursor getCursor(String usersTableAlias, String... projection) {
            String value = value();
            if (value == null){
                //the following string may be used in DB to supply stub data
                // to display when there is no user registered
                value = "";
            }
            return FriendsData.getFriendsOf(value, usersTableAlias, projection);
        }

        @Override
        public int getLoaderId() {
            return FriendsData.LOADER_ID_FRIEND_LIST;
        }

        @Override
        public @Nullable
        Activity activity() {
            return getActivity();
        }

        @NonNull
        @Override
        public LoaderManager supportLoaderManager() {
            return getActivity().getSupportLoaderManager();
        }

        @Override
        public @NonNull TextView stageName() {
            return mStageName;
        }

        @Override
        public @NonNull ProgressBar stageProgress() {
            return mStageProgress;
        }

        @Override
        public @NonNull View stageViewsParent() {
            return mStageLayout;
        }

        @Override
        public @NonNull TextView amount() {
            return mAmount;
        }

        @Override
        public void doChangeCursor(Cursor cursor) {
            mCursorAdapter.changeCursor(cursor);
        }

        @Override
        public void resetScroll() {
            mRecyclerView.scrollToPosition(0);
        }

        //ILoaderSource override
        @Override
        public String value(int... index) {
            return mCurrentUser == null ? null : String.valueOf(mCurrentUser.id);
        }
    };

    private final ImageLoader.OnDownloadStartedListener mDownloadStartedListener = new ImageLoader.OnDownloadStartedListener() {
        @Override
        public void onDownloadStarted(int id) {
            if(id == FriendsData.LOADER_ID_WHOSE_PHOTO) {
                mLastPhotoProgress = true;

            /*^*/
                FragmentActivity activity = getActivity();
                if (activity != null)
                    activity.runOnUiThread(
            /*/
                mHandler.post(
            /*$*/
                        new Runnable() {
                            @Override
                            public void run() {
                                mPhotoWaiter.setVisibility(View.VISIBLE);
                            }
                        }
                    );
            }
        }
    };
    private final ILoaderSource mWhoseImageSource = new ILoaderSource() {
        @Override
        public String value(int... index) {
            return mCurrentUser.photo_200;
        }
    };
    private LoaderManager.LoaderCallbacks<String> mUserPhotoLoaderCallback = new LoaderManager.LoaderCallbacks<String>() {
        //ToDo: extract to a common Delegate for all the loadable images
        @Override
        public Loader<String> onCreateLoader(int id, final Bundle args) {
            ImageLoader il = new ImageLoader(getActivity());
            setupLoader(il, id, args);
            return  il;
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String photoFileName) {
            final ImageView imageView;
            if (loader.getId() == FriendsData.LOADER_ID_WHOSE_PHOTO) {
                mLastPhotoProgress = false;
                imageView = mAvatar;
            } else {
                WeakReference<ImageView> refPhoto = mFriendlistPhotos.get(loader.getId());
                if (refPhoto != null) {
                    imageView = refPhoto.get();
                } else {
                    imageView = null;
                }
            }

            if (imageView != null) {
                if (photoFileName != null) {
                    imageView.setImageURI(Uri.fromFile(new File(photoFileName)));
                } else {
                    imageView.setImageResource(R.drawable.blue_user_icon);
                }
            }
            mPhotoWaiter.setVisibility(View.GONE);
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {}
    };

    /////////////////////// CONSTRUCTORS ///////////////////////
    public static FriendListFragment newInstance(VKApiUser user) {
        FriendListFragment fragment = new FriendListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ROOT_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FriendListFragment() {}

    /////////////////////// PUBLIC METHODS ///////////////////////

    public boolean popUser(){
        if (mUserStack == null || mUserStack.isEmpty()){
            return false;
        }

        mCurrentUser = mUserStack.pop();
        getArguments().putParcelable(ARG_ROOT_USER, mCurrentUser);
        refresh(true);
        return true;
    }

    /////////////////////// FRAGMENT OVERRIDES ///////////////////////

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mCurrentUser = getArguments().getParcelable(ARG_ROOT_USER);
        }
        if (savedInstanceState != null) {
            mLastPhotoProgress = savedInstanceState.getBoolean(KEY_PHOTO_PROGRESS, false);

            mUserStack = new Stack<>();
            Parcelable[] users = savedInstanceState.getParcelableArray(KEY_USER_STACK);
            if (users != null) {
                for (Parcelable user : users) {
                    mUserStack.push((VKApiUser) user);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        mPhotoWaiter = (ProgressBar) view.findViewById(R.id.user_photo_waiter);
        if (mLastPhotoProgress){
            mPhotoWaiter.setVisibility(View.VISIBLE);
        }
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
        mStageLayout = view.findViewById(R.id.stage_layout);
        mAmount = (TextView) view.findViewById(R.id.amounts);

        setHasOptionsMenu(true);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        // Set the adapter
        if (mRecyclerView != null) {
            final AdapterImageManagementDelegate delegate = new AdapterImageManagementDelegate() {
                @Override
                public void addImageView(int key, ImageView view) {
                    mFriendlistPhotos.put(key, new WeakReference<>(view));
                }

                @Override
                public void recycleImageView(int key) {
                    mFriendlistPhotos.remove(key);
                }

                @Override
                public void refreshPhoto(int loaderId, Bundle args) {
                    refreshUserPhoto(loaderId, args, false);
                }

                @Override
                public void updateStaticViews(View container) {
                    final View findFriends = container.findViewById(R.id.find_friends);
                    if (findFriends != null){
                        final Object tag = container.getTag();
                        if (tag != null && tag instanceof String){
                            findFriends.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mUserStack == null){
                                        mUserStack = new Stack<>();
                                    }
                                    mUserStack.push(mCurrentUser);
                                    mCurrentUser = FriendsData.getUser((String)tag);
                                    getArguments().putParcelable(ARG_ROOT_USER, mCurrentUser);
                                    refresh(true);
                                }
                            });
                            findFriends.setVisibility(View.VISIBLE);
                        }
                    }

                    final View showProfile = container.findViewById(R.id.showProfile);
                    if (showProfile != null){
                        final Object tag = container.getTag();
                        if (tag != null && tag instanceof String){
                            showProfile.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    VKFApplication.showUserProfile((String)tag);
                                }
                            });
                            showProfile.setVisibility(View.VISIBLE);
                        }
                    }
                }
            };
            mCursorAdapter = new FilterableRecyclerViewCursorAdapter(null
                    , FriendListsManager.FIELDS_FROM, FriendListsManager.VEWS_TO, FriendListsManager.SEARCH_FIELDS
                    , delegate
                    , R.layout.friendlist_user
            ){
                @Override
                public void onClick(View v) {
                    Object tag = v.getTag();
                    if (tag != null && tag instanceof String) {
                        mListener.onListFragmentInteraction(getTag(), (String) tag);
                        getFragmentManager().popBackStack();
                    }
                }
            };
            mRecyclerView.setAdapter(mCursorAdapter);
        }

        mFullReload = savedInstanceState == null;
        refresh(mFullReload);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList(mFullReload);

        //ToDo: either change to {@code true} or invent another way to keep data actual
        //once there comes more that one host of friends
        mFullReload = false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.friendlist_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);


        if (mLastPhotoProgress){
            outState.putBoolean(KEY_PHOTO_PROGRESS, true);
        }
        if (mUserStack != null && !mUserStack.isEmpty()){
            VKApiUser[] users = new VKApiUser[mUserStack.size()];
            outState.putParcelableArray(KEY_USER_STACK, mUserStack.toArray(users));
        }
    }

    @Override
    public void onStop() {
        InputMethodManager imm = (InputMethodManager) VKFApplication.app().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getView();
        if ( view != null ) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /////////////////////// HELPER METHODS ///////////////////////

    private void refresh(boolean reload) {
        if (mCurrentUser != null) {
            mToolbar.setSubtitle(mCurrentUser.fields.optString(FriendsData.FIELDS_NAME_GEN));

            //ToDo: extract to a common delegate
            refreshUserPhoto(FriendsData.LOADER_ID_WHOSE_PHOTO, null, reload);

            //start friend list loader
            refreshList(reload);
        }
    }

    private void refreshList(boolean reload) {
        FriendListsManager.getInstance(mViewProvider).updateList(FriendsData.LOADER_ID_FRIEND_LIST, reload);
    }

    private void refreshUserPhoto(int loaderId, Bundle args, boolean reload) {
        LoaderManager lm = getActivity().getSupportLoaderManager();
        Loader ldr = lm.getLoader(loaderId);

        //regardless whether we have the loader or not - reinitialize callbacks
        lm.initLoader(loaderId, args, mUserPhotoLoaderCallback);

        //but restart loading if only we already had it before (have NOT just run it at initialization)
        if (ldr != null) {
            setupLoader((ImageLoader) ldr, loaderId, args);
            if (reload) {
                ldr.onContentChanged();
            }
        }
    }

    private void setupLoader(ImageLoader il, int id, final Bundle args){
        if (id == FriendsData.LOADER_ID_WHOSE_PHOTO) {
            il.setLoaderSource(mWhoseImageSource);
            il.setOnDownloadStartedListener(mDownloadStartedListener);
        } else {
            il.setLoaderSource(new ILoaderSource() {
                @Override
                public String value(int... index) {
                    return args.getString(KEY_PHOTO);
                }
            });
        }
    }

    /////////////////////// INTERFACE IMPLEMENTATIONS ///////////////////////

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mCursorAdapter.filter(newText);
        return true;
    }

    /////////////////////// INTERFACE DECLARATIONS ///////////////////////
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
