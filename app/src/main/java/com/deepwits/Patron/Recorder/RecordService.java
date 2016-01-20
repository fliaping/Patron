package com.deepwits.Patron.Recorder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RecordService extends Service {
    public RecordService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
