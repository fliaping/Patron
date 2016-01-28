package net.majorkernelpanic.streaming;

/**
 * Created by Payne on 1/27/16.
 */
public class Setting {
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
}
