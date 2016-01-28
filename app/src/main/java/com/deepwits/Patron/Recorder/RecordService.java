package com.deepwits.Patron.Recorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.deepwits.Patron.Config;
import com.deepwits.Patron.DataBase.MediaFileDAOImpl;
import com.deepwits.Patron.R;
import com.deepwits.Patron.StorageManager.StorageManager;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl2cameraeye.VideoPreview;
import net.majorkernelpanic.streaming.video.VideoQuality;

public class RecordService extends Service {
    public final String TAG = "RecordService";
    private MediaFileDAOImpl mfImpl;
    private Handler handler ;
    private StorageManager storageManager;
    private VideoPreview mVideoCapture;
    private int mCameraId;
    private View mRecordView;
    private ViewGroup  mPreview;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mRecorderViewParams;
    private PatronServer mPatronServer;


    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Config.ok(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler = new Handler();
        storageManager = StorageManager.getInstance(this);  //存储管理对象
        storageManager.start();   //启动存储线程
        mfImpl = new MediaFileDAOImpl(this);  //数据库实现类
        new Command(this);  //指令相应类注册

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mRecordView = LayoutInflater.from(this).inflate(R.layout.sidebar_layout, null);
        mPreview = (ViewGroup) mRecordView.findViewById(R.id.mPreview);

        mRecorderViewParams = new WindowManager.LayoutParams();
        mRecorderViewParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mRecorderViewParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mRecorderViewParams.width = 200;
        mRecorderViewParams.height = 200;
        mRecorderViewParams.x = 1;//widgetBgLayout.getWidth();
        mRecorderViewParams.y = 1;
        mWindowManager.addView(mRecordView, mRecorderViewParams);

        registTestBroadcast();
        mVideoCapture = VideoPreview.createVideoPreview(this, mCameraId, 0);

        //createContextAndStartCamera(0);

        SessionBuilder.getInstance()
                .setContext(this)
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(new VideoQuality(640,80,30,(1280*720)<<3));
        mPatronServer = new PatronServer(this);
        //mPatronServer.setTest();
        mPatronServer.onCreate();
    }

    public void open(){
        if(!mVideoCapture.isRecording()){
            try {
                mVideoCapture.setOutputFile(getStoreFileName());
                mVideoCapture.startRecord();
                //mVideoCapture.startShortVideo();
            } catch (Throwable e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }else{
            mVideoCapture.stopRecord();
            //mVideoCapture.stopShortVideo();
        }
    }
    private String getStoreFileName() {
        return Config.NORMAL_VIDEO_PATH + "NORMAL_" +System.currentTimeMillis() + ".mp4";
    }


    protected void createContextAndStartCamera(int cameraId) {
        Log.d(TAG, "createContextAndStartCamera: " + cameraId);

        mVideoCapture.start(640, 480, 30);

        this.mPreview.addView(mVideoCapture.getSurfaceView());

        if (mVideoCapture != null) {
            mVideoCapture.startPreview();
        }

    }

    public VideoPreview getVideoPreview(){
        return this.mVideoCapture;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Handler getHandler(){
        return handler;
    }
    public StorageManager getStorageManager(){
        return storageManager;
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
    public void rtsp(){
        if(!mVideoCapture.isStreaming()){
            try {
                mVideoCapture.startStream();
            } catch (Throwable e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }else{
            mVideoCapture.stopStream();
        }
    }

    private void registTestBroadcast(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("xuping_recorder");
        registerReceiver(new TestBroadcast(), intentFilter);
    }
    private boolean isShow = true;

    class TestBroadcast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra("type");
            Toast.makeText(RecordService.this,"xuping_recorder"+type,Toast.LENGTH_SHORT).show();
            switch (type){
                case "preview" :
                    if(isShow){
                        if(!mVideoCapture.isPreview()){
                            createContextAndStartCamera(0);
                        }
                        mRecorderViewParams.width = 200;
                        mRecorderViewParams.height = 200;
                        mWindowManager.updateViewLayout(mRecordView,mRecorderViewParams);
                        isShow = false;
                    }else {
                        mRecorderViewParams.width = 1;
                        mRecorderViewParams.height = 1;
                        mWindowManager.updateViewLayout(mRecordView,mRecorderViewParams);
                        isShow = true;
                        //mVideoCapture.stopPreview();
                    }
                    break;
                case "record":
                    open();break;
                case "stream":
                    rtsp();break;
            }
        }
    }
}
