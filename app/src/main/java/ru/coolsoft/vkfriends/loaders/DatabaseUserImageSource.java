package ru.coolsoft.vkfriends.loaders;

import ru.coolsoft.vkfriends.FriendsData;

/**
 * Provides a way for Image Loader to get source URL from database
 */
public abstract class DatabaseUserImageSource implements ILoaderSource {
    protected abstract int getUserId();
    protected abstract String getPhotoFieldName();

    @Override
    public String value() {
        return FriendsData.getUser(getUserId(), getPhotoFieldName()).fields.optString(getPhotoFieldName(), null);
    }
}
