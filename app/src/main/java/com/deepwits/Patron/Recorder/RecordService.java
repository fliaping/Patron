package com.deepwits.Patron.Recorder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.deepwits.Patron.StorageManager.StorageManager;

public class RecordService extends Service {
    public RecordService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StorageManager storageManager = StorageManager.getInstance(this);
        storageManager.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
