package com.deepwits.Patron.DetecteEvent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager sm;
	boolean mRecordingStatus = false;
	public static final String SENSITIVITY_VALUE = "sensitivity_value";// 灵敏值
	public static SharedPreferences sharedPreferences;
	public static final String SETP_SHARED_PREFERENCES = "setp_shared_preferences";// 设置
	private Boolean FLAG = true;

	public void init() {
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		sm.registerListener(this,
				sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_main);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			return true;
//		}
		return super.onOptionsItemSelected(item);
	}

	float linear_acceleration[] = new float[3];
	private float gravity[] = new float[3];
	final float alpha = 0.8f;
	private long begintime;
	float last_acceleration[] = new float[3];

	

	/**
	 * 车辆碰撞检测伪例；
	 */
	private float mScale[] = new float[2];
	private float mLastExtremes[] = new float[2];
	private long begin, end;
	Boolean Flag = false;

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stu
		Sensor sensor = event.sensor;
		synchronized (this) {
			if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				float vSum = 0;

				for (int i = 0; i < 3; i++) {
					// 补偿后的各方向值
					gravity[i] = alpha * gravity[i] + (1 - alpha)* event.values[i];
					
					linear_acceleration[i] = event.values[i] - gravity[i];

				}
			
				
				if (linear_acceleration[2]>-30) {
				
					Flag = true;
					begin = event.timestamp;

				} else if (Flag) {

					end = System.currentTimeMillis();
					if (Math.abs(vSum) < 20) {
						Flag = false;

					}
				}
			}

		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public void sengbroadcast(Context context) {
		Intent intent = new Intent();
		intent.setAction("com.sensor.detection.stopDetect");
		intent.putExtra("type", "detection");
		intent.putExtra("value", "success");
		context.sendBroadcast(intent);
	}
}
