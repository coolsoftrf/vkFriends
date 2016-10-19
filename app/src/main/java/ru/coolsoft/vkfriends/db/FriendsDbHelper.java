package ru.coolsoft.vkfriends.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by Bobby© on 12.04.2015.
 * Database helper class. Implements getters and setters for application data entities
 */
public class FriendsDbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "friends.db";

    private static int DB_VER = 1;

    public FriendsDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    private class DbFieldInfo{
        final boolean _primary;
        final String _type;
        final String _name;
        final boolean _notnull;
        final String _default;

        DbFieldInfo(boolean primary, String type, String name, boolean notNull, String defVal){
            _primary = primary;
            _type = type;
            _name = name;
            _notnull = notNull;
            _default = defVal;
        }
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        initStructure(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void initStructure(SQLiteDatabase db) {
        Class[] tables = FriendsContract.class.getDeclaredClasses();
        for (Class table : tables) {
            Field[] fields = table.getDeclaredFields();
            String tName = "";
            ArrayList<DbFieldInfo> dbFields = new ArrayList<>();
            StringBuilder key = new StringBuilder();

            for (Field field : fields) {
                Annotation ans[] = field.getDeclaredAnnotations();
                for (Annotation an : ans) {
                    if (an instanceof DbTable){
                        try {
                            tName = (String) field.get(null);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (an instanceof DbField){
                        try {
                            dbFields.add(new DbFieldInfo(
                                            ((DbField) an).primary()
                                            ,((DbField) an).type()
                                            , (String)field.get(null)
                                            ,((DbField) an).notnull()
                                            , ((DbField) an).default_value())
                            );
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (!tName.equals("") && dbFields.size() > 0){
                StringBuilder sql = new StringBuilder("CREATE TABLE ")
                        .append(tName)
                        .append(" (")
                        .append(BaseColumns._ID)
                        .append(" INTEGER");
                for (DbFieldInfo dbField : dbFields) {
                    sql.append(", ")
                            .append(dbField._name)
                            .append(" ")
                            .append(dbField._type);
                    if (dbField._primary){
                        key.append(", ").append(dbField._name);
                    }

                    if (!dbField._default.equals("")) {
                        sql.append(" DEFAULT ")
                                .append(dbField._default);
                    } else if (dbField._notnull){
                        sql.append(" NOT NULL");
                    }
                }
                sql.append(", PRIMARY KEY (")
                    .append(BaseColumns._ID)
                    .append(key)
                    .append("))");
                db.execSQL(sql.toString());
            }//else the table definition is incorrect
        }
    }
}
