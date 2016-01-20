package com.deepwits.Patron.DataBase;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.deepwits.Patron.DataBase.DBOpenHelper;
import com.deepwits.Patron.DefaultConfig;
import com.deepwits.Patron.Recorder.RecordService;
import com.deepwits.Patron.StorageManager.FileResolve;
import com.deepwits.Patron.StorageManager.MediaFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Payne on 1/13/16.
 * 数据库具体操作实现
 */
public class MediaFileDAOImpl {
    private final String TAG = "MediaFileDAOImpl";
    private DBOpenHelper dbOpenHelper;
    private FileResolve fileResolve;
    public MediaFileDAOImpl(RecordService recordService){
        this.dbOpenHelper = new DBOpenHelper(recordService, "Recorder.db", null, 1);
        fileResolve = new FileResolve();
    }
    public boolean add(MediaFile mediaFile) {// 插入记录
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();// 取得数据库操作
        if(0 == mediaFile.getId()){
            db.execSQL("insert into "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+" (filename,path,size,width,height," +
                            "date,duration,thumb_path,latitude,longitude,event_type,media_type,command_type) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[]{ mediaFile.getFilename(), mediaFile.getPath(), mediaFile.getSize(), mediaFile.getWidth(),
                            mediaFile.getHeight(), mediaFile.getDate(), mediaFile.getDuration(),mediaFile.getThumbPath(),
                            mediaFile.getLatitude(),mediaFile.getLongitude(),mediaFile.getEventType(),
                            mediaFile.getMediaType(),mediaFile.getCommandOrign()});
        }
        db.close();// 记得关闭数据库操作
        return true;
    }

    public boolean delete(int id) {// 删除纪录
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        db.execSQL("delete from " + DefaultConfig.DB_MEDIAFILE_TABLE_NAME + " where id=?", new Integer[]{id});
        db.close();
        return true;
    }

    public boolean update(MediaFile mediaFile) {// 修改纪录
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        if(mediaFile.getId()!=0){
            db.execSQL("update " + DefaultConfig.DB_MEDIAFILE_TABLE_NAME +
                            " set filename=?,path=?,size=?,width=?,height=?,date=?,duration=?,thumb_path=?,latitude=?" +
                            " longitude=?,event_type=?,media_type=?,command_type=? where id=?",
                    new Object[]{mediaFile.getFilename(), mediaFile.getPath(), mediaFile.getSize(), mediaFile.getWidth(),
                            mediaFile.getHeight(), mediaFile.getDate(), mediaFile.getDuration(), mediaFile.getThumbPath(),
                            mediaFile.getLatitude(), mediaFile.getLongitude(), mediaFile.getEventType(),
                            mediaFile.getMediaType(), mediaFile.getCommandOrign()});
        }
        db.close();
        return true;
    }

    public MediaFile queryById(int id) {// 根据ID查找纪录
        MediaFile mediaFile = null;
        Cursor cursor = null;
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        if(id > 0){
            // 用游标Cursor接收从数据库检索到的数据
            cursor = db.rawQuery("select * from " + DefaultConfig.DB_MEDIAFILE_TABLE_NAME + " where id=?",
                                new String[]{String.valueOf(id)});
        }
        if(cursor!=null){
            if (cursor.moveToFirst() && !cursor.isNull(0)) {// 依次取出数据
                String path = cursor.getString(cursor.getColumnIndex("path"));
                try {
                    mediaFile = new MediaFile(path);
                    mediaFile.setPath(cursor.getString(cursor.getColumnIndex("path")));
                    mediaFile.setSize(cursor.getLong(cursor.getColumnIndex("size")));
                    mediaFile.setWidth(cursor.getInt(cursor.getColumnIndex("width")));
                    mediaFile.setHeight(cursor.getInt(cursor.getColumnIndex("height")));
                    mediaFile.setDate(cursor.getLong(cursor.getColumnIndex("date")));
                    mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("duration")));
                    mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("thumb_path")));
                    mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("latitude")));
                    mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("longitude")));
                    mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("event_type")));
                    mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("command_origin")));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                    db.close();
                }
            }
        }
        return mediaFile;
    }


    /**
     * 条件查询记录列表
     * @param mediaType  媒体类型,null 为查询全部
     * @param eventType  事件类型,null 为查询全部
     * @param commandOrigin  来源类型,null 为查询全部
     * @return 条件查询结果
     */
    public List<MediaFile> query(MediaFile.MediaType mediaType,MediaFile.EventType eventType,
                                 MediaFile.CommandOrigin commandOrigin) {// 查询所有记录
        List<MediaFile> lists = new ArrayList<MediaFile>();
        MediaFile mediaFile = null;
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        if(mediaType == null && eventType==null && commandOrigin==null){ //全为空,则查询全部 000
            cursor = db.rawQuery("select * from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME, null);
        }
        if(mediaType == null && eventType==null && commandOrigin!=null){ //001
            cursor = db.rawQuery("select * from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where commandOrigin=?",
                    new String[]{String.valueOf(commandOrigin.get())});
        }
        if(mediaType == null && eventType!=null && commandOrigin==null){ //010
            cursor = db.rawQuery("select * from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where eventType=?",
                    new String[]{String.valueOf(eventType.get())});
        }
        if(mediaType == null && eventType!=null && commandOrigin!=null){ //011
            cursor = db.rawQuery("select * from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where eventType=? and commandOrigin=?",
                    new String[]{String.valueOf(eventType.get()),String.valueOf(commandOrigin.get())});
        }
        if(mediaType != null && eventType==null && commandOrigin!=null){ //100
            cursor = db.rawQuery("select * from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=?",
                    new String[]{String.valueOf(mediaType.get())});
        }
        if(mediaType != null && eventType==null && commandOrigin!=null){ //101
            cursor = db.rawQuery("select * from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and commandOrigin=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(commandOrigin.get())});
        }
        if(mediaType != null && eventType!=null && commandOrigin==null){ //110
            cursor = db.rawQuery("select * from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and eventType=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(eventType.get())});
        }
        if(mediaType != null && eventType!=null && commandOrigin!=null){ //111
            cursor = db.rawQuery("select * from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and eventType=? and commandOrigin=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(eventType.get()),String.valueOf(commandOrigin.get())});
        }
        if(cursor!=null){
            while (cursor.moveToNext() && !cursor.isNull(0)) {
                String path = cursor.getString(cursor.getColumnIndex("path"));
                mediaFile = new MediaFile(path);
                mediaFile.setPath(cursor.getString(cursor.getColumnIndex("filename")));
                mediaFile.setPath(cursor.getString(cursor.getColumnIndex("path")));
                mediaFile.setSize(cursor.getLong(cursor.getColumnIndex("size")));
                mediaFile.setWidth(cursor.getInt(cursor.getColumnIndex("width")));
                mediaFile.setHeight(cursor.getInt(cursor.getColumnIndex("height")));
                mediaFile.setDate(cursor.getLong(cursor.getColumnIndex("date")));
                mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("duration")));
                mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("thumb_path")));
                mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("latitude")));
                mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("longitude")));
                mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("event_type")));
                mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("media_type")));
                mediaFile.setDuration(cursor.getInt(cursor.getColumnIndex("command_origin")));
                lists.add(mediaFile);
            }
            cursor.close();
        }
        db.close();
        return lists;
    }

    /**
     * 根据类型查询文件大小
     * @param mediaType  媒体类型,null 为查询全部
     * @param eventType  事件类型,null 为查询全部
     * @param commandOrigin  来源类型,null 为查询全部
     * @return 条件查询结果
     */
    public long getMeidaSize(MediaFile.MediaType mediaType,MediaFile.EventType eventType,MediaFile.CommandOrigin commandOrigin) {
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        if(mediaType == null && eventType==null && commandOrigin==null){ //全为空,则查询全部 000
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME, null);
        }
        if(mediaType == null && eventType==null && commandOrigin!=null){ //001
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where commandOrigin=?",
                    new String[]{String.valueOf(commandOrigin.get())});
        }
        if(mediaType == null && eventType!=null && commandOrigin==null){ //010
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where eventType=?",
                    new String[]{String.valueOf(eventType.get())});
        }
        if(mediaType == null && eventType!=null && commandOrigin!=null){ //011
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where eventType=? and commandOrigin=?",
                    new String[]{String.valueOf(eventType.get()),String.valueOf(commandOrigin.get())});
        }
        if(mediaType != null && eventType==null && commandOrigin!=null){ //100
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=?",
                    new String[]{String.valueOf(mediaType.get())});
        }
        if(mediaType != null && eventType==null && commandOrigin!=null){ //101
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and commandOrigin=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(commandOrigin.get())});
        }
        if(mediaType != null && eventType!=null && commandOrigin==null){ //110
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and eventType=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(eventType.get())});
        }
        if(mediaType != null && eventType!=null && commandOrigin!=null){ //111
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and eventType=? and commandOrigin=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(eventType.get()),String.valueOf(commandOrigin.get())});
        }
        long result = 0;
        if(cursor != null){
            cursor.moveToFirst();
            result = cursor.getLong(0);
            cursor.close();
        }
        db.close();
        return result;
    }

    /**
     * 根据类型条件查询视频时长
     * @param eventType  事件类型
     * @param commandOrigin 指令来源类型
     * @return 条件查询结果
     */
    public int getVideoDuration(MediaFile.EventType eventType,MediaFile.CommandOrigin commandOrigin){    //统计某个类型的视频时长
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        if(eventType == null && commandOrigin == null){  //00
            cursor = db.rawQuery("select sum(duration) from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                                "where mediaType=?",new String[]{String.valueOf(MediaFile.MediaType.VIDEO.get())});
        }
        if(eventType == null && commandOrigin != null){  //01
            cursor = db.rawQuery("select sum(duration) from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                    "where mediaType=? and commandOrigin=?",
                    new String[]{String.valueOf(MediaFile.MediaType.VIDEO.get()), String.valueOf(commandOrigin.get())});
        }
        if(eventType != null && commandOrigin == null){  //10
            cursor = db.rawQuery("select sum(duration) from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            "where mediaType=? and eventType=?",
                    new String[]{String.valueOf(MediaFile.MediaType.VIDEO.get()), String.valueOf(eventType.get())});
        }
        if(eventType != null && commandOrigin != null){   //11
            cursor = db.rawQuery("select sum(duration) from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            "where mediaType=? and eventType=? and commandOrigin=?",
                    new String[]{String.valueOf(MediaFile.MediaType.VIDEO.get()), String.valueOf(eventType.get()),String.valueOf(commandOrigin.get())});
        }
        int result = 0;
        if(cursor != null){
            cursor.moveToFirst();
            result = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return result;
    }

    /**
     * 根据条件查询媒体文件个数
     * @param mediaType  媒体类型,null 为查询全部
     * @param eventType  事件类型,null 为查询全部
     * @param commandOrigin  来源类型,null 为查询全部
     * @return 条件查询结果
     */
    public int getMeidaCount(MediaFile.MediaType mediaType,MediaFile.EventType eventType,MediaFile.CommandOrigin commandOrigin) {
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        if(mediaType == null && eventType==null && commandOrigin==null){ //全为空,则查询全部 000
            cursor = db.rawQuery("select count(*) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME, null);
        }
        if(mediaType == null && eventType==null && commandOrigin!=null){ //001
            cursor = db.rawQuery("select count(*) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where commandOrigin=?",
                    new String[]{String.valueOf(commandOrigin.get())});
        }
        if(mediaType == null && eventType!=null && commandOrigin==null){ //010
            cursor = db.rawQuery("select count(*) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where eventType=?",
                    new String[]{String.valueOf(eventType.get())});
        }
        if(mediaType == null && eventType!=null && commandOrigin!=null){ //011
            cursor = db.rawQuery("select count(*) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where eventType=? and commandOrigin=?",
                    new String[]{String.valueOf(eventType.get()),String.valueOf(commandOrigin.get())});
        }
        if(mediaType != null && eventType==null && commandOrigin!=null){ //100
            cursor = db.rawQuery("select count(*) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=?",
                    new String[]{String.valueOf(mediaType.get())});
        }
        if(mediaType != null && eventType==null && commandOrigin!=null){ //101
            cursor = db.rawQuery("select count(*) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and commandOrigin=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(commandOrigin.get())});
        }
        if(mediaType != null && eventType!=null && commandOrigin==null){ //110
            cursor = db.rawQuery("select sum(size) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and eventType=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(eventType.get())});
        }
        if(mediaType != null && eventType!=null && commandOrigin!=null){ //111
            cursor = db.rawQuery("select count(*) from "+ DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and eventType=? and commandOrigin=?",
                    new String[]{String.valueOf(mediaType.get()),String.valueOf(eventType.get()),String.valueOf(commandOrigin.get())});
        }
        int result = 0;
        if(cursor != null){
            cursor.moveToFirst();
            result = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return result;
    }
    /**
     * 查询最老的文件
     * @param mediaType 媒体类型
     * @param eventType 事件类型
     * @return 最老文件的路径
     */
    public String mostOldFilePath(MediaFile.MediaType mediaType,MediaFile.EventType eventType){
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        if(eventType == null && mediaType == null){  //00
            cursor = db.rawQuery("select * from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where (date in (select min(date) from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " ))",null);
        }
        if(eventType == null && mediaType != null){  //01
            cursor = db.rawQuery("select * from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where (date in (select min(date) from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? ))",
                    new String[]{ String.valueOf(mediaType.get())});
        }
        if(eventType != null && mediaType == null){  //10
            cursor = db.rawQuery("select * from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where (date in (select min(date) from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where eventType=?))",
                    new String[]{ String.valueOf(eventType.get())});
        }
        if(eventType != null && mediaType != null){   //11
            cursor = db.rawQuery("select * from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where (date in (select min(date) from "+DefaultConfig.DB_MEDIAFILE_TABLE_NAME+
                            " where mediaType=? and eventType=?))",
                    new String[]{ String.valueOf(mediaType.get()),String.valueOf(eventType.get())});
        }
        String result = null;
        if(cursor!=null){
            cursor.moveToFirst();
            if(cursor.getInt(cursor.getColumnIndex("id")) > 0) //保证有数据
                result = cursor.getString(cursor.getColumnIndex("path"));
            cursor.close();
        }
        db.close();
        return  result;//返回最老正常视频文件路径
    }

}
