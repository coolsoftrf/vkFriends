package ru.coolsoft.vkfriends.loaders.sources;

import ru.coolsoft.vkfriends.FriendsData;

/**
 * Provides a way for Image Loader to get source URL from database
 */
public abstract class DatabaseUserSource implements ILoaderSource {
    protected abstract String getUserId();
    protected abstract String getDbFieldName();
    protected abstract String getObjFieldName();

    @Override
    public String value() {
        final String objFN = getObjFieldName();
        return FriendsData.getUser(getUserId()
                , new String[]{getDbFieldName()}, new String[]{objFN})

                .fields.optString(objFN, null);
    }
}
