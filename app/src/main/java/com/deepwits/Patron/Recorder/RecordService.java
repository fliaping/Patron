package com.deepwits.Patron.Recorder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.deepwits.Patron.DataBase.MediaFileDAOImpl;
import com.deepwits.Patron.StorageManager.StorageManager;

public class RecordService extends Service {
    private MediaFileDAOImpl mfImpl;

    public RecordService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StorageManager storageManager = StorageManager.getInstance(this);
        storageManager.start();
        mfImpl = new MediaFileDAOImpl(this);  //数据库实现类
        new Command(this);  //指令相应类注册
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    public void lockFiles(int[] ids){
        if(ids.length > 0 && mfImpl !=null){
            for(int i = 0;i<ids.length;i++){
                mfImpl.lockFileImpl(ids[i]);
            }
        }
    }

    public void unlockFiles(int[] ids){
        if(ids.length > 0 && mfImpl !=null){
            for(int i = 0;i<ids.length;i++){
                mfImpl.unlockFileImpl(ids[i]);
            }
        }
    }

    public void deleteFiles(int[] ids){
        if(ids.length > 0 && mfImpl !=null){
            for(int i = 0;i<ids.length;i++){
                mfImpl.deleteFileImpl(ids[i]);
            }
        }
    }
}
