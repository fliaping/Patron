package com.deepwits.Patron.Recorder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import com.deepwits.Patron.Config;

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
        intentFilter.addAction(Config.ACTION_BROADCAST);
        mActionBroadcastReceiver = new ActionBroadcastReceiver();
        mRecordService.registerReceiver(mActionBroadcastReceiver,intentFilter);
        Log.v(TAG, "registerActionBroadcast");
    }

    private void registerSettingBroadcast(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Config.SETTING_BROADCAST);
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
                    break;
                case "stop_record" :
                    //mRecordService.stopRecord();
                    break;
                case "capture_picture" :
                    //mRecordService.capturePicture();
                    break;
                case "voice_on" :
                    //mRecordService.voiceOn();
                    break;
                case "voice_off" :
                    //mRecordService.voiceOff();
                    break;
                case "loop_record_on" :
                    //mRecordService.loopRecordOn();
                    break;
                case "loop_record_off" :
                    //mRecordService.loopRecordOff();
                    break;
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
                    break;
                case "delete_file" :
                    Log.e("MediaFileDAOImpl", "receive delete broadcast");
                    int fileIds_1[] = intent.getIntArrayExtra("file_id");
                    mRecordService.deleteFiles(fileIds_1);
                    break;
                default:
                    Log.e(TAG,"未定义广播参数:"+action_name);

            }
        }

    }



    class SettingBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String videoWidth = intent.getStringExtra(Config.VIDEO_WIDTH);
            String videoHeight = intent.getStringExtra(Config.VIDEO_HEIGHT);
            String videoFramerate = intent.getStringExtra(Config.VIDEO_FRAMERATE);
            String videoBitrate = intent.getStringExtra(Config.VIDEO_BITRATE);
            String videoQuality = intent.getStringExtra(Config.VIDEO_QUALITY);
            String pictureWidth = intent.getStringExtra(Config.PICTURE_WIDTH);
            String pictureHeight = intent.getStringExtra(Config.PICTURE_HEIGHT);
            String pictureQuality = intent.getStringExtra(Config.PICTURE_QUALITY);
            String rtspPort = intent.getStringExtra(Config.RTSP_PORT);
            SharedPreferences xSharedPreferences= mRecordService.getSharedPreferences("setting", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = xSharedPreferences.edit();
            if(videoWidth != null){
                editor.putInt(Config.VIDEO_WIDTH, Integer.parseInt(videoWidth));
            }
            if(videoHeight != null){
                editor.putInt(Config.VIDEO_HEIGHT, Integer.parseInt(videoHeight));
            }
            if(videoFramerate != null){
                editor.putInt(Config.VIDEO_FRAMERATE, Integer.parseInt(videoFramerate));
            }
            if(videoBitrate != null){
                editor.putInt(Config.VIDEO_BITRATE, Integer.parseInt(videoBitrate));
            }
            if(videoQuality != null){
                editor.putString(Config.VIDEO_QUALITY, videoQuality);
            }
            if(pictureWidth != null){
                editor.putInt(Config.PICTURE_WIDTH, Integer.parseInt(pictureWidth));
            }
            if(pictureHeight != null){
                editor.putInt(Config.PICTURE_HEIGHT, Integer.parseInt(pictureHeight));
            }
            if(pictureQuality != null){
                editor.putString(Config.PICTURE_QUALITY, pictureQuality);
            }
            if(rtspPort != null){
                editor.putInt(Config.RTSP_PORT, Integer.parseInt(rtspPort));
            }
            editor.commit();
        }
    }

}
