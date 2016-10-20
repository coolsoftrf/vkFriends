package ru.coolsoft.vkfriends;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
//import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
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
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

import android.net.Uri;

import java.io.File;

import ru.coolsoft.vkfriends.dummy.DummyContent;
import ru.coolsoft.vkfriends.fragments.FriendListFragment;
import ru.coolsoft.vkfriends.loaders.ImageLoader;
import ru.coolsoft.vkfriends.loaders.sources.SharedPreferencesSource;

public class MainActivity extends AppCompatActivity
implements AppBarLayout.OnOffsetChangedListener
        , NavigationView.OnNavigationItemSelectedListener
        , SharedPreferences.OnSharedPreferenceChangeListener
        , View.OnClickListener
        , FriendListFragment.OnListFragmentInteractionListener
{
    private final static String TAG = MainActivity.class.getSimpleName();

    //main view controls
    private FrameLayout mFl;
    private TextView mContactNameLeft;
    private TextView mContactNameRight;
    private ImageView mContactImageLeft;
    private ImageView mContactImageRight;
    private Space mSpace1;
    private Space mSpace2;

    //measuring parameters
    private int mTextSizeStart;
    private int mTextSizeEnd;
    private int mAppBarHeight;

    //navigation controls
    private NavigationView mNavView;
    private ImageView mAvatar;
    private ProgressBar mWaiter;
    private ProgressBar mPhotoWaiter;

    //callback handlers
    //Handler mDelayHandler = new Handler();
    private VKCallback<VKAccessToken> mVKAuthCallback = new VKCallback<VKAccessToken>() {
        @Override
        public void onResult(VKAccessToken res) {
            // Пользователь успешно авторизовался
            res.saveTokenToSharedPreferences(MainActivity.this, VKFApplication.PREF_KEY_ACCESS_TOKEN);
            //reload avatar
            //update contacts in db
        }
        @Override
        public void onError(VKError error) {
            // Произошла ошибка авторизации (например, пользователь запретил авторизацию)
        }
    };
    private VKRequest.VKRequestListener mVKCurrentUserRequestListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {
            Log.d(TAG, "USER data retrieved");

            if (response.parsedModel instanceof VKList){
                Object o = ((VKList)response.parsedModel).get(0);
                if (o instanceof VKApiUser){
                    VKApiUser me = (VKApiUser) o;
                    FriendsData.setCurrentUser (me);
                    final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    final String name = sp.getString(VKFApplication.PREF_KEY_USERNAME, null);
                    final String newName = me.toString();

                    /*===DEBUG DELAY===*/
                    /**
                    final boolean posted = mDelayHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {/**/
                    Log.d(TAG, "Updating USERNAME preference");
                    SharedPreferences.Editor editor = null;
                    if (newName.equals(name)){
                        refreshNavigationView();
                    } else {
                        editor = sp.edit();
                        editor.putString(VKFApplication.PREF_KEY_USERNAME, newName);
                    }

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
            Log.d(TAG, "Error retrieving USER data: " + error.errorMessage);
            refreshNavigationView();
            mWaiter.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
            super.onError(error);
        }
    };

    LoaderManager.LoaderCallbacks<String> mUserPhotoLoaderCallback = new LoaderManager.LoaderCallbacks<String>() {
        @Override
        public Loader<String> onCreateLoader(int id, Bundle args) {
                ImageLoader il = new ImageLoader(MainActivity.this
                        , new SharedPreferencesSource(
                            PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                            , VKFApplication.PREF_KEY_USERPHOTO
                ));
                il.setOnDownloadStartedListener(new ImageLoader.OnDownloadStartedListener() {
                    @Override
                    public void onDownloadStarted() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPhotoWaiter.setVisibility(View.VISIBLE);
                            }
                        });
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
            Log.d(TAG, "image loader reset");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initParams();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        initViews();
    }

    private void initParams(){
        TypedArray attr = getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        mAppBarHeight = attr.getDimensionPixelSize(0, 0);
        attr.recycle();

        mTextSizeStart = getResources().getDimensionPixelSize(R.dimen.text_size);
        mTextSizeEnd = getResources().getDimensionPixelSize(R.dimen.text_size_small);
    }

    private void initViews() {
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

        mFl = (FrameLayout) findViewById(R.id.title);

        mContactNameLeft = (TextView) findViewById(R.id.name1);
        if (mContactNameLeft!= null) {
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
        mSpace1 = (Space) findViewById(R.id.spaceAvatar1);
        mSpace2 = (Space) findViewById(R.id.spaceAvatar2);
    }

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


        final int size = (int)((2 + fraction) * mAppBarHeight);
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
            case R.id.action_settings:

                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_login:
                VKSdk.login(this, VKScope.FRIENDS, VKScope.PHOTOS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, mVKAuthCallback)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
                refreshUserPhoto();
                break;
            default:
                break;
        }
    }

    private void refreshUserPhoto() {
        final LoaderManager lm = getSupportLoaderManager();
        final Loader ldr = lm.getLoader(FriendsData.LOADER_ID_USER_PHOTO);

        //regardless whether we have the loader or not - reinitialize callbacks
        lm.initLoader(FriendsData.LOADER_ID_USER_PHOTO, null, mUserPhotoLoaderCallback);

        //but restart loading if we did NOT have it before only
        if (ldr != null){
            ldr.onContentChanged();
        }
    }

    /**
     * Changes User Name shared preference accordingly that will cause {@link #refreshNavigationView()} to be invoked
     */
    private void requestAccountDetails(){
        Log.d(TAG, "Access Token update handler");
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final VKAccessToken token = VKAccessToken.tokenFromSharedPreferences(this, VKFApplication.PREF_KEY_ACCESS_TOKEN);
        if (token == null){
            final String name = sp.getString(VKFApplication.PREF_KEY_USERNAME, null);
            if (name != null){
                Log.d(TAG, "removing USERNAME preference");
                SharedPreferences.Editor editor = sp.edit();
                editor.remove(VKFApplication.PREF_KEY_USERNAME);
                editor.remove(VKFApplication.PREF_KEY_USERPHOTO);
                editor.apply();
            }
        } else {
            mWaiter.setVisibility(View.VISIBLE);

            VKRequest request = VKApi.users().get(VKParameters.from(VKApiConst.FIELDS
                    , TextUtils.join("," , new String[]{ FriendsData.FIELDS_NAME_GEN, FriendsData.FIELDS_PHOTO50, FriendsData.FIELDS_PHOTO200})
            ));
            Log.d(TAG, "requesting USER data");
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

        Log.d(TAG, "Updating user name in Navigation View. Current name is " + String.valueOf(name));
        if (name == null) {
            miLogin.setTitle(R.string.action_login);
        } else {
            miLogin.setTitle(name);
        }

       refreshUserPhoto();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.avatar1:
            case R.id.name1:
            case R.id.avatar2:
            case R.id.name2:
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();

                FriendListFragment flf = FriendListFragment.newInstance(FriendsData.getCurrentUser());
                ft.add(R.id.container, flf, (String)v.getTag());
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.addToBackStack(null);

                ft.commit();

                break;
            default:
                break;
        }
    }

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {

    }
}
