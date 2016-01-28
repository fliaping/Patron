package net.majorkernelpanic.streaming.video;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import net.majorkernelpanic.streaming.exceptions.ConfNotSupportedException;
import net.majorkernelpanic.streaming.exceptions.StorageUnavailableException;
import net.majorkernelpanic.streaming.hw.EncoderDebugger;
import net.majorkernelpanic.streaming.mp4.MP4Config;
import net.majorkernelpanic.streaming.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Payne on 1/27/16.
 */
public class PatronStream extends VideoStream {


    private Semaphore mLock = new Semaphore(0);
    private MP4Config mConfig;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public PatronStream() {
        mMimeType = "video/avc";
        mVideoEncoder = MediaRecorder.VideoEncoder.H264;
        mPacketizer = new H264Packetizer();

    }

    /**
     * Returns a description of the stream using SDP. It can then be included in an SDP file.
     */
    public synchronized String getSessionDescription() throws IllegalStateException {
        if (mConfig == null) throw new IllegalStateException("You need to call configure() first !");
        return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
                "a=rtpmap:96 H264/90000\r\n" +
                "a=fmtp:96 packetization-mode=1;profile-level-id="+mConfig.getProfileLevel()+";sprop-parameter-sets="+mConfig.getB64SPS()+","+mConfig.getB64PPS()+";\r\n";
    }

    /**
     * Starts the stream.
     * This will also open the camera and display the preview if {@link #startPreview()} has not already been called.
     */
    public synchronized void start() throws IllegalStateException, IOException {
        if (!mStreaming) {
            configure();
            byte[] pps = Base64.decode(mConfig.getB64PPS(), Base64.NO_WRAP);
            byte[] sps = Base64.decode(mConfig.getB64SPS(), Base64.NO_WRAP);
            ((H264Packetizer)mPacketizer).setStreamParameters(pps, sps);
            super.start();
        }
    }

    /**
     * Configures the stream. You need to call this before calling {@link #getSessionDescription()} to apply
     * your configuration of the stream.
     */
    public synchronized void configure() throws IllegalStateException, IOException {
        super.configure();
        mMode = mRequestedMode;
        mQuality = mRequestedQuality.clone();
        Log.e(TAG,"---------  PatronStream configure");
        mConfig = testH264();

        Log.e(TAG,"---------  PatronStream configure pps "+mConfig.getB64PPS());
    }
    public synchronized void configure(byte mode) throws IllegalStateException, IOException {
        super.configure();
        mMode = mode;
        mQuality = mRequestedQuality.clone();
        Log.e(TAG,"---------  PatronStream configure");
        mConfig = testH264();

        Log.e(TAG,"---------  PatronStream configure pps "+mConfig.getB64PPS());
    }

    /**
     * Tests if streaming with the given configuration (bit rate, frame rate, resolution) is possible
     * and determines the pps and sps. Should not be called by the UI thread.
     **/
    private MP4Config testH264() throws IllegalStateException, IOException {
        if (mMode == MODE_MEDIARECORDER_API) return testMediaRecorderAPI();
        else if(mMode == MODE_PATRON_RECORD ) return testPatronAPI();
        else return testMediaCodecAPI();
    }

    private MP4Config testPatronAPI() throws RuntimeException, IOException {
        return new MP4Config("J0LgHo1oCgPabIAAAAMAgAAACgeKEVA=","KM4ESSA=");
    }
    @SuppressLint("NewApi")
    private MP4Config testMediaCodecAPI() throws RuntimeException, IOException {
        createCamera();
        updateCamera();
        try {
            if (mQuality.resX>=640) {
                // Using the MediaCodec API with the buffer method for high resolutions is too slow
                //mMode = MODE_MEDIARECORDER_API;
            }
            EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.resX, mQuality.resY);
            //add
            String SPS = debugger.getB64SPS();
            String PPS = debugger.getB64PPS();
            Log.e(TAG,"----------- SPS:"+SPS+"  PPS:"+PPS);
            return new MP4Config(debugger.getB64SPS(), debugger.getB64PPS());
        } catch (Exception e) {
            // Fallback on the old streaming method using the MediaRecorder API
            Log.e(TAG, "Resolution not supported with the MediaCodec API, we fallback on the old streamign method.");
            mMode = MODE_MEDIARECORDER_API;
            return testH264();
        }
    }

    // Should not be called by the UI thread
    private MP4Config testMediaRecorderAPI() throws RuntimeException, IOException {
        String key = PREF_PREFIX+"h264-mr-"+mRequestedQuality.framerate+","+mRequestedQuality.resX+","+mRequestedQuality.resY;

        if (mSettings != null) {
            if (mSettings.contains(key)) {
                String[] s = mSettings.getString(key, "").split(",");
                return new MP4Config(s[0],s[1],s[2]);
            }
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new StorageUnavailableException("No external storage or external storage not ready !");
        }

        final String TESTFILE = Environment.getExternalStorageDirectory().getPath()+"/spydroid-test.mp4";

        Log.i(TAG,"Testing H264 support... Test file saved at: "+TESTFILE);

        try {
            File file = new File(TESTFILE);
            file.createNewFile();
        } catch (IOException e) {
            throw new StorageUnavailableException(e.getMessage());
        }

        // Save flash state & set it to false so that led remains off while testing h264
        boolean savedFlashState = mFlashEnabled;
        mFlashEnabled = false;

        boolean previewStarted = mPreviewStarted;

        boolean cameraOpen = mCamera!=null;
        createCamera();

        // Stops the preview if needed
        if (mPreviewStarted) {
            lockCamera();
            try {
                mCamera.stopPreview();
            } catch (Exception e) {}
            mPreviewStarted = false;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        unlockCamera();

        try {

            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setVideoEncoder(mVideoEncoder);
            mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
            mMediaRecorder.setVideoSize(mRequestedQuality.resX,mRequestedQuality.resY);
            mMediaRecorder.setVideoFrameRate(mRequestedQuality.framerate);
            mMediaRecorder.setVideoEncodingBitRate((int)(mRequestedQuality.bitrate*0.8));
            mMediaRecorder.setOutputFile(TESTFILE);
            mMediaRecorder.setMaxDuration(3000);

            // We wait a little and stop recording
            mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    Log.d(TAG,"MediaRecorder callback called !");
                    if (what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d(TAG,"MediaRecorder: MAX_DURATION_REACHED");
                    } else if (what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                        Log.d(TAG,"MediaRecorder: MAX_FILESIZE_REACHED");
                    } else if (what==MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN) {
                        Log.d(TAG,"MediaRecorder: INFO_UNKNOWN");
                    } else {
                        Log.d(TAG,"WTF ?");
                    }
                    mLock.release();
                }
            });

            // Start recording
            mMediaRecorder.prepare();
            mMediaRecorder.start();

            if (mLock.tryAcquire(6, TimeUnit.SECONDS)) {
                Log.d(TAG,"MediaRecorder callback was called :)");
                Thread.sleep(400);
            } else {
                Log.d(TAG,"MediaRecorder callback was not called after 6 seconds... :(");
            }
        } catch (IOException e) {
            throw new ConfNotSupportedException(e.getMessage());
        } catch (RuntimeException e) {
            throw new ConfNotSupportedException(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {}
            mMediaRecorder.release();
            mMediaRecorder = null;
            lockCamera();
            if (!cameraOpen) destroyCamera();
            // Restore flash state
            mFlashEnabled = savedFlashState;
            if (previewStarted) {
                // If the preview was started before the test, we try to restart it.
                try {
                    startPreview();
                } catch (Exception e) {}
            }
        }

        // Retrieve SPS & PPS & ProfileId with MP4Config
        MP4Config config = new MP4Config(TESTFILE);

        // Delete dummy video
        File file = new File(TESTFILE);
        if (!file.delete()) Log.e(TAG,"Temp file could not be erased");

        Log.i(TAG,"H264 Test succeded...");

        // Save test result
        if (mSettings != null) {
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString(key, config.getProfileLevel()+","+config.getB64SPS()+","+config.getB64PPS());
            editor.commit();
        }

        return config;

    }


    private long oldnow = 0;
    private long now  = 0;
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void writeVideoSampleData(byte[] data) {
        Log.e(TAG,"PatronStream get a frame data:"+data.length);
        now = System.nanoTime() / 1000;
        oldnow = now;

        int bufferIndex = mMediaCodec.dequeueInputBuffer(500000);
        if (bufferIndex >= 0) {
            patronInputBuffers[bufferIndex].clear();
            ByteBuffer buffer = patronInputBuffers[bufferIndex];
            if (data == null)
                Log.e(TAG, "Symptom of the \"Callback buffer was to small\" problem...");
            else {
                int min = buffer.capacity() < data.length?buffer.capacity() : data.length;
                buffer.put(data, 0, min);
            }
            mMediaCodec.queueInputBuffer(bufferIndex, 0, patronInputBuffers[bufferIndex].position(), now, 0);
        } else {
            Log.e(TAG, "No buffer available !");
        }

    }

}
