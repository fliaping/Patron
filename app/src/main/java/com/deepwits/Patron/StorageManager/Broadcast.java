package com.deepwits.Patron.StorageManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Payne on 1/13/16.
 * 针对不支持TF热插拔的机器,自定义TF事件广播
 */
public class Broadcast extends TimerTask{
    public final static String ACTION_MEDIA_MOUNTED = "com.deepwits.Patron.StorageManager.MEDIA_MOUNTED";
    public final static String ACTION_MEDIA_UNMOUNTED = "com.deepwits.Patron.StorageManager.ACTION_MEDIA_UNMOUNTED";
    private Context context = null;
    //构造函数
    public Broadcast(Context context){
        this.context = context;
        Timer timer = new Timer();
        timer.schedule(this, 0, 1000);//每秒执行一次

        //转发系统广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        context.registerReceiver(new SDcaedReceiver(), intentFilter);// 注册监听函数
    }
    @Override
    public void run() {
        if(extSDexists()){
            mountedBroadcast();
        }else {
            unmountedBroadcast();
        }
    }

    public class SDcaedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ACTION_MEDIA_MOUNTED.equals(intent.getAction()) &&  extSDexists()){
                mountedBroadcast();
            }
            if(ACTION_MEDIA_UNMOUNTED.equals(intent.getAction()) && extSDexists()){
                unmountedBroadcast();
            }
        }
    }

    private void mountedBroadcast(){
        Intent i = new Intent(ACTION_MEDIA_MOUNTED);
        context.sendBroadcast(i);
    }

    private void unmountedBroadcast(){
        Intent i = new Intent(ACTION_MEDIA_UNMOUNTED);
        context.sendBroadcast(i);
    }

    private boolean extSDexists(){
        String extSD = StorageUtil.getExtSDCardPaths().get(0);
        if(extSD!=null && new File(extSD).exists()){
            return true;
        }else {
            return false;
        }
    }
}
