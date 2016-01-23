package cn.xvping.H264Encoder;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaMuxer.OutputFormat;
import android.os.Build;

import com.yysky.commons.Log;
import com.yysky.commons.utils.ByteUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

//import android.util.Log;

public class MediaMuxerWrapper {

    private static final String TAG = "MediaMuxerWrapper";
    private final static int TRACK_TOTALS = 2;
    private MediaMuxer mMuxer;
    private int mTrackAdded = 0;
    private boolean mStarted;

    private TrackInfo mVideoTrack;
    private TrackInfo mAudioTrack;

    public TrackInfo getVideoTrackInfo() {
        return this.mVideoTrack;
    }

    public TrackInfo getAudioTrack() {
        return this.mAudioTrack;
    }

    public static MediaMuxerWrapper create(String savefile) throws Exception {
        MediaMuxer muxer = new MediaMuxer(savefile, OutputFormat.MUXER_OUTPUT_MPEG_4);
        return new MediaMuxerWrapper(muxer);
    }

    private MediaMuxerWrapper(MediaMuxer muxer) {
        this.mMuxer = muxer;
    }

    public MediaMuxer getMediaMuxer() {
        return this.mMuxer;
    }

    public void start() {
        if (!mStarted) {
            this.mMuxer.start();
            mStarted = true;
            videoNextTime = System.currentTimeMillis();
        }
    }

    private boolean mVideoTrackAdded = false;

    public int addVideoTrack(MediaFormat format) {
        int idx = this.mMuxer.addTrack(format);
        this.mVideoTrackAdded = true;
        return idx;
    }

    private boolean mAudioTrackAdded = false;

    public int addAudioTrack(MediaFormat format) {
        int idx = this.mMuxer.addTrack(format);
        this.mAudioTrackAdded = true;
        return idx;
    }

    public synchronized boolean tryStarted() {
        if (mVideoTrackAdded && mAudioTrackAdded) {
            if (!this.isStarted()) {
                this.mMuxer.start();
                this.mStarted = true;
            }
            return true;
        }
        return false;
    }

    public boolean isStarted() {
        return this.mStarted;
    }

    public void stop(TrackInfo trackInfo) {
        try {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            info.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            info.size = 0;
            info.presentationTimeUs = 0;
            if (this.mMuxer == null) {
                this.mStarted = false;
            } else {
                this.mMuxer.writeSampleData(trackInfo.trackIdx, ByteBuffer.wrap(new byte[0]), info);
                this.mMuxer.stop();
                this.mMuxer.release();
                this.mMuxer = null;
                this.mStarted = false;
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

	/* ===================================================== */

    public static MediaCodec createVideoEncoder() throws IOException {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 320, 240);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, (320 * 240) << 3);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        MediaCodec mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        return mediaCodec;
    }

    public static MediaCodec createAudioEncoder() throws IOException {
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        MediaCodec mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        return mediaCodec;
    }

    private final static int TIME_UNIT = 10 * 1000;

    private final static Object sync = new Object();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void writeSample(MediaMuxerWrapper muxer, MediaCodec mediaCodec, TrackInfo info, byte[] frame) {
        synchronized (sync) {
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIME_UNIT);//等缓冲区
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(frame);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, frame.length, 0, 0);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void drainEncoder(MediaMuxerWrapper muxer, MediaCodec codec, TrackInfo info, boolean endOfStream) {
//		if (endOfStream) {
//			codec.signalEndOfInputStream();
//		}
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            synchronized (sync) {
                int encoderStatus = codec.dequeueOutputBuffer(bufferInfo, TIME_UNIT);
//				Log.e(TAG, "Video BufferIndex:" + encoderStatus);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//					Log.d(TAG,"EncoderStatus :" + encoderStatus + "Break");
                    break;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxer.isStarted()) {
                        int trackInfo = muxer.addVideoTrack(codec.getOutputFormat());
                        info.trackIdx = trackInfo;
                        if (!muxer.tryStarted()) {
                            Log.d(TAG, "MediaMuxer Started Failure. wait Audio Track!");
                            break;
                        }
                        Log.e(TAG, "Start MediaMuxer in Video Track:" + info.trackIdx);
                    } else {
                        Log.e(TAG, "MediaMuxer Video Track already started");
                    }
                } else if (encoderStatus < 0) {
                    Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else {
                    ByteBuffer outputBuffer = codec.getOutputBuffer(encoderStatus);
                    if (outputBuffer == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                    }
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        bufferInfo.size = 0;
                    }
                    if (bufferInfo.size != 0 && muxer.isStarted()) {
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        if (bufferInfo.presentationTimeUs < 0) {
                            bufferInfo.presentationTimeUs = 0;
                        }
                        muxer.write(info.trackIdx, outputBuffer, bufferInfo);
                        Log.d(TAG, "trackIndex:" + info.trackIdx + " sent " + bufferInfo.size + " bytes to muxer, ts=" +
                                bufferInfo.presentationTimeUs);
                    }
                    codec.releaseOutputBuffer(encoderStatus, false);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "bufferInfoFlag end of stream :" + (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) + "Break");
                        break;
                    }
                }
                if (endOfStream) {
                    Log.e(TAG, "Break end of stream:" + endOfStream);
                    break;
                }
            }
        }
    }

    private long videoNextTime = System.currentTimeMillis(); //第一帧,启动编码器
    public long videoTime;

    public void write(int trackIndex, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
        if (videoTime!=0){
            bufferInfo.presentationTimeUs = (videoTime - videoNextTime) * 1000;
            Log.e(TAG, "  videoTime:" + videoTime + "  videoNextTime:" + videoNextTime);
        }else {
            bufferInfo.presentationTimeUs = (System.currentTimeMillis() - videoNextTime) * 1000;
        }
        byte[] buff = new byte[bufferInfo.size];
        buffer.get(buff);
        Log.e(TAG, buff.length + " bytes written. flag:" + bufferInfo.flags + "  offset:" + bufferInfo.offset + "  presentationTimeUs:" + bufferInfo.presentationTimeUs);

        byte[] tmp = new byte[6];
        System.arraycopy(buff, 0, tmp, 0, tmp.length);

        Log.e(TAG, "trackIndex:" + trackIndex + "  len:" + buff.length + " bytes written. Info:  " + ByteUtils.binaryToHexString(tmp, true));
        try {
            synchronized (this) {
                this.mMuxer.writeSampleData(trackIndex, buffer, bufferInfo);
            }
            Log.e(TAG, "Sample Data Written!");
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static class TrackInfo {
        MediaMuxerWrapper muxer;
        MediaCodec codec;
        int trackIdx = -1;

        public MediaMuxerWrapper getMuxer() {
            return muxer;
        }

        public MediaCodec getCodec() {
            return codec;
        }

        public int getTrackIndex() {
            return trackIdx;
        }
    }
}
