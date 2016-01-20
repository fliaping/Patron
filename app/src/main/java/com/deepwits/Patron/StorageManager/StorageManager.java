package com.deepwits.Patron.StorageManager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.deepwits.Patron.DataBase.MediaFileDAOImpl;
import com.deepwits.Patron.DefaultConfig;
import com.deepwits.Patron.Recorder.RecordService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by xp on 15-9-23.
 * 存储管理线程
 */
public class StorageManager extends Thread {
    private static final String TAG = "StorageManager";
    //静态内部类的单例模式
    private StorageManager() {
    }
    private static class StorageManagerHolder {
        private static final StorageManager INSTANCE = new StorageManager();
    }
    public static final StorageManager getInstance(RecordService mService) {
        StorageManager SM = StorageManagerHolder.INSTANCE;
        SM.mService = mService;
        return SM;
    }

    private Looper mLooper;
    private FileResolve fileResolve;
    private static RecordService mService;
    private MediaFileDAOImpl mfImpl;
    private Handler mHandler;
    private final long timeToSync = 3 * 1000 * 60;  //默认3分钟定时同步
    /**
     * 设置软硬限制的目的是存储管理线程的定时删除
     */
    private final long softLimit = DefaultConfig.SOFT_LIMIT;  //默认存储软限制500M
    private final long hardLimit = DefaultConfig.HARD_LIMIT;  //默认存储硬限制150M
    private boolean isFirst = true;
    public boolean isScanned = false;


    @Override
    public void run() {
        super.run();
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            mHandler = new Handler();
            notifyAll();
        }
        mfImpl = new MediaFileDAOImpl(mService);
        fileResolve = new FileResolve();
        MediaFile mf = new MediaFile();
        /*mf.setPath("/sdcard/xx.mp4");
        mf.setDate(12323);
        mfImpl.add(mf);*/



        mHandler.postDelayed(syncTask, 1000 * 5); //５秒后开始同步数据库
        Looper.loop();
    }

    public void addToDB(String path) {  //添加文件到数据库
        mHandler.post(new AddRunnable(path));
    }

    public void checkStorageAndRecord(boolean isRecord) {
        long avai = StorageUtil.getAvailableSize(DefaultConfig.ROOT);
        if (!isRecord) {            //大于硬限制可以在后台删除，不影响相机启动
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    cleanStorage();
                }
            });
        } else {                             //小于软限制时，等待文件删除到软限制时启动
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (cleanStorage()) {
                        //mService.startRecord();
                    }
                }
            });
        }
    }

    private Runnable syncTask = new Runnable() {
        @Override
        public void run() {
            syncMediaFile();
            mHandler.postDelayed(syncTask, timeToSync);
        }
    };

    private class AddRunnable implements Runnable {  //数据库写入异步方法
        private String path;

        public AddRunnable(String path) {
            this.path = path;
        }
        @Override
        public void run() {
            if (path == null) return;
            MediaFile mediaFile = new MediaFile(path);
            fileResolve.resolve(mediaFile);
            mfImpl.add(mediaFile);
        }
    }

    /**
     * @param f        递归遍历文件
     * @param list     所有文件结果列表
     * @param path     要遍历的文件夹路径
     * @param tempList 正在占用视频文件列表
     */
    public void fileList(File f, List<MediaFile> list, String path, List<File> tempList) {
        if (f != null && list != null) {
            if (f.isDirectory()) {
                final File[] fileArray = f.listFiles();
                if (fileArray != null) {
                    for (int i = 0; i < fileArray.length; i++) {
                        //递归调用
                        if (!fileArray[i].isDirectory()) {
                            String fileName = fileArray[i].getName();
                            String prefix = StorageUtil.getFilePrefix(fileName);
                            if ((path.equalsIgnoreCase(DefaultConfig.VIDEOS_DIR) && !prefix.equalsIgnoreCase("mp4")) ||    //删除文件中非mp4的文件
                                    (path.equalsIgnoreCase(DefaultConfig.PICTURES_DIR) && !prefix.equalsIgnoreCase("jpg"))     //删除文件中非jpg的文件
                                    ) {
                                if (prefix.equalsIgnoreCase("temp")) {    //正在占用的视频文件不删
                                    tempList.add(fileArray[i]);
                                } else {
                                    fileArray[i].delete();
                                    Log.e(TAG, "fileList delete " + fileArray[i].getAbsolutePath() + " prefix:" + prefix);
                                }
                                continue;
                            }
                            MediaFile mediaFile = new MediaFile(fileArray[i].getPath());
                            Log.e(TAG,mediaFile.getPath());
                            fileResolve.resolve(mediaFile); //解析文件
                            list.add(mediaFile);
                        } else {
                            if (fileArray[i].getName().toCharArray()[0] == '.') {    //删除隐藏的空文件夹
                                Log.e(TAG, "delete hidden file " + fileArray[i].getName());
                                fileArray[i].delete();
                            }
                            fileList(fileArray[i], list, path, tempList);
                        }
                    }
                }
            }
        }
    }

    /**
     * @param path 要遍历的文件夹路径
     * @param list 所有文件结果列表
     */
    public void getFileList(String path, List<MediaFile> list) {
        List<File> tempList = new ArrayList<File>();
        fileList(new File(path), list, path, tempList);

        //删除正常视频文件夹中的(xxxx-xx-xx)空文件夹
        File directory = new File(DefaultConfig.NORMAL_VIDEO_PATH);
        File[] normalvideo = directory.listFiles();
        if (normalvideo != null) {
            for (int i = 0; i < normalvideo.length; i++) {
                if (normalvideo[i].isDirectory() && normalvideo[i].listFiles().length == 0)
                    normalvideo[i].delete();                //删除空文件夹
                if (!normalvideo[i].isDirectory())
                    normalvideo[i].delete();   //删除Videos目录下文件，因为文件在Videos/xxxx-xx-xx/目录中
            }
        }
        //删除锁定视频文件夹中的空文件夹
        File[] lockvideo = new File(DefaultConfig.LOCK_VIDEO_PATH).listFiles();
        if (lockvideo != null) {
            for (int i = 0; i < lockvideo.length; i++) {
                if (lockvideo[i].isDirectory() && lockvideo[i].listFiles().length == 0)
                    lockvideo[i].delete();                //删除空文件夹
           /* if(!lockvideo[i].isDirectory()) lockvideo[i].delete();  */
            }
        }

        //启动删除多余的temp文件
        File newTempFile = null;
        for (int i = 0; i < tempList.size(); i++) {
            if (newTempFile == null) {
                newTempFile = tempList.get(i);
            } else {
                if (newTempFile.lastModified() < tempList.get(i).lastModified()) {
                    newTempFile.delete();
                    newTempFile = tempList.get(i);
                } else {
                    tempList.get(i).delete();
                }
            }
        }

    }

    public void syncMediaFile() {
        long pre = System.currentTimeMillis();
        List<MediaFile> storageList = new ArrayList<MediaFile>();
        long prescan = System.currentTimeMillis();
        getFileList(DefaultConfig.APP_PATH, storageList); //存储中视频文件列表
        long comparetime = System.currentTimeMillis();
        List<MediaFile> dbList = mfImpl.query(null,null,null);
        Map<String, MediaFile> fileMap = new HashMap<String, MediaFile>();  //所有文件的 <文件名-文件对象>映射表，便于插入时查询检索
        /**
         * 利用videoMap<key,value>的数据结构，key为文件名，value的初值为文件类型(大于0)，
         * 找出本地文件(storageVideoList)和数据库文件(dbVideoList)的差异。
         *
         *视频数据库同步算法：
         *      1.取得本地文件列表，全部插入videoMap中。
         *      2.将数据库取得的数据插入videoMap中,
         *        (1) 若原表中包含数据库中某条记录，检测如果类型和大小相同，不做改动(value 为0)，若类型不同，则更新数据库(value 为-2)
         *        (2) 若不包含该条记录，需要从数据库中删除(value 为-1)。
         * 注意：视频文件开始时会被读到数据库，因此通过判断大小，在下次扫描时更新文件。
         *
         * 所以： 当value大于0是要插入数据库的, value为0是不做改动的，为-1是应该从数据库中删除的记录,为-2是应该从数据库更新的
         */
        long presync = System.currentTimeMillis();
        Map<String, Integer> mediaMap = new HashMap<String, Integer>();   //视频map表，用于标记数据库更新类型
        for (MediaFile mediaFile : storageList) {
            //Log.v(TAG,"localVideo:" + video.getFilename());
            mediaMap.put(mediaFile.getFilename(), mediaFile.getEventType());   //将map中的value设为文件类型（大于0）
            fileMap.put(mediaFile.getFilename(), mediaFile);  //将视频文件插入检索表
        }
        for (MediaFile db : dbList) {            //遍历数据库中视频列表
            if (mediaMap.containsKey(db.getFilename())) {    //原表中包含数据库中某条记录
                MediaFile localFile = fileMap.get(db.getFilename());   //从检索表获取本地文件对象

                if (db.getEventType() == localFile.getEventType() && db.getSize() == localFile.getSize()) {   //类型大小相同,value置为0,不更新
                    mediaMap.put(db.getFilename(), 0);
                } else {                                          //类型不同，value置为-2，更新数据库
                    mediaMap.put(db.getFilename(), -2);
                }
                //Log.v(TAG,"Video:"+localFile.getFilename()+"  " + dbvideo.getFilename() + "result:" + videoMap.get(dbvideo.getFilename()));
            } else {                                          //原表中不包含该条记录,value置为-1，添加到数据库
                mediaMap.put(db.getFilename(), -1);
                //Log.v(TAG, "Video:" + "-----------------" + "  " + dbvideo.getFilename() + "result:" + videoMap.get(dbvideo.getFilename()));
            }
        }

        int picinsert = 0, picupdate = 0, picdelete = 0, picFailInsert = 0;
        int vidinsert = 0, vidupdate = 0, viddelete = 0, vidFailInsert = 0;

        Iterator videoIt = mediaMap.entrySet().iterator();
        while (videoIt.hasNext()) {
            Map.Entry entry = (Map.Entry) videoIt.next();
            String key = (String) entry.getKey();
            int value = (int) entry.getValue();
            MediaFile file = fileMap.get(key);
            if (value > 0) {   //添加新增的视频记录
                //Log.e(TAG,"新增视频:"+key);
                if (!mfImpl.add(file))  //往数据库添加文件记录
                {
                    vidFailInsert++;
                }
                vidinsert++;
                Log.v(TAG, "videoMap:" + key + " " + value);
            } else if (value == -1) {  //删除本地文件中没有的数据记录
                mfImpl.delete(file.getId());
                viddelete++;
            } else if (value == -2) {
                mfImpl.update(file);
                vidupdate++;
            }
        }

    }



    private boolean cleanStorage() {

        return true;
    }
    public MediaFileDAOImpl getMfImpl(){
        return mfImpl;
    }
    /*<!-- 在SDCard中创建与删除文件权限 -->
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
    <!-- 往SDCard写入数据权限 -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />*/
}
