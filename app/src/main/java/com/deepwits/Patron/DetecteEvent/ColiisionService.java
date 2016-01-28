package com.deepwits.Patron.DetecteEvent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.yysky.commons.utils.ShareUtils;

public class ColiisionService extends Service implements SensorEventListener {
    private SensorManager sm;
    private final String STOPCAR_DETECT = "com.sensor.detection.stopDetect"; //碰撞触发
    private final String SETTING_TYPE = "SensorSetting";
    private int sensitivity;
    private static float sensor_value = Integer.MAX_VALUE;
    private Context mContext;
    private static SharedPreferences sharedPreferences_collision;
    private final String COLLISION_DETECT = "collision_detect";
    private final String LEVEL = "level";
    private Editor editor_collision;
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        if (sharedPreferences_collision == null) {        	
			sharedPreferences_collision = getApplicationContext().getSharedPreferences(
					COLLISION_DETECT, getApplicationContext().MODE_PRIVATE);
		}
		editor_collision = sharedPreferences_collision.edit(); 

    }

    /**
     * 每次收到广播 传值启动service时调用此函数
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        if (intent == null || intent.getExtras() == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        Bundle bundle = intent.getExtras();
        int level  = bundle.getInt("level",3);        
        if (sharedPreferences_collision == null) {        	
			sharedPreferences_collision = getApplicationContext().getSharedPreferences(
					COLLISION_DETECT, getApplicationContext().MODE_PRIVATE);
		}
		editor_collision = sharedPreferences_collision.edit();
		editor_collision.putInt(LEVEL, level);
		editor_collision.commit();
		setValue(level);
        return super.onStartCommand(intent, flags, startId);
    }

    private void init() {
        this.mContext = this.getApplicationContext();
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        if (sm != null) {
            sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            sm.unregisterListener(this);
        }
        // TODO Auto-generated method stub
        super.onDestroy();
    }


    /**
     * 传感器灵敏度改变调用
     *
     * @param value
     */


    float linear_acceleration[] = new float[3];
    private float gravity[] = new float[3];
    final float alpha = 0.8f;
    private long begintime;
    float last_acceleration[] = new float[3];
    boolean mRecordingStatus = false;
    boolean FLAG = false;
    private float acc_sum;
    private int count = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        //  Log.e("tlg", "onSensorChanged");
        // TODO Auto-generated method stub
        Sensor sensor = event.sensor;
        synchronized (this) {
            if (count < 11) {
                count++; //11
            }
          //Log.d("tlg","Sensor count:"+count);
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && count > 10) {  //9
                begintime = System.currentTimeMillis();
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
                // 去除重力加速度后的值
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];
                //传感器Z方向加速度值为0 只取XY方向的值处理
                //  Log.e("tlg", linear_acceleration[0] + " " + linear_acceleration[1] + "  " + linear_acceleration[2]);
                for (int i = 0; i < 3; i++) {
                    if (last_acceleration[i] - linear_acceleration[i] > sensor_value) {
                        FLAG = true;
                        break;
                    }
                }
                // 发送广播震动事件广播 60
                if (FLAG) {
                    sengBroadcast(getApplicationContext(), STOPCAR_DETECT);
                    Log.e("tlg", "震动事件触发+++++++++++++++++++++++++++++++++++++++++++++++++++++" + acc_sum);
                    FLAG = false;
                    //  Toast.makeText(this.getApplicationContext(), "震动事件触发", Toast.LENGTH_SHORT).show();
                }
                for (int i = 0; i < 3; i++) {
                    last_acceleration[i] = linear_acceleration[i];
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    /**
     * 广播发送
     *
     * @param context
     */

    public void sengBroadcast(Context context, String Action) {

        Intent intent = new Intent();
        intent.setAction(Action);
        intent.putExtra("type", "detection");
        intent.putExtra("value", "success");
        context.sendBroadcast(intent);
    }
    
    public void setValue(int sensitivity) {
        switch (sensitivity) {
            case 1:
                Intent mintent = new Intent(getApplicationContext(), ColiisionService.class);
                getApplicationContext().stopService(mintent);
                break;
            case 2:
                sensor_value = 19.62001F;
                break;
            case 3:
                sensor_value = 31.39201F;
                break;
            case 4:
                sensor_value = 47.08801F;
                break;
            case 5:
                sensor_value = 55.08801F;
                break;
        }
    }

}
