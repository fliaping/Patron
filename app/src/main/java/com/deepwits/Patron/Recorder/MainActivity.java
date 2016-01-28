package com.deepwits.Patron.Recorder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.deepwits.Patron.Config;
import com.deepwits.Patron.R;
import net.majorkernelpanic.streaming.gl2cameraeye.VideoPreview;

import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class MainActivity extends AppCompatActivity {

    private WindowManager mWindowManager;
    private View mRecordView;
    private ViewGroup mPreview;
    private VideoPreview mVideoCapture;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        this.startService(new Intent(this, RecordService.class));

        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editor = getSharedPreferences(Config.SETTING_SP_NAME, MODE_PRIVATE).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(9408));
        editor.commit();

        /*SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface);
        SessionBuilder.getInstance()
                .setSurfaceView(surfaceView)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264);*/
                //.setVideoQuality(new VideoQuality(1280,720,20,5000000));

        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("xuping_recorder");
                intent.putExtra("type","record");
                sendBroadcast(intent);
            }
        });
        findViewById(R.id.stream).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("xuping_recorder");
                intent.putExtra("type","stream");
                sendBroadcast(intent);
            }
        });
        findViewById(R.id.preview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("xuping_recorder");
                intent.putExtra("type","preview");
                sendBroadcast(intent);
            }
        });
        mPreview = (ViewGroup) findViewById(R.id.mPreview);
        //createContextAndStartCamera(0);


        //RecorderProvider recorderProvider = new RecorderProvider();
        //Log.e("MainActivity", recorderProvider.getSetting());
        //this.startService(new Intent(this,RtspServer.class));
    }

    public void open(){
        if(!mVideoCapture.isRecording()){
            try {
                mVideoCapture.setOutputFile(getStoreFileName());
                mVideoCapture.startRecord();
                mVideoCapture.startShortVideo();
            } catch (Throwable e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }else{
            mVideoCapture.stopRecord();
            mVideoCapture.stopShortVideo();
        }
    }



    private String getStoreFileName() {
        return Config.NORMAL_VIDEO_PATH + "NORMAL_" +System.currentTimeMillis() + ".mp4";
    }


    protected void createContextAndStartCamera(int cameraId) {
        Log.d(TAG, "createContextAndStartCamera: " + cameraId);
        mVideoCapture = VideoPreview.createVideoPreview(this, cameraId, 0);
        mVideoCapture.start(640, 480, 30);

        this.mPreview.addView(mVideoCapture.getSurfaceView());

        if (mVideoCapture != null) {
            mVideoCapture.startPreview();
        }

    }

    public static Boolean isShort = false;

}
