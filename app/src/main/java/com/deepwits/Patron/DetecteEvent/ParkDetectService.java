package com.deepwits.Patron.DetecteEvent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ParkDetectService extends Service implements SensorEventListener {
	private SensorManager sm;
	public static int AUDIO_INPUT = MediaRecorder.AudioSource.CAMCORDER;
	public static int AUDIO_SAMPLE_RATE = 44100;
	private int bufferSizeInBytes = 0;
	private AudioRecord mAudioRec = null;
	private float voicedB;
	private final String TAG = "Detection";
	private boolean Flag_Voice = false;
	Context context;
	private static boolean flag = true;
	private int count = 0;
	public static SharedPreferences sharedPreferences_park;
	private Editor editor_park;
	private final String LEVEL = "level";

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		init();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getExtras() == null) {
			return super.onStartCommand(intent, flags, startId);
		}
		getSharePreference();
		setConfig(intent);
		return super.onStartCommand(intent, flags, startId);
	}

	public void setValue(int sensitivity) {
		switch (sensitivity) {
		case 1:
			shockvalue = 41;
			dbvalue = 100;
			break;
		case 2:
			shockvalue = 31;
			dbvalue = 80;
			break;
		case 3:
			shockvalue = 19;
			dbvalue = 60;
			break;
		}
	}

	@Override
	public void onDestroy() {
		close();
		Log.d("tlg", "ShockDetect is Destroy");
		super.onDestroy();
	}

	float linear_acceleration[] = new float[3];
	private float gravity[] = new float[3];
	final float alpha = 0.8f;
	float last_acceleration[] = new float[3];
	private long begintime;
	private boolean Flag = false;
	private int sum;
	private SharedPreferences spf;
	private int shockvalue;
	private static int dbvalue;
	private final String PARK_DETECT = "parking_monitor";

	/**
	 * 停车震动检测
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		Sensor sensor = event.sensor;
		Log.d("tlg", ParkDetectService.this.hashCode() + "");
		sum = 0;
		synchronized (this) {
			if (count < 11) {
				count++; // 11
			}
			if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && count > 10) {
				gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
				gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
				gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
				// 去除重力加速度后的值
				linear_acceleration[0] = event.values[0] - gravity[0];
				linear_acceleration[1] = event.values[1] - gravity[1];
				linear_acceleration[2] = event.values[2] - gravity[2];
				// Log.d("XX",
				// "原始"+event.values[0]+"  "+event.values[1]+"  "+event.values[2]);
				Log.d("tlg", "去重力" + linear_acceleration[0] + "  "
						+ linear_acceleration[1] + "  "
						+ linear_acceleration[2]);
				for (int i = 0; i < gravity.length; i++) {
					sum += Math.pow(2, linear_acceleration[i]);
				}
				sum = (int) (Math.sqrt(sum) * 100) / 100;
				Log.d("tlg", "sum: " + sum);
				// 获取当前系统的时间
				begintime = System.currentTimeMillis();

				if (event.timestamp - begintime > 500) {
				} else {
				}

				if (sum > shockvalue) {
					Log.d("tlg",
							sum
									+ " DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD chufa");
					// 设置标识位
					Flag = true;
				}
			}
		}
	}

	Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			com.yysky.commons.Log.d("tlg", "voice detect begin");
			while (flag) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}

				voicedB = record();
				if (voicedB >= 140.0F)
					voicedB = 140.0F;
				voicedB = ((int) (100.0F * voicedB) / 100.0F);
				if (voicedB > dbvalue) {
					Flag_Voice = true;
					Log.d(TAG, " :" + voicedB);
				}
			}
		}
	};
	Thread mRetriveVoiceAmplThread = null;

	public void initVoice() {
		// TODO Auto-generated method stub
		context = getApplicationContext();
		bufferSizeInBytes = 4096;
		// Intent intent = new Intent(context, ShockDetect.class);
		// context.startService(intent);
		if (mAudioRec == null) {
			mAudioRec = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE,
					AudioFormat.CHANNEL_IN_STEREO,
					AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
		}
		mAudioRec.startRecording();
		if (mRetriveVoiceAmplThread == null) {
			mRetriveVoiceAmplThread = new Thread(this.mRunnable);
			mRetriveVoiceAmplThread.start();
		}
		detect();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	// 获取声音分贝
	private float record() {
		if (mAudioRec == null) {
			return 1;
		}
		short[] arrayOfShort = new short[bufferSizeInBytes];
		int i = 0;
		try {
			i = mAudioRec.read(arrayOfShort, 0, bufferSizeInBytes);
		} catch (Exception e) {
			Log.d("ShockDetect", e.getMessage(), e);
		}
		int j = 0;
		float f = 0.0F;
		do {
			f += Math.abs(arrayOfShort[j]);
			j++;
		} while (j < i / 2);
		return 10.0F * (float) Math.log(f / (i / 2));
	}

	// 启动定时任务
	public void detect() {
		Timer detectTimer = new Timer("detect");
		com.yysky.commons.Log.d("tlg", "dingshirenwu");
		TimerTask detect_ttTask = new TimerTask() {
			boolean voice;
			boolean sensor;

			// ShockDetect sdDetect;

			@Override
			public void run() {
				// TODO Auto-generated method stub
				voice = Flag_Voice;
				sensor = Flag;
				// com.yysky.commons.Log.d("tlg", "voice " + voice + " sensor "
				// + sensor);
				if (voice && sensor) {
					sendBroadcast();
				}
				Flag_Voice = false;
				Flag = false;
			}
		};
		detectTimer.scheduleAtFixedRate(detect_ttTask, 100L, 500L);
	}

	public void sendBroadcast() {
		Log.d(TAG, " ==>>>>>>ShockDetect");
		Intent intent = new Intent();
		spf = getApplicationContext().getSharedPreferences(PARK_DETECT,
				Context.MODE_PRIVATE);
		int recordtime = spf.getInt("recordtime", 30);
		intent.setAction("com.deepwits.pine.ShockDetect");
		intent.putExtra("type", "stopEvent");
		intent.putExtra("sessionID", "stopEvent");
		intent.putExtra("duration", recordtime);
		// Toast.makeText(this,"开始拍照",Toast.LENGTH_SHORT).show();
		sendBroadcast(intent);
	}

	public void close() {
		if (mAudioRec != null) {
			flag = false;
			this.mRetriveVoiceAmplThread = null;
			Log.d("tlg", "stop record");
			mAudioRec.stop();
			mAudioRec.release();// 释放资源
			mAudioRec = null;
		}
		if (sm != null) {
			sm = (SensorManager) getSystemService(SENSOR_SERVICE);
			sm.unregisterListener(this);
		}
	}

	private void getSharePreference() {
		if (sharedPreferences_park == null) {
			sharedPreferences_park = getApplicationContext()
					.getSharedPreferences(PARK_DETECT,
							getApplicationContext().MODE_PRIVATE);
		}
		editor_park = sharedPreferences_park.edit();
	}

	public void init() {
		if (sm == null) {
			sm = (SensorManager) getSystemService(SENSOR_SERVICE);
			sm.registerListener(this,
					sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
	//将改变的数据保存 
	private void setConfig(Intent intent) {
		Bundle bundle = intent.getExtras();
		Boolean park_status = bundle.getBoolean("parking_status");
		int level = bundle.getInt("level", 0);
		//判断开关是否改变
		if (park_status == null) {
			park_status = sharedPreferences_park.getBoolean("parking_status",
					false);
		}
		//判断是否改变了安保等级		
		if (level == 0) {
			level = sharedPreferences_park.getInt(LEVEL, 3);
		} else {
			editor_park.putInt(LEVEL, level);
			editor_park.commit();
		}
		//如果是开启状态，且没有启动程序 则开启
		if (park_status && !flag) {
			setValue(level);
			count = 0;
			flag = true;
			initVoice();
		}else {//反之 如果开启则关闭			
			if (flag) {
				close();
			}
		}	
	}
}
