package com.deepwits.Patron.DataBase;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.deepwits.Patron.DefaultConfig;

/**
 * Created by xp on 15-9-23.
 * 存储模块数据库连接
 */
public class DBOpenHelper extends SQLiteOpenHelper {

    /**
     * 构造SQL语句创建表
     */

    public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {  // 覆写onCreate方法，当数据库创建时就用SQL命令创建一个表

        /**
         *
         */
        db.execSQL("create table "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME +"(" +
                "id                 integer         primary key AUTOINCREMENT," +
                "filename           varchar(225)    not null," +
                "path               varchar(225)    not null," +
                "size               integer," +
                "width              integer," +
                "height             integer," +
                "date               integer," +
                "duration           integer," +
                "thumb_path         varchar(225)," +
                "latitude           float           default 666," +
                "longitude          float           default 666," +
                "event_type         integer," +
                "media_type         integer," +
                "command_origin     integer)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
