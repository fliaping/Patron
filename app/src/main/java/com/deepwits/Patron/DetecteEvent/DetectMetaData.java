package com.deepwits.Patron.DetecteEvent;

import android.net.Uri;
import android.provider.BaseColumns;

public interface DetectMetaData {
	 // 定义外部访问的Authority
    public static final String DETECT_AUTHORITY = "com.deepwits.patron.detect";
    public static final String SECURITY_AUTHORITY = "com.deepwits.patron.security";
    //单个数据操作标志
    public static final String ITEM_FLAG = "ITEM";
    // 数据库版本
    public static final int VERSION = 1;

    public interface VideoTableMetaData extends BaseColumns{
        // 表名
        public static final String TABLE_NAME = "VideoList";
        // 外部程序访问本表的uri地址
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + DETECT_AUTHORITY + "/" + TABLE_NAME);

        // driving_event表列名
        public static final String FILENAME = "filename";
        public static final String PATH = "path";
        public static final String SIZE = "size";
        public static final String WIDTH = "width";
        public static final String HEIGHT = "height";
        public static final String DATE = "date";
        public static final String DURATION = "duration";
        public static final String TYPE = "type";

        //默认排序
        public static final String SORT_ORDER="date desc";
        //得到event表中的所有记录
        public static final String CONTENT_LIST="vnd.android.cursor.dir/vnd.recorderprovider.videolist";
        //得到一条记录
        public static final String CONTENT_ITEM="vnd.android.cursor.item/vnd.drivingprovider.videolist";
    }

    public interface PicTableMetaData extends BaseColumns{

        // 表名
        public static final String TABLE_NAME = "PicList";
        // 外部程序访问本表的uri地址
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + DETECT_AUTHORITY + "/" + TABLE_NAME);

        // driving_event表列名
        public static final String FILENAME = "filename";
        public static final String PATH = "path";
        public static final String SIZE = "size";
        public static final String WIDTH = "width";
        public static final String HEIGHT = "height";
        public static final String DATE = "date";
        public static final String TYPE = "type";

        //默认排序
        public static final String SORT_ORDER="date desc";
        //得到event表中的所有记录
        public static final String CONTENT_LIST="vnd.android.cursor.dir/vnd.recorderprovider.piclist";
        //得到一条记录
        public static final String CONTENT_ITEM="vnd.android.cursor.item/vnd.drivingprovider.piclist";

    }
}
