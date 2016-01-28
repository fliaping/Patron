package com.deepwits.Patron.DetecteEvent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
/*
 * create by tlg 20160121
 * 
 */
public class DetectReceiver extends BroadcastReceiver {
    private SharedPreferences sharedpreferences;
    private final String START_CAR_ACTION = "com.deepwits.patron.start_car";
    private final String STOP_CAR_ACTION = "com.deepwits.patron.stop_car";
    private final String SETTING_CHANGE = "com.deepwits.patron.settingchange";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        if (START_CAR_ACTION.equals(intent.getAction())) {
        		//汽车启动，开启碰撞检测，停止停车监控
        		Intent startColiision = new Intent(context,ColiisionService.class);
        		context.startService(startColiision);
        		Intent stopParkDetect = new Intent(context,ParkDetectService.class);
        		context.stopService(stopParkDetect);

        } else if (STOP_CAR_ACTION.equals(intent.getAction())) {
        	//汽车停止，关闭碰撞检测，开启停车监控
        	Intent stopColiision = new Intent(context,ColiisionService.class);
    		context.stopService(stopColiision);
    		Intent startParkDetect = new Intent(context,ParkDetectService.class);
    		context.startService(startParkDetect);
        	
        } else if (SETTING_CHANGE.equals(intent.getAction())) {
        	String type = intent.getStringExtra("type");
        	if(type == null||"".equals(type)){
        		return ;
        	}else if("park".equals(type)){
        		Bundle bundle = intent.getBundleExtra("bundle");            	
            	Intent mIntent = new Intent(context, ParkDetectService.class);
            	mIntent.putExtras(bundle);
                context.startService(mIntent);
        	}else if ("collision".equals(type)) {
        		Bundle bundle = intent.getBundleExtra("bundle");            	
            	Intent mIntent = new Intent(context, ParkDetectService.class);
            	mIntent.putExtras(bundle);
                context.stopService(mIntent);
			}
        	
        }

    }

}