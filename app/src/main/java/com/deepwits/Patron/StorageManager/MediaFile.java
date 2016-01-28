package com.deepwits.Patron.StorageManager;

import android.util.Log;

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
    private int event_type;   //事件类型
    private int media_type;   //媒体类型
    private int command_origin; //指令来源

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

    public enum CommandOrigin {   //指令来源
        WECHAT(1), APP(2);
        private int i;
        CommandOrigin(int i) { this.i = i; }
        public int get() { return this.i; }
    }

    //构造函数
    public MediaFile(){

    }
    public MediaFile(String path){
        setPath(path);
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

    public int getEventType() {
        return event_type;
    }

    public int getMediaType() {
        return media_type;
    }

    public int getCommandOrign() {
        return command_origin;
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

    public void setId(int id){
        this.id = id;
    }
    public void setFilename(String filename) {
        if(null == filename){               //参数为空,则根据路径分割出文件名
            this.filename = splitFileName();
        }else {                         //参数不为空,根据参数设置
            this.filename = filename;
        }
    }

    public void setPath(String path) {
        this.path = path;
        if(null != path){
            setFilename(null);  //有了路径后就可自动设置文件名和媒体类型
            setMediaType(-1);
        }
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

    public void setEventType(int event_type) {
        this.event_type = event_type;
    }

    public void setMediaType(int media_type) {
        if(-1 == media_type){               //若参数为空,则根据路径判断媒体类型
            String tmp = splitPostfix(path);
            if("mp4".equalsIgnoreCase(tmp)){
                this.media_type = MediaType.VIDEO.get();
            }
            if("jpg".equalsIgnoreCase(tmp)){
                this.media_type = MediaType.PICTURE.get();
            }
        }else {                         //若参数不为空,则根据参数设置
            this.media_type = media_type;
        }
    }

    public void setCommandOrign(int command_orign) {
        this.command_origin = command_orign;
    }

    /**
     * 从路径中分割出文件名
     * @return
     */
    private String splitFileName(){
        int tmp = path.lastIndexOf(File.separator);
        if(tmp>0 && tmp<path.length()){
            return path.substring(tmp+1,path.length());
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
            return src.substring(tmp+1,src.length());
        }else {
            return null;
        }
    }

    public void print(String TAG){
        Log.e(TAG,"id: "+this.getId());
        Log.e(TAG,"filename: "+this.getFilename());
        Log.e(TAG,"path: "+this.getPath());
        Log.e(TAG,"size: "+this.getSize());
        Log.e(TAG,"width: "+this.getWidth());
        Log.e(TAG,"height: "+this.getHeight());
        Log.e(TAG,"date: "+this.getDate());
        Log.e(TAG,"duration: "+this.getDuration());
        Log.e(TAG,"thumb_path: "+this.getThumbPath());
        Log.e(TAG,"latitude: "+this.getLatitude());
        Log.e(TAG,"longitude: "+this.getLongitude());
        Log.e(TAG,"event_type: "+this.getEventType());
        Log.e(TAG,"media_type: "+this.getMediaType());
        Log.e(TAG,"command_origin: "+this.getCommandOrign());

    }
}
