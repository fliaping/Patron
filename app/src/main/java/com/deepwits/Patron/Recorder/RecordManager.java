package com.deepwits.Patron.Recorder;

import android.os.Handler;
import android.util.Log;

import com.deepwits.Patron.Config;
import com.deepwits.Patron.StorageManager.StorageManager;
import com.deepwits.Patron.StorageManager.StorageUtil;

import net.majorkernelpanic.streaming.gl2cameraeye.VideoPreview;

import net.majorkernelpanic.streaming.video.VideoStream;

import java.io.IOException;

/**
 * Created by Payne on 1/15/16.
 */
public class RecordManager extends VideoStream{
    private MyTimerTask timedSaveTask;
    private boolean isTimedStop = true;
    private boolean isLockCurrentVideo = false;

    private VideoPreview mVideoCapture;
    private boolean isRecord  = false;
    private boolean isLoopRecord = true;

    public RecordManager(RecordService service) {
        this.mRecordService = service;
        mHandler = mRecordService.getHandler();
        mStorageManager = mRecordService.getStorageManager();
        videoPreview = mRecordService.getVideoPreview();
    }

    private String TAG = "RecordManager";
    private RecordService mRecordService;
    private Handler mHandler = null;
    private StorageManager mStorageManager ;
    private VideoPreview videoPreview;
    //private MediaRecorder mMediaRecorder;  //媒体记录类



    //初始化录像
    public boolean initRecord() {  //surface创建时执行
        Log.d(TAG, "initRecord .......");

        timedSaveTask = new MyTimerTask(mHandler, Config.getDefaultI(Config.VIDEO_DUR), 0) {   //定义定时保存函数
            @Override
            public void timerTask() {
                if (isRecord) {   //正在录像时，停止录像保存文件后再重启
                    if(isLoopRecord){  //若是循环录制,循环录制时停止后重启
                        isTimedStop = true; //停止后自动重启
                        mStopRecord();
                    }else {
                        isTimedStop = false;
                    }

                }
            }
        };
        Log.d(TAG, "initRecord  success .......");
        return false;
    }

    public void loopRecord(boolean isLoopRecord){
        this.isLoopRecord = isLoopRecord;
    }

    public synchronized boolean startRecord() {

        if (!videoPreview.isRecording()) {
            long avai = StorageUtil.getAvailableSize(Config.ROOT);
            try {
                if (avai > Config.SOFT_LIMIT) {                  //大于软限制，正常启动

                        videoPreview.startRecord();

                } else if (avai > Config.HARD_LIMIT) {            //大于硬限制小于软限制可以在后台删除，不影响相机启动
                    mRecordService.getStorageManager().checkStorageAndRecord(false);
                    videoPreview.startRecord();
                } else {                             //小于硬限制时，等待文件删除到软限制时启动
                    mStorageManager.checkStorageAndRecord(true);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else return false;
    }
    public void stopRecord(){
        isTimedStop = false;
        mStopRecord();
    }

    private synchronized boolean mStopRecord() {
        if(videoPreview.isRecording()){
            videoPreview.stopRecord();
            if (isTimedStop) {
                startRecord();
            } else {
                isTimedStop = false;   //不是定时停止，为手动停止，录像标志为假
                //timedSaveTask.pause();
            }
            Log.d(TAG, "mStopRecord success......");
        }
        return true;
    }



    /** Stops the stream. */
    public synchronized void stop() {
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);

            super.stop();
            // We need to restart the preview
            if (!mCameraOpenedManually) {
                destroyCamera();
            } else {
                try {
                    startPreview();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getSessionDescription() throws IllegalStateException {
        return null;
    }


    protected void createContextAndStartCamera(int cameraId) {
        Log.d(TAG, "createContextAndStartCamera: " + cameraId);
        mVideoCapture = VideoPreview.createVideoPreview(mRecordService, mCameraId, 0);
        mVideoCapture.start(640, 480, 30);


        if (mVideoCapture != null) {
            mVideoCapture.startPreview();
        }
    }







}
