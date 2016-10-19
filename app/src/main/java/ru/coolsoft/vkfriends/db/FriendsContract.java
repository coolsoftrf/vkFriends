package ru.coolsoft.vkfriends.db;

import android.provider.BaseColumns;

import ru.coolsoft.vkfriends.FriendsData;

/**
 * Created by Bobby© on 12.04.2015.
 * Contract class that defines application database structure
 */
public final class FriendsContract {
    public class Users implements BaseColumns {
        @DbTable
        public static final String TABLE_NAME = "Users";
/*
        @DbField(primary = true)
        public static final String COLUMN_USER_ID = "UserId";
*/
        @DbField(type = "TEXT")
        public static final String COLUMN_USER_NAME = "Name";
        @DbField(type = "TEXT")
        public static final String COLUMN_USER_NAME_GEN = FriendsData.FIELDS_NAME_GEN;
        @DbField(type = "TEXT")
        public static final String COLUMN_USER_PHOTO50 = FriendsData.FIELDS_PHOTO50;
        @DbField(type = "TEXT")
        public static final String COLUMN_USER_PHOTO200 = FriendsData.FIELDS_PHOTO200;

    }

    public class Friends implements BaseColumns {
        @DbTable
        public static final String TABLE_NAME = "Friends";
/*
        @DbField (primary = true)
        public static final String COLUMN_USER_ID = "UserId";
*/
        @DbField (primary = true)
        public static final String COLUMN_FRIEND_ID = "FriendId";
        @DbField(default_value = "0")
        public static final String COLUMN_HIDDEN = "DiscoveredHidden";
    }
}
