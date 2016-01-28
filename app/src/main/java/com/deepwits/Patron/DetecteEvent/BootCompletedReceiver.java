package com.deepwits.Patron.DetecteEvent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yysky.commons.utils.Utils;

//import android.util.Log;

/**
 * Created by xp on 15-10-24.
 * 系统启动完成广播接收器
 */
//开机自启动广播
public class BootCompletedReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        //延时5秒
        Utils.delayStartService(context,"com.sensor.detection.DetectService",5000);
    }
}
