package ru.coolsoft.vkfriends.common;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.model.VKApiUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import ru.coolsoft.vkfriends.VKFApplication;
import ru.coolsoft.vkfriends.db.FriendsContract;
import ru.coolsoft.vkfriends.db.FriendsDbHelper;

/**
 * Data manager for VK users
 */
public class FriendsData {
    public final static String FIELDS_ID = "id";
    public final static String FIELDS_NAME = "first_name";
    public final static String FIELDS_NAME_GEN = "first_name_gen";
    public final static String FIELDS_PHOTO200 = VKApiUser.FIELD_PHOTO_200;

    public final static VKParameters PARAMS_USER_DETAILS = VKParameters.from(VKApiConst.FIELDS
            , TextUtils.join("," , new String[]{ FriendsData.FIELDS_NAME_GEN, FriendsData.FIELDS_PHOTO200})
    );

    public final static int LOADER_ID_USER_PHOTO = 1;
    public final static int LOADER_ID_WHOSE_PHOTO = 2;
    public final static int LOADER_ID_COMMON_FRIENDS_LIST = 3;

    public final static int LOADER_ID_FRIEND_LIST = 10;
    public final static int LOADER_ID_LEFT_USER_PHOTO = 11;
    public final static int LOADER_ID_RIGHT_USER_PHOTO = 12;


    public final static int LOADER_ID_FRIENDLIST_PHOTO_START = 10000;

    private static SQLiteDatabase mDb;
    private static SQLiteDatabase db(){
        if (mDb == null){
            mDb = new FriendsDbHelper(VKFApplication.app()).getWritableDatabase();
        }

        return mDb;
    }

    public static void beginTran(){
        db().beginTransaction();
    }

    public static void setTranSuccessful(){
        db().setTransactionSuccessful();
    }
    public static void endTran(){
        db().endTransaction();
    }

    public static void cleanFilesDir(){
        File dir = VKFApplication.app().getFilesDir();
        if (dir != null) {
            for (File f : dir.listFiles()) {
                if(!f.delete()){
                    //do nothing with the files we can't handle
                    Log.v("cleanup", "can't remove " + f.getName());
                }
            }
        }
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

    public static VKApiUser getUser(String itemId) {
        return getUser(itemId
                , new String[]{
                        FriendsContract.Users._ID
                        , FriendsContract.Users.COLUMN_USER_NAME
                        , FriendsContract.Users.COLUMN_USER_NAME_GEN
                        , FriendsContract.Users.COLUMN_USER_PHOTO200
                }
                , new String[]{
                        FriendsData.FIELDS_ID
                        , FriendsData.FIELDS_NAME
                        , FriendsData.FIELDS_NAME_GEN
                        , FriendsData.FIELDS_PHOTO200
                });
    }

    public static VKApiUser getUser(String id, String[] dbFieldNames, String[] objFieldNames) {
        Cursor c = db().query(FriendsContract.Users.TABLE_NAME, dbFieldNames
                , FriendsContract.Users._ID + " = ?", new String[]{String.valueOf(id)}
                , null, null, null);

        VKApiUser user = null;
        try {
            if (c.getCount() > 0) {
                c.moveToFirst();

                Map<String, String> mapFields = new HashMap<>();
                for (int i = 0; i < dbFieldNames.length && i < objFieldNames.length; i++) {
                    mapFields.put(objFieldNames[i], c.getString(i));
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
    public static Cursor getCommonFriendsOf(String user1Id, String user2id, String usersTableAlias, String... columns){
        return db().rawQuery("SELECT " + TextUtils.join(", ", columns) + " FROM "
                + FriendsContract.Users.TABLE_NAME + " " + usersTableAlias + " INNER JOIN "
                + FriendsContract.Friends.TABLE_NAME + " f1 ON f1."
                + FriendsContract.Friends.COLUMN_FRIEND_ID + "="
                + usersTableAlias + "." + FriendsContract.Users._ID + " INNER JOIN "
                + FriendsContract.Friends.TABLE_NAME + " f2 ON f2."
                + FriendsContract.Friends.COLUMN_FRIEND_ID + "="
                + usersTableAlias + "." + FriendsContract.Users._ID + " WHERE f1."
                + FriendsContract.Friends._ID + " = ? AND f2."
                + FriendsContract.Friends._ID + " = ?"

                , new String[]{user1Id, user2id}
        );
    }

    public static void setCurrentUser(VKApiUser me){
        VKFApplication.app().setMe(me);
    }
    public static VKApiUser getCurrentUser(){
        return VKFApplication.app().getMe();
    }

    public static void setLeftUser(VKApiUser left){
        VKFApplication.app().setLeft(left);
    }
    public static VKApiUser getLeftUser(){
        return VKFApplication.app().getLeft();
    }

    public static void setRightUser(VKApiUser right){
        VKFApplication.app().setRight(right);
    }
    public static VKApiUser getRightUser(){
        return VKFApplication.app().getRight();
    }

    public static class Invalid {
        public static final int INDEX = -1;
    }
}
