package cn.xvping.H264Encoder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.os.Build;
//import android.util.Log;
import android.util.Size;
import android.widget.Toast;


import com.deepwits.pine.recorder.AsyncQueue;
import com.deepwits.pine.recorder.AudioFunc;
import com.deepwits.pine.recorder.BackCamera;
import com.deepwits.pine.recorder.Config;
import com.deepwits.pine.recorder.FrameData;
import com.deepwits.pine.recorder.FrontCamera;
import com.deepwits.pine.recorder.RecordManager;
import com.deepwits.pine.recorder.RecordService;
import com.deepwits.pine.recorder.SimpleSpeechSynthesisControl;
import com.yysky.commons.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import H264Encoder.MediaMuxerWrapper;

/**
 * Created by xp on 15-8-28.
 */
public class Encoder implements Camera.PreviewCallback {
    private static final String TAG = "Encoder";
    private static Context mContext;
    long encoder = 0;
    RandomAccessFile raf = null;
    byte[] h264Buff = null;

    static {
        System.loadLibrary("x264");
    }

    //private Context mContext;
    private int cameraSelector;
    private byte[] vidByte;
    private byte[] picByte;
    private MediaMuxerWrapper muxer;
    MediaMuxer mediaMuxer;
    private int videoTrackIndex;

    private MediaCodec.BufferInfo bufferInfo;
    public boolean isEncoder = false;
    private byte[] isKeyFrame = new byte[1];
    //private MediaFormat videoFormat;
    private boolean isEnd = false;
    private ByteBuffer byteBuffer;
    private ByteBuffer avccbuf;
    private ByteBuffer byteBuffer1;
    private ByteBuffer byte_csd0;
    private ByteBuffer byte_csd1;
    private ByteBuffer audio_byte_csd0;
    private long audio_nexttime;
    private long video_nexttime;
    private RecordManager mRecorder;
    private RecordService mService;
    private FrontCamera mFrontCamera;
    private BackCamera mBackCamera;

    public boolean isTakePic = false;
    private File videoOutputFile;
    private File picOutputFile;

    //图片拍照信息参数
    private int picQuality;
    private String picType;
    private int videoQuality;
    private int videoFps;
    private String videoType;
    private AudioFunc audioFunc;
    public boolean isWriteSampleData;
    private int framecount = 0;
    private Size inSize;
    private Size videoOutSize;
    private Size picOutSize;
    private boolean isSetedPreviewSize;
    public boolean mMuxerStarted;
    private YuvImage image;
    private static MediaPlayer mediaPlayer;

    private MediaMuxerWrapper.TrackInfo trackInfo = new MediaMuxerWrapper.TrackInfo();
    //定义阻塞FIFO队列
    private AsyncQueue<FrameData> asyncQueue = new AsyncQueue<>();

    public Encoder() {
    }

    /**
     * @param cameraSelector 0为后置摄像头，1为前置
     * @param mService
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public Encoder(int cameraSelector, RecordService mService, Context context) {
        Log.d(TAG, "Encoder Construct....cameraSelector:" + cameraSelector);
        mContext = context;
        cameraClick();
        this.cameraSelector = cameraSelector;
        this.mService = mService;
        this.mRecorder = mService.getRecorderManager();
        this.mFrontCamera = mRecorder.getmFrontCamera();
        this.mBackCamera = mRecorder.getmBackCamera();
        h264Buff = new byte[320 * 240 * 8];

        audio_nexttime = 0;
        video_nexttime = 0;

        Log.d(TAG, "Encoder Construct success....");

    }

    public static void cameraClick() {
        Log.d(TAG, "cameraClick......");
        try {
            AssetFileDescriptor assetFileDescriptor = mContext.getAssets().openFd("camera_click.ogg");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(),
                    assetFileDescriptor.getLength());
            mediaPlayer.prepare(); //准备
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "cameraClick  success......");
    }

    public synchronized String takePic(int width, int height, int quality, String type) {
        this.mFrontCamera = mRecorder.getmFrontCamera(); //前置
        this.mBackCamera = mRecorder.getmBackCamera(); //后置
        Log.e(TAG, "begin to take pic width:" + width + " height:" + height);
        if (!isTakePic) {
            Fraction fraction = new Fraction(width, height);
            if (!fraction.equals(new Fraction(16, 9)) && !fraction.equals(new Fraction(4, 3))) {  //参数不合法,采用默认参数
                //this.picOutSize = new Size(1280, 720);
                this.picOutSize = new Size(1920, 1080);
            } else {
                this.picOutSize = new Size(width, height);   //图片尺寸
            }
            //isSetedPreviewSize = false;
            //this.picByte = new byte[picOutSize.getWidth() * picOutSize.getHeight() * 3 / 2];
            this.picQuality = quality;
            this.picType = type;
            if (type == null || type.equalsIgnoreCase("voice_photo")) {
                picOutputFile = mRecorder.getOutputMediaFile(mRecorder.MEDIA_TYPE_TAKE_IMAGE);
            } else {
                picOutputFile = mRecorder.getOutputMediaFile(mRecorder.MEDIA_TYPE_UPLOAD_IMAGE); //图片存储路径
            }
            Log.i(TAG, "图片路径：pic output path:" + picOutputFile);
            if (picOutputFile == null) {
                isTakePic = false;
                mRecorder.isTakePic = false;
                return null;
            }
            isTakePic = true;
            return picOutputFile.getAbsolutePath();
        } else return null;
    }

//    private FileOutputStream fos = null;
//    private FileOutputStream vidBack = null;
//    private FileOutputStream data = null;

    private MediaCodec mVideoCodec;
    private int conut_chu = 0;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public String startShortVideo(int width, int height, int quality, int fps, String type) throws Exception {
        this.mBackCamera = mRecorder.getmBackCamera(); //后置
        this.mFrontCamera = mRecorder.getmFrontCamera();
        this.videoQuality = quality;
        this.videoFps = fps;
        this.videoType = type;

        count_put = 0;
        Log.e(TAG, "Encoder startShortVideo " + isEncoder + " count:" + count_put);
        if (!isEncoder) {
            Fraction fraction = new Fraction(width, height);
            if (!fraction.equals(new Fraction(16, 9)) || fraction.equals(new Fraction(4, 3))) {  //参数不合法,采用默认参数
                this.videoOutSize = new Size(320, 240);
            } else {
                this.videoOutSize = new Size(width, height);   //图片尺寸
            }
            isSetedPreviewSize = false;
            vidByte = new byte[videoOutSize.getWidth() * videoOutSize.getHeight() * 3 / 2]; //320*240*1.5
            //downSampledByte = new byte[videoOutSize.getWidth() * videoOutSize.getHeight() * 3 / 2];
            this.mVideoCodec = MediaMuxerWrapper.createVideoEncoder();

            videoOutputFile = new File(Config.getUPLOAD_VIDEO_PATH() + "UPLOAD_" + System.currentTimeMillis() + ".mp4");

            muxer = MediaMuxerWrapper.create(videoOutputFile.getAbsolutePath());//创建MediaMuxer

            audioFunc = new AudioFunc(muxer, this);  //要在muxer对象新建之后创建
            audioFunc.startRecord();           //启动录像线程

            try {
                Thread.sleep(500);//让音频等视频、防止丢帧
            } catch (Throwable e) {
                Log.e(TAG, e.getMessage(), e);
            }
            this.mVideoCodec.start();
//           videoTrackIndex = muxer.addTrack(videoFormat);  //添加 video Track
//           muxer.start();                     //启动muxer
            video_nexttime = System.currentTimeMillis();
            isEnd = false;
            isEncoder = true;
            asyncQueue.setHandler(new AsyncQueue.Handler<FrameData>() {
                @Override
                public void onStart() {

                }

                @Override
                public void onData(FrameData frameData) {
                    //System.arraycopy(bytes, 0, downSampledByte, 0, downSampledByte.length);
                    //Log.e(TAG, "queue.isStop2:" + queue.isStop() + " queue.isEmpty2:" + queue.isEmpty());
                    muxer.videoTime = frameData.time;
                    MediaMuxerWrapper.writeSample(muxer, mVideoCodec, trackInfo, frameData.data); //MediaCodec对下采样之后的数据进行编码
                    MediaMuxerWrapper.drainEncoder(muxer, mVideoCodec, trackInfo, false);
                    conut_chu++;
                }

                @Override
                public void onFinish() {
                    MediaMuxerWrapper.drainEncoder(muxer, mVideoCodec, trackInfo, true);
                    stopVideoAndRelease();
                }
            });
            asyncQueue.start();
            Log.e(TAG, "Encoder startShortVideo success !! " + "isEncoder" + isEncoder + "Output Path :" + videoOutputFile.getAbsolutePath());
            return videoOutputFile.getAbsolutePath();
        } else return null;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopShortVideo() {
        Log.i(TAG, "stopShortVideo " + isEncoder);
        //isEnd = true;       //标志结束
        isEncoder = false; //不往队列送数据(不在生产数据)
        asyncQueue.stop();
        Log.i(TAG, "stopShortVideoed!!! " + isEncoder);
    }

    private void finishRecorder() {
//        if (this.vidBack != null) {
//            try {
//                this.vidBack.close();
//            } catch (Throwable e) {
//                Log.e(TAG, e.getMessage(), e);
//            }
//        }
//        if (this.fos != null) {
//            try {
//                this.fos.close();
//            } catch (Throwable e) {
//                Log.e(TAG, e.getMessage(), e);
//            }
//        }
//        if (this.data != null) {
//            try {
//                this.data.close();
//            } catch (Throwable e) {
//                Log.e(TAG, e.getMessage(), e);
//            }
//        }
    }

    private void stopVideoAndRelease() {
        Log.e(TAG, "stopVideoAndRelease......put :" + count_put + "  chu:" + conut_chu);
//        if (this.videoWriterThread != null) {
//            this.videoWriterThread.sendStopSignal();
//            synchronized (lock) {
//                try {
//                    lock.wait(500);
//                } catch (InterruptedException e) {
//                    Log.e(TAG, e.getMessage(), e);
//                }
//            }
//            this.videoWriterThread = null;
//        }
        if (mVideoCodec != null) {
            Log.d(TAG, "VideoCodec Stop");
            this.mVideoCodec.stop();
            this.mVideoCodec.release();
            this.mVideoCodec = null;
            Log.d(TAG, "VideoCodec Stoped!!" + this.mVideoCodec);
        }
        mRecorder.mHandler.post(new Runnable() {
            @Override
            public void run() {
                audioFunc.stopRecord();
                synchronized (audioFunc) {
                    while (audioFunc.isRecord != false) {
                        try {
                            audioFunc.wait();
                        } catch (InterruptedException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                }
                muxer.stop(trackInfo);
                isEnd = true; //小视频录制结束
                Log.i(TAG, "stop shortvideo" + "isEncoder" + isEncoder);
                addFileToDB(true);     //视频添加到数据库
                mRecorder.isRecordingShortVideo = false; //录制完成
                if (cameraSelector == 0 || cameraSelector == 1) {  //小视频录制成功广播
                    mRecorder.sendBroadcast(cameraSelector, videoType, videoOutputFile.getAbsolutePath());
                }
                if (!videoType.equalsIgnoreCase("stopEvent")) {
                    //Toast.makeText(mService, "监控视频录制成功", Toast.LENGTH_SHORT).show();
                    SimpleSpeechSynthesisControl.startSpeechSynthesis(mService, "视频录制成功");
                }
                if (mService.getRecorderManager().afterOrderStart) {
                    mService.getRecorderManager().startRecord();
                    mService.getRecorderManager().afterOrderStart = false;
                }
                if (mRecorder.isCloseCamera){
                    mService.CloseCamera();
                }
            }
        });
    }


    private synchronized void stopPicAndRelease() {
        Log.e(TAG, "stopPicAndRelease......!!");
        mRecorder.mHandler.post(new Runnable() {
            @Override
            public void run() {
                FileOutputStream filecon = null;
                try {
                    filecon = new FileOutputStream(picOutputFile.getAbsolutePath());
                    if (image != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, stream);//YuvImage压缩
                        Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        Bitmap bitmap = Bitmap.createScaledBitmap(bmp, picOutSize.getWidth(), picOutSize.getHeight(), true);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, filecon);  //把位图输出到指定的文件
                    }
                } catch (Throwable e) {
                    isTakePic = false;
                    mRecorder.isTakePic = false;
                    Log.e(TAG, e.getMessage(), e);
                }
                addFileToDB(false);  //照片添加到数据库
                if (picType == null) {    //手动拍照
                    mediaPlayer.start();//播放声音
                    mRecorder.reflesh(false, picOutputFile.getAbsolutePath());  //拍照成功,刷新媒体库
                    Log.e(TAG, "pic path:" + picOutputFile.getAbsolutePath());
                } else if (picType.equalsIgnoreCase("voice_photo")) {
                    mediaPlayer.start();
                    mRecorder.reflesh(false, picOutputFile.getAbsolutePath());  //拍照成功,刷新媒体库
                    mRecorder.sendBroadcast(cameraSelector, picType, picOutputFile.getAbsolutePath());
                } else {
                    if (cameraSelector == 0 || cameraSelector == 1) {
                        mediaPlayer.start();
                        mRecorder.sendBroadcast(cameraSelector, picType, picOutputFile.getAbsolutePath());
                        Log.e(TAG, "pic path:" + picOutputFile.getAbsolutePath());
                    }
                }
                isTakePic = false;
                mRecorder.isTakePic = false;
                Log.e(TAG, "isTakePic: " + isTakePic + " afterOrderStart:" + mService.getRecorderManager().afterOrderStart);
                if (mRecorder.isCloseCamera){
                    mService.CloseCamera();
                }
            }
        });
        if (mService.getRecorderManager().afterOrderStart) {
            mService.getRecorderManager().startRecord();
            mService.getRecorderManager().afterOrderStart = false;
        }
        Log.e(TAG, "stopPicAndRelease end !!");
    }

    private void addFileToDB(boolean isVideo) {
        Log.d(TAG, "addFileToDB ..... !!" + "isVideo" + isVideo);
        if (isVideo) {
            mService.getStorageManager().addToDB(videoOutputFile.getAbsolutePath());
        } else {
            mService.getStorageManager().addToDB(picOutputFile.getAbsolutePath());
        }
        Log.d(TAG, "addFileToDB ..... success!!");
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected void finalize() {
        if (muxer != null) {
            muxer.stop(trackInfo);//.release();
        }
        try {
            super.finalize();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void removePreviewCallback() {
        Log.d(TAG, "removePreviewCallback.....");
        if (cameraSelector == 0 && mRecorder != null) {
            mRecorder.removePreviewCallback();  //移除callback，使回调函数失效
            //mRecorder.releaseCameraAndPreview();
            Log.i(TAG, "remove camera 0 PreviewCallback" + "cameraSelector" + cameraSelector);
        }
        if (cameraSelector == 1 && mFrontCamera != null) {
            mFrontCamera.releaseCameraAndPreview();
            Log.i(TAG, "releaseCameraAndPreview camera 1" + "cameraSelector" + cameraSelector);
        }
        if (cameraSelector == 0 && mBackCamera != null) {
            mBackCamera.releaseCameraAndPreview();
            Log.i(TAG, "releaseCameraAndPreview camera 1" + "cameraSelector" + cameraSelector);
        }
        Log.d(TAG, "removePreviewCallback end .....");
    }


    public native long CompressBegin(int width, int height);

    public native int CompressBuffer(long encoder, int type, byte[] in, int insize, byte[] out, byte[] isKeyFrame);

    public native int CompressEnd(long encoder);

    private int count_put = 0;

    public void onPreviewFrame(byte[] data, Camera camera) {  //尽量减少本函数中所做的工作
        //Log.i(TAG, "camera:" + cameraSelector + " is onPreviewFrame " + isEncoder + " " + isTakePic);
        //Log.e("onPreviewFrame ", "count:" + count);
        if (!isSetedPreviewSize) {    //设置相机预览宽高
            Camera.Size size = camera.getParameters().getPreviewSize();
            inSize = new Size(size.width, size.height);
            isSetedPreviewSize = true;
        }
        if (isEncoder) {
            boolean isDownSampled = false;
            FrameData frameData = new FrameData();
            frameData.time = System.currentTimeMillis();
            frameData.data = new byte[videoOutSize.getWidth() * videoOutSize.getHeight() * 3 / 2];
            //byte[] downSamplingByte = new byte[videoOutSize.getWidth() * videoOutSize.getHeight() * 3 / 2];
            int ds = downSampling(data, inSize, frameData.data, videoOutSize);   //视频下采样
            isDownSampled = (1 == ds || -1 == ds);
            Log.e(TAG, "isDownSampled  isEnd:" + isEnd);
            if (isDownSampled) {
                asyncQueue.put(frameData); //进队列
                count_put++;
//                try {
//                    if(!queue.isStop()) {
//                        queue.produce(vidByte); //将下采样后的数据丢到队列中
//                    }
//                } catch (InterruptedException e) {
//                    Log.e(TAG, e.getMessage(), e);
//                }
//                if (consumerThread == null) {   //将数据丢给MediaCodec进行编码
//                    Log.e(TAG, "consumerThread == null  new");
//                    consumerThread = new Consumer(muxer, mVideoCodec, trackInfo);
//                    consumerThread.start();
//                }
            }
        }
        //拍照指令
        if (isTakePic) {
            isTakePic = false;//防止图片断层
            Log.e(TAG, "takePic");
            delImageData(data, camera);
        }
        //任务完成，清除回调函数
        if (!isEncoder && !isTakePic) {
            removePreviewCallback();
        }
        fillPreviewCallback(data, camera);
    }

    //填数据到frameBuffer
    private void fillPreviewCallback(byte[] data, Camera camera) {
        //采用setPreviewCallbackWithBuffer
        Log.d(TAG, "fillPreviewCallback....");
        if (cameraSelector == 0) {
            if (mRecorder != null) {
                if (mRecorder.frameBuffer == null) {
                    mRecorder.frameBuffer = new byte[inSize.getWidth() * inSize.getHeight() * 3 / 2];
                    Log.d(TAG, "allocate new framebuffer for backCamera");
                } else {
                    Log.d(TAG, "data addr " + data.length + " frameBuffer addr" + mRecorder.frameBuffer.length);
                }
                camera.addCallbackBuffer(mRecorder.frameBuffer);
            } else {
                Log.i(TAG, "mRecorder is null");
            }
            if (mBackCamera != null) {
                if (mBackCamera.frameBuffer == null) {
                    mBackCamera.frameBuffer = new byte[inSize.getWidth() * inSize.getHeight() * 3 / 2];
                    Log.d(TAG, "allocate new framebuffer for frontCamera");
                } else {
                    Log.i(TAG, "data addr " + data.length + " frameBuffer addr" + mBackCamera.frameBuffer.length);
                }
                camera.addCallbackBuffer(mBackCamera.frameBuffer);
            } else {
                Log.d(TAG, "mBackCamera is null");
            }
        } else if (cameraSelector == 1) {
            if (mFrontCamera != null) {
                if (mFrontCamera.frameBuffer == null) {
                    mFrontCamera.frameBuffer = new byte[inSize.getWidth() * inSize.getHeight() * 3 / 2];
                    Log.d(TAG, "allocate new framebuffer for frontCamera");
                } else {
                    Log.i(TAG, "data addr " + data.length + " frameBuffer addr" + mFrontCamera.frameBuffer.length);
                }
                camera.addCallbackBuffer(mFrontCamera.frameBuffer);
            } else {
                Log.d(TAG, "mFrontCamera is null");
            }
        }
        Log.d(TAG, "fillPreviewCallback success....");
    }

    private void delImageData(byte[] data, Camera camera) {
        Log.e(TAG, "delIamgeData....");
        //Log.e(TAG, "picOutSize: width" + picOutSize.getWidth() + "heigh" + picOutSize.getHeight());
        Log.e(TAG, "inSize: width" + inSize.getWidth() + "heigh" + inSize.getHeight());
        boolean isDownSampled = false;
        this.picByte = new byte[inSize.getWidth() * inSize.getHeight() * 3 / 2];
        System.arraycopy(data, 0, picByte, 0, picByte.length);
        image = new YuvImage(picByte, ImageFormat.NV21, inSize.getWidth(), inSize.getHeight(), null);
        isDownSampled = true; //所以的都不进行下采样，直接压缩
        if (isDownSampled) {
            stopPicAndRelease();   //写入文件
        } else {
            Log.e(TAG, "图片下采样失败");
        }
    }

//    private void delVideoData() {
//        if (!queue.isEmpty()) {
//            try {
//                System.arraycopy(queue.consume(), 0, downSampledByte, 0, downSampledByte.length);
//            } catch (InterruptedException e) {
//                Log.e(TAG, e.getMessage(), e);
//            }
//            MediaMuxerWrapper.writeSample(this.muxer, this.mVideoCodec, trackInfo, downSampledByte); //MediaCodec对下采样之后的数据进行编码
//            if (videoWriterThread == null) {
//                videoWriterThread = new ShortVideoProcessThread(this.muxer, this.mVideoCodec, trackInfo); //开线程使用MediaMuxer合音视频
//                videoWriterThread.start();
//            }
//            count++;
//        } else if(!isEncoder){
//            isEnd = true;
//        }
//    }

    /**
     * YUV图像下采样函数,仅支持NV21格式(YYYYYYYY VUVU), one of YUV420SP
     *
     * @param in      源图像数据
     * @param inSize  源图像尺寸
     * @param out     目标图像数据
     * @param outSize 目标图像尺寸
     * @return 是否下采样成功
     */
    public int downSampling(byte[] in, Size inSize, byte[] out, Size outSize) {    //下采样函数
        if (in == null || inSize == null || out == null || outSize == null) {
            Log.e(TAG, "下采样函数有参数为空,请检查 " + in + " " + inSize + " " + out + " " + outSize);
            return 0;
        }
        int inWidth = inSize.getWidth(), inHeight = inSize.getHeight();
        int outWidth = outSize.getWidth(), outHeight = outSize.getHeight();
        int inLength = inSize.getWidth() * inSize.getHeight() * 3 / 2;

        if (inSize.getHeight() < outSize.getHeight()) {
            Log.e(TAG, "源数据的尺寸小于目标数据尺寸,将采用源图像尺寸");
            return -1;
        }
        if (inSize.getHeight() == outSize.getHeight() && inSize.getWidth() == outSize.getWidth()) {
            Log.e(TAG, "源数据的尺寸等于目标数据尺寸,不进行下采样");
            return -1;
        }
        if (in.length != inLength) {
            Log.e(TAG, "源数据的长度(" + in.length + ")与源尺寸应有数据长度(" + inLength + ")不相同");
            return 0;
        }
        int outLength = outSize.getWidth() * outSize.getHeight() * 3 / 2;
        if (out.length != outLength) {
            Log.e(TAG, "目标数据的长度(" + out.length + ")与目标尺寸应有数据长度(" + outLength + ")不相同");
            return 0;
        }
        Fraction inFraction = new Fraction(inWidth, inHeight);
        Fraction outFraction = new Fraction(outWidth, outHeight);
        if (!inFraction.equals(new Fraction(16, 9))) {
            Log.e(TAG, "源数据比例不是16:9");
            return 0;
        }

        if (outFraction.equals(new Fraction(16, 9))) {  //比例不变的下采样
            int interval = inHeight / outHeight;       //隔点采样跳过的像素
            int SY = 0, SV = inWidth * inHeight, SU = SV + 1;
            int i = 0; //目标图像存放数组指针
            int linePtr = 0, ptr = 0;
            for (int j = 0; j < outHeight; j++) {
                ptr = 0;   //每行的采样点指针
                for (int k = 0; k < outWidth; k++) {
                    out[i++] = in[linePtr + ptr];
                    ptr += interval;
                }
                linePtr += inWidth * interval;
            }
            linePtr = SV;
            int iVout = i + (i >> 2);//outHeigth*outWidth+outHeight/2*outWidth/2;
            for (int j = 0; j < outHeight / 2; j++) {
                ptr = 0;
                for (int k = 0; k < outWidth / 2; k++) {
                    out[iVout++] = in[linePtr + ptr];   //隔点采样V分量
                    out[i++] = in[linePtr + ptr + 1];  //隔点采样U分量
                    ptr += interval * 2;
                }
                linePtr += inWidth * interval;           //下一个采样行
            }
            Log.e(TAG, "将比例为16:9的" + inSize.getWidth() + "*" + inSize.getHeight() + "等比下采样为:" + outSize.getWidth() + "*" + outSize.getHeight());
            return 1;
        } else if (outFraction.equals(new Fraction(4, 3))) {   //16:9 下采样到 4:3
            int sWidth = inWidth, sHeight = inHeight;   //原始图像大小(16:9)
            int dWidth = outWidth, dHeight = outHeight;        //目标图像大小(4:3)
            int interval = sHeight / dHeight;
            int tWidth = dWidth * interval, tHeight = dHeight * interval; //从原始图像中能裁到（4:3）的最大图像
            int widthInt = (sWidth - tWidth) / 2; // 裁剪丢掉的一边宽度,
            int heightInt = (sHeight - tHeight) / 2;
            int SY = 0, SV = sWidth * sHeight, SU = SV + 1;
            int DY = heightInt * sWidth + widthInt,         //目标图像Y起始位置
                    DV = SV + heightInt / 2 * sWidth + widthInt,  //目标图像V起始位置
                    DU = DV + 1;           //目标图像U起始位置
            int i = 0, //目标图像存放数组指针
                    ptr = 0;   //各分量在原始图像中采样指针
            //对Y进行下采样
            for (int j = 0; j < dHeight; j++) {
                for (int k = 0; k < dWidth; k++) {
                    out[i++] = in[DY + ptr];
                    ptr += interval;  //隔点采样
                }
                ptr += sWidth * (interval - 1) + (sWidth - tWidth);   //隔行采样
            }
            //对VU进行下采样
            int iVout = i + (i >> 2);//outHeigth*outWidth+outHeight/2*outWidth/2;
            int linePtr = SV + heightInt / 2 * sWidth;
            for (int j = 0; j < dHeight / 2; j++) {
                ptr = widthInt;   //每一行跳过裁剪宽度
                for (int k = 0; k < dWidth / 2; k++) {
                    out[iVout++] = in[linePtr + ptr];   //隔点采样V分量
                    out[i++] = in[linePtr + ptr + 1];  //隔点采样U分量
                    ptr += interval * 2;
                }
                linePtr += sWidth * interval;           //下一个采样行
            }
            Log.e(TAG, "将比例为16:9的" + inSize.getWidth() + "*" + inSize.getHeight() + "下采样为4:3的:" + outSize.getWidth() + "*" + outSize.getHeight());
            return 1;
        } else {
            Log.e(TAG, "目标数据比例不支持,仅支持16:9和4:3");
            return 0;
        }
    }
}