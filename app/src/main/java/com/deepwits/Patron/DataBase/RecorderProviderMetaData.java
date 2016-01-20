package com.deepwits.Patron.DataBase;

import android.net.Uri;
import android.provider.BaseColumns;

import com.deepwits.Patron.DefaultConfig;

/**
 * Created by Payne on 1/19/16.
 * Recorder Provider 的Meta信息
 */
public class RecorderProviderMetaData {
    // 定义外部访问的Authority
    public static final String AUTHORITY = "com.deepwits.Patron.StorageManager";

    // 数据库版本
    public static final int VERSION = 1;
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.paynexu.media";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.paynexu.media";
    public static final String DB_NAME = DefaultConfig.DB_NAME;

    public interface MediaFileMetaData extends BaseColumns {
        // 表名
        public static final String TABLE_NAME = DefaultConfig.DB_MEDIAFILE_TABLE_NAME;
        // 外部程序访问本表的uri地址
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + AUTHORITY + "/" + TABLE_NAME);



        // driving_event表列名
        public static final String ID = "id";
        public static final String FILENAME = "filename";
        public static final String PATH = "path";
        public static final String SIZE = "size";
        public static final String WIDTH = "width";
        public static final String HEIGHT = "height";
        public static final String DATE = "date";
        public static final String DURATION = "duration";
        public static final String THUMB_PATH = "thumb_path";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String EVENT_TYPE = "event_type";
        public static final String MEDIA_TYPE = "media_type";
        public static final String COMMAND_ORIGIN = "command_origin";


        //默认排序
        public static final String SORT_ORDER="date desc";
    }
}
