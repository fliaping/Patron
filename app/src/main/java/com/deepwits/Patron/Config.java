package com.deepwits.Patron;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.deepwits.Patron.StorageManager.StorageUtil;

import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Payne on 1/13/16.
 */
public class Config {
    private static Context context;
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
    /**
     * 使用Config类前要先调用该函数
     * @return 存储是否准备好
     * @throws IOException  存储未准备好
     */
    private Config(){

    }
    public static boolean ok(Context context) throws Exception {
        Config.context = context;
        if(Config.context == null){
            throw new Exception("Config 未传入Context");
        }
        String ROOT = StorageUtil.getStorageDir();
        ROOT = "/storage/sdcard0";
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
            THUMBNAIL_PATH = APP_PATH+THUMBNAIL_DIR+S;

            Config config = new Config();      //初始化默认配置
            config.initSetMap();config.initDefaultSp();
            Log.e("StorageManager","APP path:"+APP_PATH);
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
    public static final String THUMBNAIL_DIR = ".thumbnail";


    public static final String DB_MEDIAFILE_TABLE_NAME = "MediaFile";
    public static final String SETTING_SP_NAME = "Setting";
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
    public static String THUMBNAIL_PATH = null;


    public static long SOFT_LIMIT = 200 * 1024 * 1024;  //软限制200M
    public static long HARD_LIMIT = 50 * 1024 * 1024;   //硬限制50M

    //设置广播,广播名：com.deepwits.Patron.setting   收到广播设置相关参数,下次生效
    public static final String SETTING_BROADCAST = "com.deepwits.Patron.setting";

    //广播参数字段key
    //本地视频配置
    public static final String VIDEO_WIDTH = "video_width";  //视频宽度
    public static final String VIDEO_HEIGHT = "video_height";  //视频高度
    public static final String VIDEO_FRAMERATE= "video_framerate";  //视频帧率
    public static final String VIDEO_BITRATE = "video_bitrate";   //视频码率
    public static final String VIDEO_QUALITY = "video_quality";   //视频质量
    public static final String VIDEO_DUR = "video_duration";  //本地视频片段长度
    public static final String VIDEO_LOOP = "loop_record";  //是否循环录制本地视频



    //上传到服务器的视频
    public static final String RMT_VID_WIDTH = "RMT_video_width";   //远程视频质量
    public static final String RMT_VID_HEIGHT = "RMT_video_height";  //远程视频高度
    public static final String RMT_VID_FRAMERATE= "RMT_video_framerate";  //远程视频帧率
    public static final String RMT_VID_BITRATE = "RMT_video_bitrate";   //远程视频码率
    public static final String RMT_VID_QUALITY = "RMT_video_quality";   //远程视频质量


    //本地图片配置
    public static final String PICTURE_WIDTH = "picture_width";   //图片宽度key
    public static final String PICTURE_HEIGHT = "picture_height";  //图片高度key
    public static final String PICTURE_QUALITY = "picture_quality";  //图片质量key


    //远程图片配置
    public static final String RMT_PIC_WIDTH = "RMT_picture_width";   //图片宽度key
    public static final String RMT_PIC_HEIGHT = "RMT_picture_height";  //图片高度key
    public static final String RMT_PIC_QUALITY = "RMT_picture_quality";  //图片质量key


    //RTSP默认配置
    public static final String RTSP_PORT = "rtsp_port";   //rtsp端口
    public static final String RTSP_VID_WIDTH = "rtsp_vid_width";   //rtsp 视频宽度
    public static final String RTSP_VID_HEIGHT = "rtsp_vid_height";  //rtsp 视频高度
    public static final String RTSP_VID_FRAMERATE = "rtsp_vid_framerate";

    //状态信息
    public static final String IS_VID_REC = "is_vid_rec";
    public static final String IS_RMT_V_REC = "is_rmt_v_rec";
    public static final String IS_RTSP_RUN = "is_rtsp_run";
    public static boolean IS_VID_REC_DE = false; //本地录像状态
    public static boolean IS_RMT_V_REC_DE = false;  //远程录像状态
    public static boolean IS_RTSP_RUN_DE = false;  //RTSP 是否在streaming

    //默认配置表
    public static Map setMap = new HashMap();
    private void initSetMap(){
        //本地视频
        setMap.put(VIDEO_WIDTH,320);
        setMap.put(VIDEO_HEIGHT,240);
        setMap.put(VIDEO_FRAMERATE,20);
        setMap.put(VIDEO_BITRATE,500000);
        setMap.put(VIDEO_QUALITY,80);
        setMap.put(VIDEO_DUR,3*60*1000);
        setMap.put(VIDEO_LOOP,1);
        //远程视频
        setMap.put(RMT_VID_WIDTH,320);
        setMap.put(RMT_VID_HEIGHT,240);
        setMap.put(RMT_VID_FRAMERATE,20);
        setMap.put(RMT_VID_BITRATE,500000);
        setMap.put(RMT_VID_QUALITY,80);
        //本地拍照
        setMap.put(PICTURE_WIDTH,320);
        setMap.put(PICTURE_HEIGHT,240);
        setMap.put(PICTURE_QUALITY,80);
        //远程拍照
        setMap.put(RMT_PIC_WIDTH,320);
        setMap.put(RMT_PIC_HEIGHT,240);
        setMap.put(RMT_PIC_QUALITY,80);
        //RTSP
        setMap.put(RTSP_PORT,9408);
        setMap.put(RTSP_VID_WIDTH,320);
        setMap.put(RTSP_VID_HEIGHT,240);
        setMap.put(RTSP_VID_FRAMERATE,20);
    }

    //参数默认值
    //public static final int DEFAULT_VIDEO_WIDTH = ;  //视频宽度

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

    private void initDefaultSp(){
        Iterator it = setMap.entrySet().iterator();
        SharedPreferences.Editor edit = context.getSharedPreferences(Config.SETTING_SP_NAME, context.MODE_PRIVATE).edit();
        while (it.hasNext()){
            Map.Entry entry = (Map.Entry) it.next();
            edit.putInt((String)entry.getKey(),(Integer)entry.getValue());
        }
        edit.commit();
    }
    public static int getSpI(String ss){
        SharedPreferences sp = context.getSharedPreferences(Config.SETTING_SP_NAME, context.MODE_PRIVATE);
        return sp.getInt(ss, (Integer) setMap.get(ss));
    }
    public static String getSpS(String ss){
        SharedPreferences sp = context.getSharedPreferences(Config.SETTING_SP_NAME, context.MODE_PRIVATE);
        return sp.getString(ss, (String) setMap.get(ss));
    }
    public static boolean getSpB(String ss){
        SharedPreferences sp = context.getSharedPreferences(Config.SETTING_SP_NAME, context.MODE_PRIVATE);
        return sp.getBoolean(ss, (boolean) setMap.get(ss));
    }
    public static void setSp(String key, int value){
        SharedPreferences.Editor edit = context.getSharedPreferences(Config.SETTING_SP_NAME, context.MODE_PRIVATE).edit();
        edit.putInt(key,value);
        edit.commit();
    }
    public static void setSp(String key, String value){
        SharedPreferences.Editor edit = context.getSharedPreferences(Config.SETTING_SP_NAME, context.MODE_PRIVATE).edit();
        edit.putString(key, value);
        edit.commit();
    }
    public static void setSp(String key, boolean value){
        SharedPreferences.Editor edit = context.getSharedPreferences(Config.SETTING_SP_NAME, context.MODE_PRIVATE).edit();
        edit.putBoolean(key, value);
        edit.commit();
    }

    public static int getDefaultI(String ss){
        return (int) Config.setMap.get(ss);
    }
    public static String getDefaultS(String ss){
        return (String) Config.setMap.get(ss);
    }
    public static boolean getDefaultB(String ss){
        return (boolean) Config.setMap.get(ss);
    }

}
