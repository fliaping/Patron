package com.deepwits.Patron.Recorder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import com.deepwits.Patron.DefaultConfig;

/**
 * Created by Payne on 1/21/16.
 * 指令相应类
 */
public class Command {
    public final String TAG = "Command";
    private ActionBroadcastReceiver mActionBroadcastReceiver;
    private SettingBroadcastReceiver mSettingBroadcastReceiver;
    RecordService mRecordService ;
    public Command(RecordService service){
        this.mRecordService = service;

        registerActionBroadcast();  //注册action广播
        registerSettingBroadcast();  //注册setting广播
    }

    private void registerActionBroadcast(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DefaultConfig.ACTION_BROADCAST);
        mActionBroadcastReceiver = new ActionBroadcastReceiver();
        mRecordService.registerReceiver(mActionBroadcastReceiver,intentFilter);
        Log.v(TAG, "registerActionBroadcast");
    }

    private void registerSettingBroadcast(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DefaultConfig.SETTING_BROADCAST);
        mSettingBroadcastReceiver = new SettingBroadcastReceiver();
        mRecordService.registerReceiver(mSettingBroadcastReceiver,intentFilter);
        Log.v(TAG,"registerActionBroadcast");
    }




    class ActionBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG,"ActionBroadcast Received");
            String action_name = intent.getStringExtra("action_name");
            if(action_name == null) return;
            switch (action_name){
                case "start_record" :
                    //mRecordService.startRecord();
                case "stop_record" :
                    //mRecordService.stopRecord();
                case "capture_picture" :
                    //mRecordService.capturePicture();
                case "voice_on" :
                    //mRecordService.voiceOn();
                case "voice_off" :
                    //mRecordService.voiceOff();
                case "loop_record_on" :
                    //mRecordService.loopRecordOn();
                case "loop_record_off" :
                    //mRecordService.loopRecordOff();
                case "lock_file" :
                    String action = intent.getStringExtra("action");
                    int fileIds[] = intent.getIntArrayExtra("file_id");
                    if(action.equalsIgnoreCase("lock")){
                        mRecordService.lockFiles(fileIds);
                    }else if(action.equalsIgnoreCase("unlock")){
                        mRecordService.unlockFiles(fileIds);
                    }else {
                        Log.e(TAG,"未定义指令 lock_file - "+action);
                    }
                case "delete_file" :
                    int fileIds_1[] = intent.getIntArrayExtra("file_id");
                    mRecordService.deleteFiles(fileIds_1);

            }
        }

    }



    class SettingBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String videoWidth = intent.getStringExtra(DefaultConfig.VIDEO_WIDTH);
            String videoHeight = intent.getStringExtra(DefaultConfig.VIDEO_HEIGHT);
            String videoFramerate = intent.getStringExtra(DefaultConfig.VIDEO_FRAMERATE);
            String videoBitrate = intent.getStringExtra(DefaultConfig.VIDEO_BITRATE);
            String videoQuality = intent.getStringExtra(DefaultConfig.VIDEO_QUALITY);
            String pictureWidth = intent.getStringExtra(DefaultConfig.PICTURE_WIDTH);
            String pictureHeight = intent.getStringExtra(DefaultConfig.PICTURE_HEIGHT);
            String pictureQuality = intent.getStringExtra(DefaultConfig.PICTURE_QUALITY);
            String rtspPort = intent.getStringExtra(DefaultConfig.RTSP_PORT);
            SharedPreferences xSharedPreferences= mRecordService.getSharedPreferences("setting", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = xSharedPreferences.edit();
            if(videoWidth != null){
                editor.putInt(DefaultConfig.VIDEO_WIDTH, Integer.parseInt(videoWidth));
            }
            if(videoHeight != null){
                editor.putInt(DefaultConfig.VIDEO_HEIGHT, Integer.parseInt(videoHeight));
            }
            if(videoFramerate != null){
                editor.putInt(DefaultConfig.VIDEO_FRAMERATE, Integer.parseInt(videoFramerate));
            }
            if(videoBitrate != null){
                editor.putInt(DefaultConfig.VIDEO_BITRATE, Integer.parseInt(videoBitrate));
            }
            if(videoQuality != null){
                editor.putString(DefaultConfig.VIDEO_QUALITY, videoQuality);
            }
            if(pictureWidth != null){
                editor.putInt(DefaultConfig.PICTURE_WIDTH, Integer.parseInt(pictureWidth));
            }
            if(pictureHeight != null){
                editor.putInt(DefaultConfig.PICTURE_HEIGHT, Integer.parseInt(pictureHeight));
            }
            if(pictureQuality != null){
                editor.putString(DefaultConfig.PICTURE_QUALITY, pictureQuality);
            }
            if(rtspPort != null){
                editor.putInt(DefaultConfig.RTSP_PORT, Integer.parseInt(rtspPort));
            }
        }
    }

}
