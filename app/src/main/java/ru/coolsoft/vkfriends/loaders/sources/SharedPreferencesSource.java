package ru.coolsoft.vkfriends.loaders.sources;

import android.content.SharedPreferences;

/**
 * Provides a way for Image Loader to get source URL from Shared Preferences
 */
public class SharedPreferencesSource implements ILoaderSource {
    private String mKey;
    private SharedPreferences mSP;

    public SharedPreferencesSource(SharedPreferences sharedPreferences, String preferenceKey){
        mSP = sharedPreferences;
        mKey = preferenceKey;
    }

    @Override
    public String value() {
        return mSP.getString(mKey, null);
    }
}
