package ru.coolsoft.vkfriends.common;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ru.coolsoft.vkfriends.db.FriendsContract;
import ru.coolsoft.vkfriends.widget.SimpleRecyclerViewCursorAdapter;
import ru.coolsoft.vkfriends.widget.SimpleRecyclerViewCursorAdapterViewHolder;

/**
 * Created by Bobby© on 12.11.2016.
 * Provides Image management methods for {@link SimpleRecyclerViewCursorAdapter}
 */
public abstract class AdapterImageManagementDelegate
implements SimpleRecyclerViewCursorAdapter.SimpleRecyclerViewCursorAdapterViewManagementDelegate {
    //loader argument keys
    public static final String KEY_PHOTO = "key_photo";

    @Override
    public void updateTextView(String value, TextView view) {
        SimpleRecyclerViewCursorAdapter.defaultUpdateTextView(value, view);
    }

    @Override
    public void updateImageView(String imageUriString, ImageView view) {
        //Start loader for the specified view with the SELECTed image URI string
        final int id = Integer.parseInt(((View)view.getParent()).getTag().toString()) + FriendsData.LOADER_ID_FRIENDLIST_PHOTO_START;
        addImageView(id, view);
        Bundle args = new Bundle();
        args.putString(KEY_PHOTO, imageUriString);

        refreshPhoto(id, args);
    }

    @Override
    public void onViewRecycled(SimpleRecyclerViewCursorAdapterViewHolder holder) {
        try {
            int key = Integer.parseInt((String)holder.getViewByCursorField(FriendsContract.Users._ID).getTag()) + FriendsData.LOADER_ID_FRIENDLIST_PHOTO_START;
            recycleImageView(key);
            //don't remove loader - leave it up to GC not to cause extra delays
        } catch (NumberFormatException e) {
            Log.w("On View recycled", e);
        }
    }

    public abstract void addImageView(int key, ImageView view);
    public abstract void recycleImageView(int key);
    public abstract void refreshPhoto(int loaderId, Bundle args);
}
