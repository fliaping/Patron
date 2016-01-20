package com.deepwits.Patron;

import com.deepwits.Patron.StorageManager.StorageUtil;

import java.io.File;
import java.io.IOException;

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
    }
    public static boolean ok() throws IOException{
        String ROOT = StorageUtil.getStorageDir();
        if(ROOT == null){
            throw new IOException("SD卡未准备好");
        }else{
            APP_PATH = ROOT + S + APP_DIR + S;
            VIDEOS_PATH = APP_PATH + VIDEOS_DIR + S;
            PICTURES_PATH = APP_PATH + PICTURES_DIR + S;
            NORMAL_VIDEO_PATH = VIDEOS_PATH + NORMAL_VIDEO_DIR + S;
            LOCK_VIDEO_PATH = VIDEOS_PATH + LOCK_VIDEO_DIR + S;
            UPLOAD_VIDEO_PATH = VIDEOS_PATH + UPLOAD_VIDEO_DIR + S;
            TAKE_PICTURE_PATH = PICTURES_PATH + TAKE_PICTURE_DIR + S;
            UPLOAD_PICTURE_PATH = PICTURES_PATH + UPLOAD_PICTURE_DIR + S;
            return true;
        }
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


    public static final String DB_MEDIAFILE_TABLE_NAME = "MediaFile";
    public static final String DB_SETTING_TABLE_NAME = "Setting";
    public static final String DB_NAME = "Recorder.db";

    public static String S = File.separator;
    public static String ROOT = null;
    public static String APP_PATH = null;
    public static String VIDEOS_PATH = null;
    public static String PICTURES_PATH = null;
    public static String NORMAL_VIDEO_PATH = null;
    public static String LOCK_VIDEO_PATH = null;
    public static String UPLOAD_VIDEO_PATH = null;
    public static String TAKE_PICTURE_PATH = null;
    public static String UPLOAD_PICTURE_PATH = null;


    public static long SOFT_LIMIT = 200 * 1024 * 1024;  //软限制200M
    public static long HARD_LIMIT = 50 * 1024 * 1024;   //硬限制50M

    //设置广播,广播名：com.deepwits.Patron.setting   收到广播设置相关参数,下次生效
    public static final String SETTING_BROADCAST = "com.deepwits.Patron.setting";

    //广播参数字段key
    public static final String VIDEO_WIDTH = "video_width";  //视频宽度
    public static final String VIDEO_HEIGHT = "video_height";  //视频高度
    public static final String VIDEO_FRAMERATE= "video_framerate";  //视频帧率
    public static final String VIDEO_BITRATE = "video_bitrate";   //视频码率
    public static final String VIDEO_QUALITY = "video_quality";   //视频质量
    public static final String PICTURE_WIDTH = "picture_width";   //图片宽度
    public static final String PICTURE_HEIGHT = "picture_height";  //图片高度
    public static final String PICTURE_QUALITY = "picture_quality";  //图片质量
    public static final String RTSP_PORT = "rtsp_port";   //rtsp端口

    //action广播, 收到广播立即执行相关动作
    public static final String ACTION_BROADCAST = "com.deepwits.Patron.action";
    /**
     * 参数:
     * start_record : 启动录像
     * stop_record :  停止录像
     * capture_picture : 拍照
     * start_voice : 启动声音录制
     * stop_voice : 关闭声音录制
     * loopRecord : 循环录制
     */
    public static final String ACTION_NAME = "action_name";   //执行动作名字
    //可携带设置参数,若有设置参数则以该参数执行动作,负责从设置中取值执行


}
