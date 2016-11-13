package ru.coolsoft.vkfriends;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.model.VKApiUser;

import ru.coolsoft.vkfriends.common.FriendsData;

/**
 * Created by Bobby© on 05.10.2016.
 * The application class that stores global data and contains general watchdogs
 */
public class VKFApplication extends Application {
    private static final String TAG = VKFApplication.class.getSimpleName();

    public static final String PREF_KEY_ACCESS_TOKEN = "access_token";
    public static final String PREF_KEY_USERNAME = "user_name";
    public static final String PREF_KEY_USERPHOTO = "user_photo_url";

    private static VKFApplication mApp;

    private VKApiUser mMe;
    private final VKApiUser mUserLeft = new VKApiUser();
    private final VKApiUser mUserRight = new VKApiUser();
    private boolean mInitialized;
    private VKAccessTokenTracker mVKAccessTokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(VKAccessToken oldToken, VKAccessToken newToken) {
            Log.i(TAG, "Auth Token has just changed from " + String.valueOf(oldToken) + " to " + String.valueOf(newToken));
            if (newToken == null) {
                // VKAccessToken is invalid
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(VKFApplication.this).edit();
                editor.remove(PREF_KEY_ACCESS_TOKEN);
                editor.apply();
            } /*else {
                newToken.saveTokenToSharedPreferences(VKFApplication.this, PREF_KEY_ACCESS_TOKEN);
            }*/
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
        FriendsData.cleanFilesDir();

        mVKAccessTokenTracker.startTracking();
        VKSdk.initialize(this);
    }

    public static VKFApplication app(){
        return mApp;
    }

    public boolean isInitialized(){
        return mInitialized;
    }

    public void setInitialized(){
        mInitialized = true;
    }

    public VKApiUser getMe() {
        return mMe;
    }
    public void setMe(VKApiUser me) {
        mMe = me;
    }

    public VKApiUser getLeft() {
        return mUserLeft;
    }
    public void setLeft(VKApiUser left) {
        mUserLeft.parse(left.fields);
    }

    public VKApiUser getRight() {
        return mUserRight;
    }
    public void setRight(VKApiUser right) {
        mUserRight.parse(right.fields);
    }
}
