package ru.coolsoft.vkfriends;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Bundle;
//import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

import android.net.Uri;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import ru.coolsoft.vkfriends.common.AdapterImageManagementDelegate;
import ru.coolsoft.vkfriends.common.FriendListsManager;
import ru.coolsoft.vkfriends.common.FriendsData;
import ru.coolsoft.vkfriends.fragments.FriendListFragment;
import ru.coolsoft.vkfriends.loaders.ImageLoader;
import ru.coolsoft.vkfriends.loaders.sources.ILoaderSource;
import ru.coolsoft.vkfriends.loaders.sources.SharedPreferencesSource;
import ru.coolsoft.vkfriends.widget.SimpleRecyclerViewCursorAdapter;

public class MainActivity extends AppCompatActivity
implements AppBarLayout.OnOffsetChangedListener
        , NavigationView.OnNavigationItemSelectedListener
        , SharedPreferences.OnSharedPreferenceChangeListener
        , View.OnClickListener
        , FriendListFragment.OnListFragmentInteractionListener
{
    private final static String TAG = MainActivity.class.getSimpleName();

    @IntDef(value = {FLAG_LEFT, FLAG_RIGHT},
            flag = true)
    @Retention(RetentionPolicy.SOURCE)
    @interface Side{}
    private final static int FLAG_LEFT = 1;
    private final static int FLAG_RIGHT = 2;

    @IntDef({View.VISIBLE, View.INVISIBLE, View.GONE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Visibility{}

    //main view controls
    private FrameLayout mFl;
    private TextView mContactNameLeft;
    private TextView mContactNameRight;
    private ImageView mContactImageLeft;
    private ImageView mContactImageRight;
    private Space mSpace1;
    private Space mSpace2;

    //stage indicators
    private TextView mStageName;
    private ProgressBar mStageProgress;
    private View mStageParent;
    private TextView mAmount;

    //navigation controls
    private NavigationView mNavView;
    private ImageView mAvatar;
    private ProgressBar mWaiter;
    private ProgressBar mPhotoWaiter;
    private ProgressBar mLeftPhotoWaiter;
    private ProgressBar mRightPhotoWaiter;

    //measuring parameters
    private int mTextSizeStart;
    private int mTextSizeEnd;
    private int mAppBarHeight;

    //list controls
    private RecyclerView mRecyclerView;
    private SimpleRecyclerViewCursorAdapter mAdapter;
    private SparseArray<WeakReference<ImageView>> mFriendlistPhotos = new SparseArray<>();
    private boolean mFullReload = true;

    ////////////////////// callback handlers //////////////////////
    //Handler mDelayHandler = new Handler();
    private VKCallback<VKAccessToken> mVKAuthCallback = new VKCallback<VKAccessToken>() {
        @Override
        public void onResult(VKAccessToken res) {
            // Пользователь успешно авторизовался
            res.saveTokenToSharedPreferences(MainActivity.this, VKFApplication.PREF_KEY_ACCESS_TOKEN);
        }
        @Override
        public void onError(VKError error) {
            // Произошла ошибка авторизации (например, пользователь запретил авторизацию)
        }
    };
    private VKRequest.VKRequestListener mVKCurrentUserRequestListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {
            Log.i(TAG, "USER data retrieved");

            if (response.parsedModel instanceof VKList){
                Object o = ((VKList)response.parsedModel).get(0);
                if (o instanceof VKApiUser){
                    VKApiUser me = (VKApiUser) o;
                    FriendsData.setCurrentUser (me);
                    final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

                    /*=== DEBUG DELAY ===*/
                    /**
                    final boolean posted = mDelayHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {/**/
                    SharedPreferences.Editor editor = null;
                    //Log.d(TAG, "Updating USERNAME preference");
                    final String name = sp.getString(VKFApplication.PREF_KEY_USERNAME, null);
                    final String newName = me.toString();
                    if (newName.equals(name)){
                        refreshNavigationView();
                    } else {
                        editor = sp.edit();
                        editor.putString(VKFApplication.PREF_KEY_USERNAME, newName);
                    }

                    //Log.d(TAG, "Updating USERPHOTO preference");
                    final String photo = sp.getString(VKFApplication.PREF_KEY_USERPHOTO, null);
                    final String newPhoto = me.photo_200;
                    if (!newPhoto.equals(photo)){
                        if (editor == null){
                            editor = sp.edit();
                        }
                        editor.putString(VKFApplication.PREF_KEY_USERPHOTO, newPhoto);
                    }

                    if (editor != null){
                        editor.apply();
                    }

                    FriendsData.updateUserData(me);
                    mWaiter.setVisibility(View.GONE);
                            /**}
                        }
                        , 5000
                    );/**/
                    //Log.d(TAG, "update posted = " + posted);
                }
            }

            super.onComplete(response);
        }

        @Override
        public void onError(VKError error) {
            Log.w(TAG, "Error retrieving USER data: " + error.errorMessage);

            //try offline mode
            VKAccessToken token = VKAccessToken.tokenFromSharedPreferences(MainActivity.this, VKFApplication.PREF_KEY_ACCESS_TOKEN);
            if (token != null) {
                FriendsData.setCurrentUser(FriendsData.getUser(token.userId));
            }

            refreshNavigationView();
            mWaiter.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
        }
    };

    private ImageLoader.OnDownloadStartedListener mDownloadStartedListener = new ImageLoader.OnDownloadStartedListener() {
        @Override
        public void onDownloadStarted(final int id) {
            setWaiterVisibilityByLoaderId(id, View.VISIBLE);
        }
    };
    private LoaderManager.LoaderCallbacks<String> mUserPhotoLoaderCallback = new LoaderManager.LoaderCallbacks<String>() {
        @Override
        public Loader<String> onCreateLoader(int id, final Bundle args) {
            final VKApiUser user;
            VKApiUser tmpUser = null;
            final ILoaderSource src;
            switch(id) {
                case FriendsData.LOADER_ID_USER_PHOTO:
                    src = new SharedPreferencesSource(
                            PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                            , VKFApplication.PREF_KEY_USERPHOTO
                    );
                    break;
                case FriendsData.LOADER_ID_LEFT_USER_PHOTO:
                    tmpUser = FriendsData.getLeftUser();
                case FriendsData.LOADER_ID_RIGHT_USER_PHOTO:
                    if (tmpUser == null){
                        tmpUser = FriendsData.getRightUser();
                    }
                    user = tmpUser;

                    src = new ILoaderSource() {
                        @Override
                        public String value(int... index) {
                            return user.photo_200;
                        }
                    };
                    break;
                default:
                    src = new ILoaderSource() {
                        @Override
                        public String value(int... index) {
                            return args.getString(AdapterImageManagementDelegate.KEY_PHOTO);
                        }
                    };
            }

            ImageLoader il = new ImageLoader(MainActivity.this);
            il.setLoaderSource(src);
            il.setOnDownloadStartedListener(mDownloadStartedListener);
            return  il;
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String photoFileName) {
            final int loaderId = loader.getId();
            final ImageView imageView;
            switch (loaderId){
                case FriendsData.LOADER_ID_USER_PHOTO:
                    imageView = mAvatar; break;
                case FriendsData.LOADER_ID_LEFT_USER_PHOTO:
                    imageView = mContactImageLeft; break;
                case FriendsData.LOADER_ID_RIGHT_USER_PHOTO:
                    imageView = mContactImageRight; break;
                default:
                    WeakReference<ImageView> refIV = mFriendlistPhotos.get(loader.getId());
                    if (refIV != null) {
                        imageView = refIV.get();
                    } else {
                        imageView = null;
                    }
            }
            if (imageView != null) {
                if (photoFileName != null){
                    imageView.setImageURI(Uri.fromFile(new File(photoFileName)));
                } else {
                    imageView.setImageResource(R.drawable.blue_user_icon);
                }
            }
            setWaiterVisibilityByLoaderId(loaderId, View.GONE);
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
            Log.w(TAG, "image loader reset");
        }
    };

    private class ViewProvider implements FriendListsManager.IViewProvider {
        private String userId;

        @Override
        public int getLoaderId() {
            return FriendsData.LOADER_ID_COMMON_FRIENDS_LIST;
        }

        @Nullable
        @Override
        public Activity activity() {
            return MainActivity.this;
        }

        @NonNull
        @Override
        public LoaderManager supportLoaderManager() {
            return getSupportLoaderManager();
        }

        @NonNull
        @Override
        public TextView stageName() {
            return mStageName;
        }

        @NonNull
        @Override
        public ProgressBar stageProgress() {
            return mStageProgress;
        }

        @NonNull
        @Override
        public View stageViewsParent() {
            return mStageParent;
        }

        @NonNull
        @Override
        public TextView amount() {
            return mAmount;
        }

        @Override
        public void doChangeCursor(Cursor cursor) {
            mAdapter.changeCursor(cursor);
        }

        @Override
        public void resetScroll() {
            mRecyclerView.scrollToPosition(0);
        }

        @Override
        public Cursor getCursor(String usersTableAlias, String... usersTableProjection) {
            return FriendsData.getCommonFriendsOf(
                    value(0), value(1)
                    , usersTableAlias, usersTableProjection
            );
        }

        //ToDo: make {@code value(void)} return Object matching the method delegate in getCursor
        @Override
        public String value(int... index) {
            if (index == null || index.length == 0){
                return userId;
            }

            final VKApiUser user;
            switch (index[0]){
                case 0:
                    user = FriendsData.getLeftUser();
                    break;
                case 1:
                    user = FriendsData.getRightUser();
                    break;
                default:
                    return null;
            }
            return String.valueOf(user != null ? user.id : 0);
        }
    }

    private ViewProvider mViewProvider = new ViewProvider();

    ////////////////////// Activity overrides //////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initParams();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        mFullReload = savedInstanceState == null;
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        FriendListsManager.getInstance(mViewProvider).updateList(FriendsData.LOADER_ID_COMMON_FRIENDS_LIST, mFullReload);
        //for some magic reason the data that was loaded but not reported to UI is not kept in Loader
        //so we need to start full reload next time we resume activity (e. g. from minimized state)
        mFullReload = true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        FriendListFragment flf = (FriendListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.container);
        if (flf == null || !flf.popUser()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, mVKAuthCallback)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    ////////////////////// private helper methods //////////////////////
    private ProgressBar findWaiterByLoaderId(int id){
        switch (id) {
            case FriendsData.LOADER_ID_USER_PHOTO:
                return mPhotoWaiter;
            case FriendsData.LOADER_ID_LEFT_USER_PHOTO:
                return mLeftPhotoWaiter;
            case FriendsData.LOADER_ID_RIGHT_USER_PHOTO:
                return mRightPhotoWaiter;
            default:
                return null;
        }
    }

    private void setWaiterVisibilityByLoaderId(int id, @Visibility int visibility) {
        ProgressBar waiter = findWaiterByLoaderId(id);
        if (waiter != null) {
            waiter.setVisibility(visibility);
        }
    }

    private void initParams(){
        TypedArray attr = getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        mAppBarHeight = attr.getDimensionPixelSize(0, 0);
        attr.recycle();

        mTextSizeStart = getResources().getDimensionPixelSize(R.dimen.text_size);
        mTextSizeEnd = getResources().getDimensionPixelSize(R.dimen.text_size_small);
    }

    private void initViews() {
        //navigation view controls
        ImageView anchor = (ImageView) findViewById(R.id.padding);
        if (anchor!= null) {
            ViewGroup.LayoutParams lp = anchor.getLayoutParams();
            lp.height = mAppBarHeight * 3;
        }

        AppBarLayout appBar = (AppBarLayout) findViewById(R.id.app_bar);
        if (appBar != null) {
            appBar.addOnOffsetChangedListener(this);
        }

        mNavView = (NavigationView) findViewById(R.id.navigation);
        mWaiter = (ProgressBar) findViewById(R.id.waiter);
        if(mNavView != null) {
            mNavView.setNavigationItemSelectedListener(this);
            if (VKFApplication.app().isInitialized()){
                refreshNavigationView();
            } else {
                getSupportLoaderManager().initLoader(FriendsData.LOADER_ID_USER_PHOTO, null, mUserPhotoLoaderCallback);
                requestAccountDetails();
                VKFApplication.app().setInitialized();
            }

            mAvatar = (ImageView) mNavView.getHeaderView(0).findViewById(R.id.avatar);
            mPhotoWaiter = (ProgressBar) mNavView.getHeaderView(0).findViewById(R.id.photo_waiter);
        }

        //title area controls
        mFl = (FrameLayout) findViewById(R.id.title);

        mContactNameLeft = (TextView) findViewById(R.id.name1);
        if (mContactNameLeft != null) {
            mContactNameLeft.setOnClickListener(this);
        }
        mContactNameRight = (TextView) findViewById(R.id.name2);
        if (mContactNameRight != null) {
            mContactNameRight.setOnClickListener(this);
        }
        mContactImageLeft = (ImageView) findViewById(R.id.avatar1);
        if (mContactImageLeft != null) {
            mContactImageLeft.setOnClickListener(this);
        }
        mContactImageRight = (ImageView) findViewById(R.id.avatar2);
        if (mContactImageRight != null) {
            mContactImageRight.setOnClickListener(this);
        }
        mLeftPhotoWaiter = (ProgressBar) findViewById(R.id.waiter_left);
        mRightPhotoWaiter = (ProgressBar) findViewById(R.id.waiter_right);
        updateUsers(FLAG_LEFT | FLAG_RIGHT);

        mSpace1 = (Space) findViewById(R.id.spaceAvatar1);
        mSpace2 = (Space) findViewById(R.id.spaceAvatar2);

        //list management
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
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
            };
            mAdapter = new SimpleRecyclerViewCursorAdapter(null
                    , FriendListsManager.FIELDS_FROM, FriendListsManager.VEWS_TO
                    , delegate
                    , R.layout.friendlist_user){

                @Override
                public void onClick(View v) {
                    final Object tag =  v.getTag();
                    if (tag != null && tag instanceof String) {
                        VKFApplication.showUserProfile((String)tag);
                    }
                }
            };
            mRecyclerView.setAdapter(mAdapter);
        }
        //loading progress controls
        mStageName = (TextView) findViewById(R.id.stage_name);
        mStageProgress = (ProgressBar) findViewById(R.id.stage_progress);
        mStageParent = findViewById(R.id.stage_layout);
        mAmount = (TextView) findViewById(R.id.amounts);

        FriendListsManager.getInstance(mViewProvider).updateList(FriendsData.LOADER_ID_COMMON_FRIENDS_LIST, mFullReload);
    }

    private void refreshUserPhoto(int loaderId, Bundle args, boolean reload) {
        final LoaderManager lm = getSupportLoaderManager();
        final Loader ldr = lm.getLoader(loaderId);

        //regardless whether we have the loader or not - reinitialize callbacks
        lm.initLoader(loaderId, args, mUserPhotoLoaderCallback);

        //but restart loading if only we already had it before (have NOT just run it at initialization)
        if (ldr != null){
            ((ImageLoader)ldr).setOnDownloadStartedListener(mDownloadStartedListener);
            if (reload) {
                ldr.onContentChanged();
            }
        }
    }

    /**
     * Changes User Name shared preference accordingly that will cause {@link #refreshNavigationView()} to be invoked
     */
    private void requestAccountDetails(){
        //Log.d(TAG, "Access Token update handler");
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final VKAccessToken token = VKAccessToken.tokenFromSharedPreferences(this, VKFApplication.PREF_KEY_ACCESS_TOKEN);
        if (token == null){
            final String name = sp.getString(VKFApplication.PREF_KEY_USERNAME, null);
            if (name != null){
                Log.i(TAG, "removing USERNAME preference");
                SharedPreferences.Editor editor = sp.edit();
                editor.remove(VKFApplication.PREF_KEY_USERNAME);
                editor.remove(VKFApplication.PREF_KEY_USERPHOTO);
                editor.apply();
            }
        } else {
            mWaiter.setVisibility(View.VISIBLE);

            VKRequest request = VKApi.users().get(FriendsData.PARAMS_USER_DETAILS);
            Log.i(TAG, "requesting USER data");
            request.executeWithListener(mVKCurrentUserRequestListener);
        }
    }

    private void onTokenUpdated(){
        requestAccountDetails();
    }

    /**
     * Invoked when change of access token requests update of the user name
     * or the activity wants to refresh the user name in its Navigation View
     */
    private void refreshNavigationView(){
        final MenuItem miLogin = mNavView.getMenu().findItem(R.id.action_login);
        final String name = PreferenceManager.getDefaultSharedPreferences(this).getString(VKFApplication.PREF_KEY_USERNAME, null);

        //Log.d(TAG, "Updating user name in Navigation View. Current name is " + String.valueOf(name));
        if (name == null) {
            miLogin.setTitle(R.string.action_login);
        } else {
            miLogin.setTitle(name);
        }

        refreshUserPhoto(FriendsData.LOADER_ID_USER_PHOTO, null, true);
    }

    private void updateUsers(@Side int side){
        if ((side & FLAG_LEFT) > 0){
            VKApiUser left = FriendsData.getLeftUser();
            if (left != null && left.id != 0) {
                mContactNameLeft.setText(left.first_name);
                refreshUserPhoto(FriendsData.LOADER_ID_LEFT_USER_PHOTO, null, true);
            }
        }
        if ((side & FLAG_RIGHT) > 0){
            VKApiUser right = FriendsData.getRightUser();
            if (right != null && right.id != 0) {
                mContactNameRight.setText(right.first_name);
                refreshUserPhoto(FriendsData.LOADER_ID_RIGHT_USER_PHOTO, null, true);
            }
        }
    }

    ////////////////////// interface implementations //////////////////////
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset/*negative*/) {
        final int maxScroll = appBarLayout.getTotalScrollRange();

        ViewGroup.LayoutParams lp = mFl.getLayoutParams();
        lp.height = maxScroll + mAppBarHeight + verticalOffset;
        mFl.setLayoutParams(lp);

        final float fraction = (float)verticalOffset / maxScroll; //negative
        final int halfWidth = appBarLayout.getMeasuredWidth() / 2;

        final int nameWidth = halfWidth +  (int)(mAppBarHeight * fraction);
        mContactNameLeft.setMaxWidth(nameWidth);
        mContactNameRight.setMaxWidth(nameWidth);

        final float textSize = fraction * (mTextSizeStart - mTextSizeEnd) + mTextSizeStart;
        mContactNameLeft.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mContactNameRight.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);


        final int size = (int)((2.5 + 1.5 * fraction) * mAppBarHeight);
        lp = mContactImageLeft.getLayoutParams();
        lp.height = lp.width = size;
        mContactImageLeft.setLayoutParams(lp);

        lp = mContactImageRight.getLayoutParams();
        lp.height = lp.width = size;
        mContactImageRight.setLayoutParams(lp);


        final int space = (int)(-fraction * (halfWidth - mAppBarHeight));
        lp = mSpace1.getLayoutParams();
        lp.width = space;
        mSpace1.setLayoutParams(lp);

        lp = mSpace2.getLayoutParams();
        lp.width = space;
        mSpace2.setLayoutParams(lp);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle nav bar item clicks here.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
/*
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

*/
            case R.id.action_login:
                VKSdk.login(this, VKScope.FRIENDS, VKScope.PHOTOS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key){
            case VKFApplication.PREF_KEY_ACCESS_TOKEN:
                onTokenUpdated();
                break;
            case VKFApplication.PREF_KEY_USERNAME:
                refreshNavigationView();
                break;
            case VKFApplication.PREF_KEY_USERPHOTO:
                refreshUserPhoto(FriendsData.LOADER_ID_USER_PHOTO, null, true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.avatar1:
            case R.id.name1:
            case R.id.avatar2:
            case R.id.name2:
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                final FriendListFragment flf = FriendListFragment.newInstance(FriendsData.getCurrentUser());
                //flf.setRetainInstance(true);
                ft.add(R.id.container, flf, (String)v.getTag())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null)
                        .commit();

                break;
            default:
                break;
        }
    }

    @Override
    public void onListFragmentInteraction(String fragmentTag, String itemId) {
        boolean changed;
        //change appropriate friend's photo and name
        VKApiUser user = FriendsData.getUser(itemId);
        if (fragmentTag.equals(getString(R.string.tag_user_left))){
            changed = FriendsData.getLeftUser().id != user.id;
            if (changed) {
                FriendsData.setLeftUser(user);
                updateUsers(FLAG_LEFT);
            }
        } else if (fragmentTag.equals(getString(R.string.tag_user_right))){
            changed = FriendsData.getRightUser().id != user.id;
            if (changed) {
                FriendsData.setRightUser(user);
                updateUsers(FLAG_RIGHT);
            }
        } else {
            //impossible case, but {@code changed} has to be initialized anyway
            changed = false;
        }

        if (changed) {
            mViewProvider.userId = itemId;
            FriendListsManager.getInstance(mViewProvider).updateList(FriendsData.LOADER_ID_COMMON_FRIENDS_LIST, true);
        }
    }
}
