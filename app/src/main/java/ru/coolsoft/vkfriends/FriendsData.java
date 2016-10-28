package ru.coolsoft.vkfriends;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.model.VKApiUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ru.coolsoft.vkfriends.db.FriendsContract;
import ru.coolsoft.vkfriends.db.FriendsDbHelper;

/**
 * Data manager for VK users
 */
public class FriendsData {
    public final static String FIELDS_PHOTO200 = VKApiUser.FIELD_PHOTO_200;
    public final static String FIELDS_NAME_GEN = "first_name_gen";

    public final static VKParameters PARAMS_USER_DETAILS = VKParameters.from(VKApiConst.FIELDS
            , TextUtils.join("," , new String[]{ FriendsData.FIELDS_NAME_GEN, FriendsData.FIELDS_PHOTO200})
    );

    public final static int LOADER_ID_USER_PHOTO = 1;
    public final static int LOADER_ID_WHOSE_PHOTO = 2;
    public final static int LOADER_ID_FRIEND_LIST = 11;

    private static SQLiteDatabase mDb;
    private static SQLiteDatabase db(){
        if (mDb == null){
            mDb = new FriendsDbHelper(VKFApplication.app()).getWritableDatabase();
        }

        return mDb;
    }

    public static void updateUserData(VKApiUser user){
        ContentValues values = new ContentValues();
        values.put(FriendsContract.Users._ID, user.id);
        values.put(FriendsContract.Users.COLUMN_USER_NAME, user.toString());
        values.put(FriendsContract.Users.COLUMN_USER_NAME_GEN, user.fields.optString(FIELDS_NAME_GEN, user.first_name));
        values.put(FriendsContract.Users.COLUMN_USER_PHOTO200, user.photo_200);

        db().replace(FriendsContract.Users.TABLE_NAME, null, values);
    }

    public static void updateFriendData(String whoseId, String whoId){
        ContentValues values = new ContentValues();
        values.put(FriendsContract.Friends._ID, whoseId);
        values.put(FriendsContract.Friends.COLUMN_FRIEND_ID, whoId);

        db().replace(FriendsContract.Friends.TABLE_NAME, null, values);
    }

    public static VKApiUser getUser(int id, String ... fieldNames) {
        Cursor c = db().query(FriendsContract.Users.TABLE_NAME, fieldNames
                , FriendsContract.Users._ID + " = ?", new String[]{String.valueOf(id)}
                , null, null, null);

        VKApiUser user = null;
        try {
            if (c.getCount() > 0) {
                c.moveToFirst();

                Map<String, String> mapFields = new HashMap<>();
                for (int i = 0; i < fieldNames.length; i++) {
                    mapFields.put(fieldNames[i], c.getString(i));
                }

                JSONObject fields = new JSONObject(mapFields);
                user = new VKApiUser(fields);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }

        return user;
    }

    public static void setCurrentUser(VKApiUser me){
        VKFApplication.app().setMe(me);
    }

    public static VKApiUser getCurrentUser(){
        return VKFApplication.app().getMe();
    }

    public static Cursor getFriendsOf(String userId, String usersTableAlias, String... columns){
        return db().rawQuery("SELECT " + TextUtils.join(", ", columns) + " FROM "
                + FriendsContract.Users.TABLE_NAME + " " + usersTableAlias + " INNER JOIN "
                + FriendsContract.Friends.TABLE_NAME + " f ON f."
                + FriendsContract.Friends.COLUMN_FRIEND_ID + "="
                + usersTableAlias + "." + FriendsContract.Users._ID + " WHERE f."
                + FriendsContract.Friends._ID + "=?"

                , new String[]{userId}
        );
    }
}
