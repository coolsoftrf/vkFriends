package ru.coolsoft.vkfriends.loaders;

import android.content.SharedPreferences;

public class SharedPreferencesSource implements ILoaderSource{
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
