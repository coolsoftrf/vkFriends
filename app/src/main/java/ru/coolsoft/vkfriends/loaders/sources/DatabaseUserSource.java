package ru.coolsoft.vkfriends.loaders.sources;

import ru.coolsoft.vkfriends.FriendsData;

/**
 * Provides a way for Image Loader to get source URL from database
 */
public abstract class DatabaseUserSource implements ILoaderSource {
    protected abstract int getUserId();
    protected abstract String getFieldName();

    @Override
    public String value() {
        return FriendsData.getUser(getUserId(), getFieldName()).fields.optString(getFieldName(), null);
    }
}
