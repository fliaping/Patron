package com.deepwits.Patron;

import com.deepwits.Patron.StorageManager.StorageUtil;

import java.io.File;

/**
 * Created by Payne on 1/13/16.
 */
public class DefaultConfig {
    /**
     * 应用目录结构：
     * --ROOT
     * |--APP_DIR
     * |--VIDEOS_DIR
     * |  |--NORMAL_VIDEO_DIR
     * |  |  |-- yyyy-mm-dd
     * |  |     |-- 20150924000000.mp4
     * |  |--LOCK_VIDEO_DIR
     * |  |  |-- yyyy-mm-dd
     * |  |     |-- SOS_20150924000000.mp4
     * |  |--UPLOAD_VIDEO_DIR
     * |     |-- UPLOAD_20150924000000.mp4
     * |
     * |--PICTURES_DIR
     * |--TAKE_PICTURE_DIR
     * |  |-- PIC_20150924000000.jpg
     * |--UPLOAD_PICTURE_DIR
     * |-- UPLOAD_20150924000000.jpg
     */

    public DefaultConfig(){
        ROOT = StorageUtil.getStorageDir();
        if(ROOT == null){
            return;
        }
        APP_PATH = ROOT + S + APP_DIR + S;
        VIDEOS_PATH = APP_PATH + VIDEOS_DIR + S;
        PICTURES_PATH = APP_PATH + PICTURES_DIR + S;
        NORMAL_VIDEO_PATH = VIDEOS_PATH + NORMAL_VIDEO_DIR + S;
        LOCK_VIDEO_PATH = VIDEOS_PATH + LOCK_VIDEO_DIR + S;
        UPLOAD_VIDEO_PATH = VIDEOS_PATH + UPLOAD_VIDEO_DIR + S;
        TAKE_PICTURE_PATH = PICTURES_PATH + TAKE_PICTURE_DIR + S;
        UPLOAD_PICTURE_PATH = PICTURES_PATH + UPLOAD_PICTURE_DIR + S;
    }
    private static String TAG = "Config";

    public static final String APP_DIR = "MyRecorder";
    public static final String VIDEOS_DIR = "Videos";
    public static final String PICTURES_DIR = "Pictures";
    public static final String NORMAL_VIDEO_DIR = "normal";
    public static final String LOCK_VIDEO_DIR = "event";
    public static final String UPLOAD_VIDEO_DIR = "ShortVideo";
    public static final String TAKE_PICTURE_DIR = "takeWhileRecord";
    public static final String UPLOAD_PICTURE_DIR = "OrderPhoto";

    public String S = File.separator;
    public String ROOT = null;
    public String APP_PATH = null;
    public String VIDEOS_PATH = null;
    public String PICTURES_PATH = null;
    public String NORMAL_VIDEO_PATH = null;
    public String LOCK_VIDEO_PATH = null;
    public String UPLOAD_VIDEO_PATH = null;
    public String TAKE_PICTURE_PATH = null;
    public String UPLOAD_PICTURE_PATH = null;
}
