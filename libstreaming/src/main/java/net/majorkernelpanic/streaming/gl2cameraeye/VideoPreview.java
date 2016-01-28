package net.majorkernelpanic.streaming.gl2cameraeye;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

//import org.chromium.base.CalledByNative;  (GL2CameraEye)
//import org.chromium.base.JNINamespace;


import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.Setting;
import net.majorkernelpanic.streaming.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;
import net.majorkernelpanic.streaming.video.PatronStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

//@JNINamespace("media")
public class VideoPreview implements PreviewCallback, VideoGLRender.OnFrameListener {
    private H264Packetizer mPacketizer;
    private MediaCodec mRtspMediaCodec;
    private boolean isEncoder;
    private AsyncQueue<FrameData> asyncQueue = new AsyncQueue<>();
    private MediaMuxerWrapper muxer;
    private boolean stream_on = true;
    private boolean shortVideo_on = false;
    private SharedPreferences mSharedPreferences = null;
    private boolean capture_picture = false;
    private int videoType;
    private boolean audio_record;


    //getSharedPreferences(Config.SETTING_SP_NAME, context.MODE_PRIVATE);

    static class CaptureFormat {
        public CaptureFormat(
                int width, int height, int framerate, int pixelformat) {
            mWidth = width;
            mHeight = height;
            mFramerate = framerate;
            mPixelFormat = pixelformat;
        }
        public int mWidth;
        public int mHeight;
        public final int mFramerate;
        public final int mPixelFormat;
        //@CalledByNative("CaptureFormat")
        public int getWidth() {
            return mWidth;
        }
        //@CalledByNative("CaptureFormat")
        public int getHeight() {
            return mHeight;
        }
        //@CalledByNative("CaptureFormat")
        public int getFramerate() {
            return mFramerate;
        }
        //@CalledByNative("CaptureFormat")
        public int getPixelFormat() {
            return mPixelFormat;
        }
    }

    // Some devices don't support YV12 format correctly, even with JELLY_BEAN or
    // newer OS. To work around the issues on those devices, we have to request
    // NV21. Some other devices have troubles with certain capture resolutions
    // under a given one: for those, the resolution is swapped with a known
    // good. Both are supposed to be temporary hacks.
    private static class BuggyDeviceHack {
        private static class IdAndSizes {
            IdAndSizes(String model, String device, int minWidth, int minHeight) {
                mModel = model;
                mDevice = device;
                mMinWidth = minWidth;
                mMinHeight = minHeight;
            }
            public final String mModel;
            public final String mDevice;
            public final int mMinWidth;
            public final int mMinHeight;
        }
        private static final IdAndSizes s_CAPTURESIZE_BUGGY_DEVICE_LIST[] = {
            new IdAndSizes("Nexus 7", "flo", 640, 480)
        };

        private static final String[] s_COLORSPACE_BUGGY_DEVICE_LIST = {
            "SAMSUNG-SGH-I747",
            "ODROID-U2",
        };

        static void applyMinDimensions(CaptureFormat format) {
            // NOTE: this can discard requested aspect ratio considerations.
            for (IdAndSizes buggyDevice : s_CAPTURESIZE_BUGGY_DEVICE_LIST) {
                if (buggyDevice.mModel.contentEquals(android.os.Build.MODEL) &&
                        buggyDevice.mDevice.contentEquals(android.os.Build.DEVICE)) {
                    format.mWidth = (buggyDevice.mMinWidth > format.mWidth)
                                        ? buggyDevice.mMinWidth
                                        : format.mWidth;
                    format.mHeight = (buggyDevice.mMinHeight > format.mHeight)
                                         ? buggyDevice.mMinHeight
                                         : format.mHeight;
                }
            }
        }

        static int getImageFormat() {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                return ImageFormat.NV21;
            }

            for (String buggyDevice : s_COLORSPACE_BUGGY_DEVICE_LIST) {
                if (buggyDevice.contentEquals(android.os.Build.MODEL)) {
                    return ImageFormat.NV21;
                }
            }
            return ImageFormat.YV12;
        }
    }

    private Camera mCamera;
    public ReentrantLock mPreviewBufferLock = new ReentrantLock();
    private Context mContext = null;
    // True when native code has started capture.
    private boolean mIsRunning = false;

    private static final int NUM_CAPTURE_BUFFERS = 3;
    private int mExpectedFrameSize = 0;
    private int mId = 0;
    // Native callback context variable.
    private long mNativeVideoCaptureDeviceAndroid = 0;

    private int mCameraOrientation = 0;
    private int mCameraFacing = 0;
    private int mDeviceOrientation = 0;

    CaptureFormat mCaptureFormat = null;
    private VideoGLSurfaceView mGlSurfaceView = null;
    private Looper mLooper = null;
    private static final String TAG = "MediaMuxerWrapper";

    public static VideoPreview createVideoPreview(
            Context context, int id, long nativeVideoCaptureDeviceAndroid) {
        return new VideoPreview(context, id, nativeVideoCaptureDeviceAndroid);
    }

    //@CalledByNative
    public static CaptureFormat[] getDeviceSupportedFormats(int id) {
        Camera camera;
        try {
             camera = Camera.open(id);
        } catch (RuntimeException ex) {
            Log.e(TAG, "Camera.open: " + ex);
            return null;
        }
        Camera.Parameters parameters = camera.getParameters();

        ArrayList<CaptureFormat> formatList = new ArrayList<CaptureFormat>();
        // getSupportedPreview{Formats,FpsRange,PreviewSizes}() returns Lists
        // with at least one element, but when the camera is in bad state, they
        // can return null pointers; in that case we use a 0 entry, so we can
        // retrieve as much information as possible.
        List<Integer> pixelFormats = parameters.getSupportedPreviewFormats();
        if (pixelFormats == null) {
            pixelFormats = new ArrayList<Integer>();
        }
        if (pixelFormats.size() == 0) {
            pixelFormats.add(ImageFormat.UNKNOWN);
        }
        for (Integer previewFormat : pixelFormats) {
            int pixelFormat = 0;

            List<int[]> listFpsRange = parameters.getSupportedPreviewFpsRange();
            if (listFpsRange == null) {
                listFpsRange = new ArrayList<int[]>();
            }
            if (listFpsRange.size() == 0) {
                listFpsRange.add(new int[] {0, 0});
            }
            for (int[] fpsRange : listFpsRange) {
                List<Camera.Size> supportedSizes =
                        parameters.getSupportedPreviewSizes();
                if (supportedSizes == null) {
                    supportedSizes = new ArrayList<Camera.Size>();
                }
                if (supportedSizes.size() == 0) {
                    supportedSizes.add(camera.new Size(0, 0));
                }
                for (Camera.Size size : supportedSizes) {
                    formatList.add(new CaptureFormat(size.width, size.height,
                            (fpsRange[0] + 999 ) / 1000, pixelFormat));
                }
            }
        }
        camera.release();
        return formatList.toArray(new CaptureFormat[formatList.size()]);
    }

    //////////////////////////////////////////////////////////////
    private boolean isPreview = false;
    private boolean isStreaming =false;
    public boolean isPreview(){
        return this.isPreview;
    }
    public boolean isStreaming(){
        return this.isStreaming;
    }

    public VideoPreview(
            Context context, int id, long nativeVideoCaptureDeviceAndroid) {
        mContext = context;
        mId = id;
        mNativeVideoCaptureDeviceAndroid = nativeVideoCaptureDeviceAndroid;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void startStream(){
        Log.e("RTSP","startStream");
        try {
            mRtspMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int width = mSharedPreferences.getInt(Setting.RTSP_VID_WIDTH,320);
        int height = mSharedPreferences.getInt(Setting.RTSP_VID_WIDTH,240);
        int framerate = mSharedPreferences.getInt(Setting.RTSP_VID_WIDTH,30);

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc",width,height );
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 500000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mRtspMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mRtspMediaCodec.start();

        // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
        mPacketizer = new H264Packetizer();
        mPacketizer.setInputStream(new MediaCodecInputStream(mRtspMediaCodec));
        mPacketizer.start();
        isStreaming = true;
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void stopStream(){
        mPacketizer.stop();
        mRtspMediaCodec.stop();
        mRtspMediaCodec.release();
        mRtspMediaCodec = null;
        isStreaming = true;

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopShortVideo() {
        Log.i(TAG, "stopShortVideo " + isEncoder);
        isEncoder = false; //不往队列送数据(不在生产数据)
        asyncQueue.stop();
        Log.i(TAG, "stopShortVideoed!!! " + isEncoder);
    }
    private int conut_chu = 0;
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public String startShortVideo() throws Exception {

        isEncoder = true;

        muxer = MediaMuxerWrapper.create("/sdcard/src/"+System.currentTimeMillis()+".mp4");//创建MediaMuxer
        muxer.addVideoTrack(320,240,20, MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888);
        muxer.start();

        asyncQueue.setHandler(new AsyncQueue.Handler<FrameData>() {
            @Override
            public void onStart() {

            }

            @Override
            public void onData(FrameData frameData) {
                Log.e(TAG,"FrameData onData length:"+frameData.data.length);
                muxer.writeVideoSample(frameData.data);
                conut_chu++;
            }

            @Override
            public void onFinish() {
                muxer.stop();
                //stopVideoAndRelease();
            }
        });
        asyncQueue.start();
        Log.e(TAG, "Encoder startShortVideo success !! " + "isEncoder" + isEncoder + "Output Path :");
        return "ok";

    }

    public void capturePicture() {
        capture_picture = true;
    }

    public void lockCurrentVideo(boolean lock){
        if(lock) videoType = 2;
        else videoType = 1;
    }
    public void setAudio_record(boolean on){
        if (on) audio_record = true;
        else audio_record = false;
    }

    // Returns true on success, false otherwise.
    //@CalledByNative
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public boolean start(int width, int height, int frameRate) {
        Log.d(TAG, "allocate: requested (" + width + "x" + height + ")@" +
                 frameRate + "fps");
        try {
            mCamera = Camera.open(mId);
        } catch (RuntimeException ex) {
            Log.e(TAG, "allocate: Camera.open: " + ex);
            return false;
        }

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mId, cameraInfo);
        mCameraOrientation = cameraInfo.orientation;
        mCameraFacing = cameraInfo.facing;
        mDeviceOrientation = getDeviceOrientation();
        Log.d(TAG, "allocate: orientation dev=" + mDeviceOrientation + ", cam=" + mCameraOrientation + ", facing=" + mCameraFacing);
        mCamera.setDisplayOrientation(270);
        Camera.Parameters parameters = mCamera.getParameters();

        // getSupportedPreviewFpsRange() returns a List with at least one
        // element, but when camera is in bad state, it can return null pointer.
        List<int[]> listFpsRange = parameters.getSupportedPreviewFpsRange();
        if (listFpsRange == null || listFpsRange.size() == 0) {
            Log.e(TAG, "allocate: no fps range found");
            return false;
        }
        int frameRateInMs = frameRate * 1000;
        // Use the first range as default.
        int[] fpsMinMax = listFpsRange.get(0);
        int newFrameRate = (fpsMinMax[0] + 999) / 1000;
        for (int[] fpsRange : listFpsRange) {
            if (fpsRange[0] <= frameRateInMs && frameRateInMs <= fpsRange[1]) {
                fpsMinMax = fpsRange;
                newFrameRate = frameRate;
                break;
            }
        }
        frameRate = newFrameRate;
        Log.d(TAG, "allocate: fps set to " + frameRate);

        // Calculate size.
        List<Camera.Size> listCameraSize =
                parameters.getSupportedPreviewSizes();
        int minDiff = Integer.MAX_VALUE;
        int matchedWidth = width;
        int matchedHeight = height;
        for (Camera.Size size : listCameraSize) {
            int diff = Math.abs(size.width - width) +
                       Math.abs(size.height - height);
            Log.d(TAG, "allocate: supported (" +
                  size.width + ", " + size.height + "), diff=" + diff);
            // TODO(wjia): Remove this hack (forcing width to be multiple
            // of 32) by supporting stride in video frame buffer.
            // Right now, VideoCaptureController requires compact YV12
            // (i.e., with no padding).
            if (diff < minDiff && (size.width % 32 == 0)) {
                minDiff = diff;
                matchedWidth = size.width;
                matchedHeight = size.height;
            }
        }
        if (minDiff == Integer.MAX_VALUE) {
            Log.e(TAG, "allocate: can not find a multiple-of-32 resolution");
            return false;
        }

        mCaptureFormat = new CaptureFormat(
                matchedWidth, matchedHeight, frameRate,
                BuggyDeviceHack.getImageFormat());
        // Hack to avoid certain capture resolutions under a minimum one,
        // see http://crbug.com/305294
        BuggyDeviceHack.applyMinDimensions(mCaptureFormat);
        Log.d(TAG, "allocate: matched (" + mCaptureFormat.mWidth + "x" +
                mCaptureFormat.mHeight + ")");

        if (parameters.isVideoStabilizationSupported()) {
            Log.d(TAG, "Image stabilization supported, currently: "
                  + parameters.getVideoStabilization() + ", setting it.");
            parameters.setVideoStabilization(true);
        } else {
            Log.d(TAG, "Image stabilization not supported.");
        }

        mGlSurfaceView = new VideoGLSurfaceView(mContext,
                this,
                mCamera,
                mCaptureFormat.mWidth,
                mCaptureFormat.mHeight);
        mLooper = Looper.myLooper();

        parameters.setPreviewSize(mCaptureFormat.mWidth,
                mCaptureFormat.mHeight);
        parameters.setPreviewFormat(mCaptureFormat.mPixelFormat);
        parameters.setPreviewFpsRange(fpsMinMax[0], fpsMinMax[1]);
        mCamera.setParameters(parameters);

        int bufSize = mCaptureFormat.mWidth *
                mCaptureFormat.mHeight *
                ImageFormat.getBitsPerPixel(
                        mCaptureFormat.mPixelFormat) / 8;

        for (int i = 0; i < NUM_CAPTURE_BUFFERS; i++) {
            byte[] buffer = new byte[bufSize];
            mCamera.addCallbackBuffer(buffer);
        }
        mExpectedFrameSize = bufSize;

        return true;
    }

    //@CalledByNative
    public int queryWidth() {
        return mCaptureFormat.mWidth;
    }

    //@CalledByNative
    public int queryHeight() {
        return mCaptureFormat.mHeight;
    }

    //@CalledByNative
    public int queryFrameRate() {
        return mCaptureFormat.mFramerate;
    }

    //@CalledByNative
    public int getColorspace() {
        return ImageFormat.YV12;
/*      switch (mCaptureFormat.mPixelFormat) {                    (GL2CameraEye)
            case ImageFormat.YV12:
                return AndroidImageFormatList.ANDROID_IMAGEFORMAT_YV12;
            case ImageFormat.NV21:
                return AndroidImageFormatList.ANDROID_IMAGEFORMAT_NV21;
            case ImageFormat.UNKNOWN:
            default:
                return AndroidImageFormatList.ANDROID_IMAGEFORMAT_UNKNOWN;
        }
*/
    }

    public GLSurfaceView getSurfaceView() {  // (GL2CameraEye)
        return mGlSurfaceView;
    }

    //@CalledByNative
    public int startPreview() {
        Log.d(TAG, "startCapture");
        if (mCamera == null) {
            Log.e(TAG, "startCapture: camera is null");
            return -1;
        }

        mPreviewBufferLock.lock();
        try {
            if (mIsRunning) {
                return 0;
            }
            mIsRunning = true;
        } finally {
            mPreviewBufferLock.unlock();
        }
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.startPreview();
        isPreview = true;

        return 0;
    }

    //@CalledByNative
    public int stopPreview() {
    	if(mMuxer!=null){
    		mMuxer.stop();
    	}
        Log.d(TAG, "stopCapture");
        if (mCamera == null) {
            Log.e(TAG, "stopCapture: camera is null");
            return 0;
        }

        mPreviewBufferLock.lock();
        try {
            if (!mIsRunning) {
                return 0;
            }
            mIsRunning = false;
        } finally {
            mPreviewBufferLock.unlock();
        }

        mCamera.stopPreview();
        mCamera.setPreviewCallbackWithBuffer(null);
        return 0;
    }

    //@CalledByNative
    public void stop() {
    	
        Log.d(TAG, "deallocate");
        if (mCamera == null){
            return;
        }

        stopPreview();
        mCaptureFormat = null;
        mCamera.release();
        mCamera = null;
        mGlSurfaceView.onPause();
        //mLooper.quit();  // Don't quit if we're the main loop (GL2CameraEye).
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mPreviewBufferLock.lock();
        try {
            if (!mIsRunning) {
                return;
            }
            if (data.length == mExpectedFrameSize) {
                int rotation = getDeviceOrientation();
                if (rotation != mDeviceOrientation) {
                    mDeviceOrientation = rotation;
                    Log.d(TAG,
                          "onPreviewFrame: device orientation=" +
                          mDeviceOrientation + ", camera orientation=" +
                          mCameraOrientation);
                }
                if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    rotation = 360 - rotation;
                }
                rotation = (mCameraOrientation + rotation) % 360;
                //nativeOnFrameAvailable(mNativeVideoCaptureDeviceAndroid,
                //        data, mExpectedFrameSize, rotation);
            }
        } finally {
            mPreviewBufferLock.unlock();
            if (camera != null) {
                camera.addCallbackBuffer(data);
            }
        }
    }

    MediaMuxerWrapper mMuxer = null;
    PatronStream patronStream = null;
    public void setPatronStream(PatronStream patronStream){
        this.patronStream = patronStream;
        Log.e(TAG,"patronStream be set in videopreview");
    }

    @Override
    public void onFrame(byte[] data, int data_size) {
        mPreviewBufferLock.lock();
        byte [] buf = null;
        if(stream_on || shortVideo_on){
            buf = ColorConvert.rgbaToargb(data, data_size);  //颜色转换

            try {
                counter++;
                if(stream_on){
                    //启动RTSP流
                    if(patronStream != null){
                        patronStream.writeVideoSampleData(buf);
                    }
                }
                if(shortVideo_on){
                        //启动小视频
                    if(mMuxer==null){
                        final String path = "/sdcard/rec/c_"+System.currentTimeMillis()+".mp4";
                        mMuxer =  MediaMuxerWrapper.create(path);
                        mMuxer.addVideoTrack(640, 480, 30, MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888);
                        mMuxer.start();
                        Timer timer = new Timer();
                        TimerTask timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                mMuxer.stop();
                                if(videoCallback != null){
                                    videoCallback.onShortVideoFinished(path);
                                }
                            }
                        };
                        timer.schedule(timerTask,30*1000);
                    }else {
                        this.mMuxer.writeVideoSample(buf);
                    }
                }

                if(capture_picture){
                    Bitmap bitmap = MyBitmapFactory.createMyBitmap(buf,640,480);
                    File pictureFile = new File("/sdcard/rec/PIC_"+System.currentTimeMillis()+".jpg");
                    OutputStream outputStream = new FileOutputStream(pictureFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,90,outputStream);
                    if(videoCallback != null){
                        videoCallback.onCaptured(pictureFile.getPath());
                    }

                }

                //Log.e(TAG, ""+System.currentTimeMillis()+"  frame: " + counter +"  size:"+data_size+"  data.length:"+data.length);


            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mPreviewBufferLock.unlock();
            }
        }
    }
    
    private int counter = 0;


    /*private native void nativeOnFrameAvailable(
            long nativeVideoCaptureDeviceAndroid,
            byte[] data,
            int length,
            int rotation);*/

    public int getDeviceOrientation() {
        int orientation = 0;
        if (mContext != null) {
            WindowManager wm = (WindowManager) mContext.getSystemService(
                    Context.WINDOW_SERVICE);
            switch(wm.getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_90:
                    orientation = 90;
                    break;
                case Surface.ROTATION_180:
                    orientation = 180;
                    break;
                case Surface.ROTATION_270:
                    orientation = 270;
                    break;
                case Surface.ROTATION_0:
                default:
                    orientation = 0;
                    break;
            }
        }
        return orientation;
    }

	private final int VIDEO_WIDTH = 1920;
	private final int VIDEO_HEIGHT = 1080;
    private MediaRecorder mRecorder = null;
    private boolean recording = false;
    private String mOutputFile = null;
    public void setOutputFile(String file){
    	this.mOutputFile = file;
    }
    public boolean isRecording(){
    	return this.recording;
    }
    public void startRecord() throws IllegalStateException, IOException{
    	if (this.mRecorder == null) {
			this.mRecorder = new MediaRecorder();
		}
		this.mRecorder.reset();
		/* ====== initCarema =========== */
		try {
			if (this.mCamera != null) {
				this.mCamera.unlock();
			}
			this.mRecorder.setCamera(mCamera);
		} catch (Throwable e) {
			if (this.mCamera != null) {
				this.mCamera.release();
			}
			this.mCamera = null;
		}
		/* ================== */
		this.mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if(audio_record){
            this.mRecorder.setAudioSource(MediaRecorder.VideoSource.CAMERA);
        }
		this.mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		this.mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if(audio_record){
            this.mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        }


		this.mRecorder.setVideoEncodingBitRate(8000000);
		this.mRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);
		this.mRecorder.setVideoFrameRate(30);
//		this.mRecorder.setPreviewDisplay(this.mSurfaceHolder.getSurface());
		if(mOutputFile!=null){
			this.mRecorder.setOutputFile(mOutputFile);
		}
		this.mRecorder.prepare();
		this.mRecorder.start();
		recording = true;
    }
    public void stopRecord(){
    	if(mRecorder!=null){
    		mRecorder.stop();
    		mRecorder.release();
    		mRecorder = null;
            if(videoCallback != null){
                videoCallback.onVideoFinished(mOutputFile,videoType);
            }
    	}
    	recording = false;
    }


    private VideoCallback videoCallback;
    public void setVideoCallback(VideoCallback videoCallback){
        this.videoCallback = videoCallback;
    }

    public interface VideoCallback{
        /**
         * 图片拍摄完成回调
         * @param path 图片路径
         */
        public void onCaptured(String path);

        /**
         * 小视频录制成功
         * @param path 小视频路径
         */
        public void onShortVideoFinished(String path);

        /**
         * 小视频录制成功
         * @param path  视频路径
         * @param type  视频类型,1为正常,2为锁定
         */
        public void onVideoFinished(String path,int type);


    }
}