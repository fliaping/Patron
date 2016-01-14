package com.deepwits.Patron.StorageManager;

import java.io.File;

/**
 * Created by Payne on 1/13/16.
 * 自定义媒体文件
 */
public class MediaFile {

    private final String TAG = "MediaFile";
    private int id;    //ID
    private String filename;  //文件名
    private String path;    //路径(带文件名)
    private long size;     //大小
    private int width;  //宽度
    private int height; //高度
    private long date;  //日期
    private long duration;  //时长
    private float latitude; //纬度
    private float longitude;   //经度
    private String thumb_path;  //缩略图路径
    private EventType event_type;   //事件类型
    private MediaType media_type;   //媒体类型
    private CommandOrign command_orign; //指令来源

    public enum EventType {   //事件类型
        NORMAL(1), LOCKED(2), UPLOAD(3);
        private int i;
        EventType(int i) { this.i = i; }
        public int get() { return this.i;}
    }

    public enum MediaType {   //媒体类型
        VIDEO(1), PICTURE(2);
        private int i;
        MediaType(int i) { this.i = i; }
        public int get() { return this.i; }
    }

    public enum CommandOrign {   //指令来源
        WECHAT(1), APP(2);
        private int i;
        CommandOrign(int i) { this.i = i; }
        public int get() { return this.i; }
    }

    //构造函数
    public MediaFile() {
    }
    public MediaFile(String path) {
        this.path = path;
        if(null!=path){
            setFilename(null);
        }
    }

    public int getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getDate() {
        return date;
    }

    public long getDuration() {
        return duration;
    }

    public String getThumbPath() {
        return thumb_path;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public EventType getEventType() {
        return event_type;
    }

    public MediaType getMediaType() {
        return media_type;
    }

    public CommandOrign getCommandOrign() {
        return command_orign;
    }

    /**
     * 获取文件所在文件夹路径
     * @param src 路径
     * @return 文件夹路径
     */
    private String getDir(String src){
        int tmp = src.lastIndexOf(File.separator);
        return src.substring(0,tmp);
    }

    public void setFilename(String filename) {
        if(null != path){               //路径不为空,则根据路径分割出文件名
            this.filename = splitFileName();
        }else {                         //路径为空,根据参数设置
            this.filename = filename;
        }
    }

    public void setPath(String path) {
        this.path = path;
        setFilename(null);  //有了路径后就可自动设置文件名和媒体类型
        setMediaType(null);
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setThumbPath(String thumb_path) {
        this.thumb_path = thumb_path;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public void setEventType(EventType event_type) {
        this.event_type = event_type;
    }

    public void setMediaType(MediaType media_type) {
        if(null != path){               //若路径不为空,则根据路径判断媒体类型
            String tmp = splitPostfix(path);
            if("mp4".equalsIgnoreCase(tmp)){
                this.media_type = MediaType.VIDEO;
            }
            if("jpg".equalsIgnoreCase(tmp)){
                this.media_type = MediaType.PICTURE;
            }
        }else {                         //若路径为空,则根据参数设置
            this.media_type = media_type;
        }
    }

    public void setCommandOrign(CommandOrign command_orign) {
        this.command_orign = command_orign;
    }

    /**
     * 从路径中分割出文件名
     * @return
     */
    private String splitFileName(){
        int tmp = path.lastIndexOf(File.separator);
        if(tmp>0 && tmp<path.length()){
            return path.substring(tmp+1,path.length()-1);
        }else {
            return null;
        }
    }

    /**
     * 从路径中分割出后缀名
     * @param src
     * @return
     */
    private String splitPostfix(String src){
        int tmp = src.lastIndexOf(".");
        if(tmp>0 && tmp<src.length()){
            return src.substring(tmp+1,src.length()-1);
        }else {
            return null;
        }
    }
}
