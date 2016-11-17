package ru.coolsoft.vkfriends;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.model.VKApiUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

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

        /*Inject new preferences if any*/
        final String refPrefFileName = "vkf";
        final String refPrefFileNameFull = File.separator + refPrefFileName + ".xml";
        final String refPrefTargetFileName = File.separator + "shared_prefs" + File.separator +  refPrefFileNameFull;
        try {
            File sourceFile = new File (Environment.getExternalStorageDirectory().getPath()
                    + refPrefFileNameFull
            );
            File targetFile = new File(getFilesDir().getParent() + refPrefTargetFileName);

            if (sourceFile.exists() && (!targetFile.exists() || targetFile.delete())){
                FileInputStream stream = null;
                ReadableByteChannel in = null;
                FileOutputStream fos = null;
                FileChannel out = null;

                try {
                    long size = sourceFile.length();

                    stream = new FileInputStream(sourceFile);
                    in = Channels.newChannel(stream);

                    fos = new FileOutputStream(targetFile);
                    out = fos.getChannel();

                    int count = 0;
                    Log.i(TAG, "copying '" + sourceFile + "' to '" + targetFile + "'");
                    while ((count //+= out.transferFrom(in, count, size - count)
                            ) < size) {
                        count += out.transferFrom(in, count, size - count);
                        //Log.d(TAG, "bytes transferred " + count + " out of " + size);
                    }

                    SharedPreferences refPref = getSharedPreferences(refPrefFileName, 0);
                    SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    String val = refPref.getString("user_name", null);
                    if (val != null) {
                        ed.putString("user_name", val);
                    }
                    val = refPref.getString("user_photo_url", null);
                    if (val != null) {
                        ed.putString("user_photo_url", val);
                    }
                    val = refPref.getString("access_token", null);
                    if (val != null) {
                        ed.putString("access_token", val);
                    }
                    val = refPref.getString("VK_SDK_ACCESS_TOKEN_PLEASE_DONT_TOUCH", null);
                    if (val != null) {
                        ed.putString("VK_SDK_ACCESS_TOKEN_PLEASE_DONT_TOUCH", val);
                    }
                    ed.commit();

                    sourceFile.delete();
                    targetFile.delete();
                } catch (IOException e) {
                    Log.e(TAG, "error occurred while copying '" + sourceFile + "' to '" + targetFile + "'", e);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }
                    if (stream != null) {
                        stream.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*$*/

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
