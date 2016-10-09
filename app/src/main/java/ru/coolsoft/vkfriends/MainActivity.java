package ru.coolsoft.vkfriends;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
//import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
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
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

public class MainActivity extends AppCompatActivity
implements AppBarLayout.OnOffsetChangedListener
, NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    //main view controls
    private FrameLayout mFl;
    private TextView mContactNameLeft;
    private TextView mContactNameRight;
    private ImageView mContactImageLeft;
    private ImageView mContactImageRight;
    private Space mSpace1;
    private Space mSpace2;

    //measuring paramenters
    private int mTextSizeStart;
    private int mTextSizeEnd;
    private int mAppBarHeight;

    //navigation controls
    private NavigationView mNavView;
    private ProgressBar mWaiter;

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
                                if (newName.equals(name)){
                                    refreshNavigationView();
                                } else {
                                    SharedPreferences.Editor editor = sp.edit();
                                    editor.putString(VKFApplication.PREF_KEY_USERNAME, newName);
                                    editor.apply();
                                }
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
                requestAccountDetails();
                VKFApplication.app().setInitialized();
            }
        }

        mFl = (FrameLayout) findViewById(R.id.title);

        mContactNameLeft = (TextView) findViewById(R.id.name1);
        mContactNameRight = (TextView) findViewById(R.id.name2);
        mContactImageLeft = (ImageView) findViewById(R.id.avatar1);
        mContactImageRight = (ImageView) findViewById(R.id.avatar2);
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
                VKSdk.login(this, VKScope.FRIENDS);
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
            default:
                break;
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
                editor.apply();
            }
        } else {
            mWaiter.setVisibility(View.VISIBLE);

            VKRequest request = VKApi.users().get();
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

        //ToDo: update user image as well
        Log.d(TAG, "Updating user name in Navigation View. Current name is " + String.valueOf(name));
        if (name == null) {
            miLogin.setTitle(R.string.action_login);
        } else {
            miLogin.setTitle(name);
        }
    }
}
