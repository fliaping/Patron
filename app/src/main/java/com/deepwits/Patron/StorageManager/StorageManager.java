package com.deepwits.Patron.StorageManager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.deepwits.Patron.DataBase.MediaFileDAOImpl;
import com.deepwits.Patron.Config;
import com.deepwits.Patron.Recorder.RecordService;

import java.io.File;
import java.io.IOException;
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
    //-------单例----

    private Looper mLooper;
    private FileResolve fileResolve;
    private static RecordService mService;
    private MediaFileDAOImpl mfImpl;
    private Handler mHandler;
    private final long timeToSync = 3 * 1000 * 60;  //默认3分钟定时同步
    /**
     * 设置软硬限制的目的是存储管理线程的定时删除
     */
    private final long softLimit = Config.SOFT_LIMIT;  //默认存储软限制500M
    private final long hardLimit = Config.HARD_LIMIT;  //默认存储硬限制150M
    private boolean isFirst = true;
    public boolean isScanned = false;


    @Override
    public void run() {
        super.run();

        try {
            Config.ok(mService); //check storage
            Log.e(TAG,"Config.ok "  + Config.APP_PATH);
            makedirs();   //make all directory
        } catch (Exception e) {
            e.printStackTrace();
        }
        mfImpl = new MediaFileDAOImpl(mService);
        fileResolve = new FileResolve();
        //存储线程方法
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            mHandler = new Handler();
            notifyAll();
        }
        mHandler.postDelayed(syncTask, 1000 * 5); //５秒后开始同步数据库
        Looper.loop();
    }
    public void addToDB(String path) {  //添加文件到数据库
        mHandler.post(new AddRunnable(path));
    }

    /**
     * 检查剩余空间,如果剩余空间不足,等待删除文件后再启动
     * @param isRecord
     */
    public void checkStorageAndRecord(boolean isRecord) {
        long avai = StorageUtil.getAvailableSize(Config.ROOT);
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
            if (f.isDirectory() && !f.isHidden()) {
                final File[] fileArray = f.listFiles();
                if (fileArray != null) {
                    for (int i = 0; i < fileArray.length; i++) {
                        //递归调用
                        if (!fileArray[i].isDirectory()) {
                            MediaFile mediaFile = new MediaFile(fileArray[i].getPath());
                            fileResolve.resolve(mediaFile); //解析文件
                            list.add(mediaFile);
                        } else {
                            if(!fileArray[i].isHidden()){  //不扫描隐藏文件夹
                                fileList(fileArray[i], list, path, tempList);
                            }
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
    }

    public void syncMediaFile() {
        long pre = System.currentTimeMillis();
        List<MediaFile> storageList = new ArrayList<MediaFile>();
        long prescan = System.currentTimeMillis();
        Log.e(TAG,"APP_PATH"+Config.APP_PATH);
        getFileList(Config.APP_PATH, storageList); //存储中视频文件列表
        long comparetime = System.currentTimeMillis();
        Log.v(TAG, "扫描文件用时为:" + (comparetime - prescan));
        List<MediaFile> dbList = mfImpl.query(null,null,null);
        if(dbList == null)  Log.e(TAG,"dbList is null");
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
            mediaMap.put(mediaFile.getFilename(), 1);   //将map中的value设为文件类型（1）
            Log.e(TAG, "mediaMap:" + mediaFile.getFilename() + " 1" );
            fileMap.put(mediaFile.getFilename(), mediaFile);  //将视频文件插入检索表
        }

            for (MediaFile db : dbList) {            //遍历数据库中视频列表
                Log.e(TAG, "dbList:" + db.getFilename()  );
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
                Log.v(TAG, "mediaMap insert :" + key + " " + value);
            } else if (value == -1) {  //删除本地文件中没有的数据记录
                mfImpl.delete(file.getId());
                viddelete++;
                Log.v(TAG, "mediaMap delete :" + key + " " + value);
            } else if (value == -2) {
                mfImpl.update(file);
                vidupdate++;
                Log.v(TAG, "mediaMap update :" + key + " " + value);
            }
        }

    }


    private boolean cleanStorage() {
        long pre = System.currentTimeMillis();
        Log.e(TAG, "start clean");
        boolean isok = false;
        int count = 0;
        delOldMediaFile();
        long totalSize = StorageUtil.getTFTotalSize(Config.ROOT);
        long availableSize = StorageUtil.getAvailableSize(Config.ROOT);
        int videocount = mfImpl.getMeidaCount(MediaFile.MediaType.VIDEO, null, null);
        long myTotalUsedSize = mfImpl.getMeidaSize(null,null,null);
        if (availableSize < softLimit) {   //可用空间小于软限制且可用空间与本应用所用空间和大于软限制
            if ((availableSize + myTotalUsedSize) > softLimit) {
                while (StorageUtil.getAvailableSize(Config.ROOT) < softLimit && count < videocount) {           //循环删除，直到可用空间大于软限制
                    delOldMediaFile();
                    count++;
                }
                if (StorageUtil.getAvailableSize(Config.ROOT) > softLimit) {
                    isok = true;
                } else {        ///循环删除未能使剩余空间大于软限制
                    Toast.makeText(mService, "录像空间不足(剩余：" + StorageUtil.getAvailableSize(Config.ROOT) / 1024 / 1024 + "M)，请手动清理存储。", Toast.LENGTH_LONG).show();
                    isok = false;
                }
            } else {// 可用空间小于软限制 且可用空间与本应用所用空间 和 小于软限制
                syncMediaFile(); //有可能是数据库没同步，强制同步数据库
                long myMediaAndAvailable = mfImpl.getMeidaSize(null,null,null) + StorageUtil.getAvailableSize(Config.ROOT);
                if (myMediaAndAvailable < Config.SOFT_LIMIT) {
                    Toast.makeText(mService, "SD卡剩余空间低于" + hardLimit / 1024 / 1024 + "M,请手动清理存储", Toast.LENGTH_LONG).show();
                }
                isok = true;
                if (availableSize < hardLimit) {
                    Toast.makeText(mService, "存储空间低于最低限制，录像将停止", Toast.LENGTH_LONG).show();
                    isok = false;
                }
            }
        } else {
            isok = true;
        }

        Log.v(TAG, "清理文件用时：" + (System.currentTimeMillis() - pre) + "ms");
        return isok;
    }

    private void delOldMediaFile(){  //删除最老视频文件
        String oldFilePath = mfImpl.mostOldFilePath(MediaFile.MediaType.VIDEO, MediaFile.EventType.NORMAL);
        new File(oldFilePath).delete();
    }

    private void makedirs(){
        File file = new File(Config.NORMAL_VIDEO_DIR);
        if(!file.exists()) file.mkdirs();
        file = new File(Config.LOCK_VIDEO_PATH);
        if(!file.exists()) file.mkdirs();
        file = new File(Config.UPLOAD_VIDEO_PATH);
        if(!file.exists()) file.mkdirs();
        file = new File(Config.TAKE_PICTURE_PATH);
        if(!file.exists()) file.mkdirs();
        file = new File(Config.UPLOAD_PICTURE_PATH);
        if(!file.exists()) file.mkdirs();
        file = new File(Config.THUMBNAIL_PATH);
        if(!file.exists()) {
            file.mkdirs();
            Log.e(TAG,"dir create "+ Config.THUMBNAIL_PATH);
        }
    }
    public MediaFileDAOImpl getMfImpl(){
        return mfImpl;
    }
    /*<!-- 在SDCard中创建与删除文件权限 -->
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
    <!-- 往SDCard写入数据权限 -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />*/
}
