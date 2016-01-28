package net.majorkernelpanic.streaming.gl2cameraeye;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaMuxer.OutputFormat;
import android.os.Build;
import android.util.Log;

public class MediaMuxerWrapper {

	private static final String TAG = "MediaMuxerWrapper";

	private MediaMuxer mMuxer;
	private boolean mStarted;

	private TrackInfo mVideoTrack;
	private TrackInfo mAudioTrack;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
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
	public void addVideoTrack(int width, int height, int frameRate, int colorFormat) throws IOException {
		if (this.mVideoTrack == null) {
			MediaCodec codec = createVideoEncoder(width, height, frameRate, colorFormat);
			this.mVideoTrack = new TrackInfo(this, codec);
		}
	}
	public void addAudioTrack() throws IOException {
		if (this.mAudioTrack == null) {
			MediaCodec codec = createAudioEncoder();
			this.mAudioTrack = new TrackInfo(this, codec);
		}
	}
	public TrackInfo getVideoTrack() {
		return this.mVideoTrack;
	}
	public TrackInfo getAudioTrack() {
		return this.mAudioTrack;
	}

	public void start() {
		if (this.mAudioTrack != null) {
			this.mAudioTrack.start();
		}
		if (this.mVideoTrack != null) {
			this.mVideoTrack.start();
		}
	}
	public boolean isStarted() {
		return this.mStarted;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public void stop() {
		try {
			if (this.mVideoTrack != null) {
				this.mVideoTrack.stop();
				this.mVideoTrack = null;
			}
			if (this.mAudioTrack != null) {
				this.mAudioTrack.stop();
				this.mAudioTrack = null;
			}
			if (mStarted) {
				this.mMuxer.stop();
				this.mMuxer.release();
				this.mMuxer = null;
			}
			this.mStarted = false;
		} catch (Throwable e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private synchronized boolean tryStarted() {
		if (!this.isStarted()) {
			if (this.mVideoTrack != null && this.mVideoTrack.getTrackIndex() == -1) {
				return false;
			}
			if (this.mAudioTrack != null && this.mAudioTrack.getTrackIndex() == -1) {
				return false;
			}
			Log.e(TAG, "Start MediaMuxer..........");
			this.mMuxer.start();
			this.mStarted = true;
			return true;
		}
		return false;
	}


	/* ===================================================== */

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private static MediaCodec createVideoEncoder(int width, int height, int frameRate, int colorFormat) throws IOException {
		if(colorFormat==-1){
			colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
		}
		MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat/*MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar*/);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, (width * height) << 3);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

		MediaCodec mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
		mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

		return mediaCodec;
	}

	/**
	 * 固定为双声道,采样率:44.1kHz 比特率:1411200 PCM:16bit
	 * @return
	 * @throws IOException
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private static MediaCodec createAudioEncoder() throws IOException {
		//TODO: 参数可外部输入
		MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1411200);
		mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

		MediaCodec mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
		mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

		return mediaCodec;
	}

	private final static int TIME_UNIT = 100;
	private final static Object SYNC = new Object();

	public boolean writeVideoSample(byte[] frame) {
		if (this.mAudioTrack != null && this.mAudioTrack.getTrackIndex() == -1) {
			// 音频尚未准备好
			return false;
		}
		return offerSample(this.mVideoTrack, frame);
	}

	public boolean writeAudioSample(byte[] data) {
		return offerSample(this.mAudioTrack, data);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private boolean offerSample(TrackInfo track, byte[] data) {
		if (track != null && track.isStarted()) {
			Log.e(TAG, "offerSample: " + data.length);
			synchronized (SYNC) {
				MediaCodec mediaCodec = track.getCodec();
				int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIME_UNIT);
				if (inputBufferIndex >= 0) {
					ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
					inputBuffer.clear();
					inputBuffer.put(data);
					mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length, 0, 0);
				}
			}
			drainEncoder(track);
			return true;
		}
		return false;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private synchronized void drainEncoder(TrackInfo track) {
		Log.e(TAG, "drainEncoder...");
		MediaCodec codec = track.getCodec();
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		while (true) {
			synchronized (SYNC) {
				int encoderStatus = codec.dequeueOutputBuffer(bufferInfo, TIME_UNIT);
				if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//					Log.e(TAG, "INFO_TRY_AGAIN_LATER...");
					break;
				} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					if (!isStarted()) {
						track.addTrack(codec.getOutputFormat());
						if (!tryStarted()) {
							Log.e(TAG, "MediaMuxer Started Failure. wait Other Track!");
							break;
						}
						Log.e(TAG, "MediaMuxer Started!");
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
					if (bufferInfo.size != 0) {
						if (!this.isStarted()) {
							Log.d(TAG, "Wait MediaMuxer Started!");
						} else {
							outputBuffer.position(bufferInfo.offset);
							outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
							if (bufferInfo.presentationTimeUs < 0) {
								bufferInfo.presentationTimeUs = 0;
							}
							write(track, outputBuffer, bufferInfo);
						}
					}
					codec.releaseOutputBuffer(encoderStatus, false);
					if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						Log.e(TAG, "BUFFER_FLAG_END_OF_STREAM...");
						break;
					}
				}
			}
		}
	}

	private long mNextVideoTime = System.currentTimeMillis(); // 第一帧,启动编码器
	private long mNextAudioTime = 0;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private void write(TrackInfo track, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
		if (track == this.mVideoTrack) {
			bufferInfo.presentationTimeUs = (System.currentTimeMillis() - mNextVideoTime) * 1000;
		} else {
			bufferInfo.presentationTimeUs = mNextAudioTime;
			mNextAudioTime += 23219;// TODO:根据采样率调整,做成外部输入
		}
		try {
			Log.d(TAG, "write writeSampleData track:"+track.getTrackIndex()+" len:"+bufferInfo.size);
			synchronized (this) {
				this.mMuxer.writeSampleData(track.getTrackIndex(), buffer, bufferInfo);
			}
		} catch (Throwable e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private static class TrackInfo {
		MediaMuxerWrapper mMuxer;
		MediaCodec mCodec;
		int mTrackIdx = -1;
		boolean mStarted = false;

		public TrackInfo(MediaMuxerWrapper muxer, MediaCodec codec) {
			this.mMuxer = muxer;
			this.mCodec = codec;
		}

		public MediaCodec getCodec() {
			return mCodec;
		}

		public int getTrackIndex() {
			return mTrackIdx;
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		public void addTrack(MediaFormat format) {
			this.mTrackIdx = this.mMuxer.getMediaMuxer().addTrack(format);
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		public void start() {
			if (this.mCodec != null) {
				this.mCodec.start();
			}
			this.mStarted = true;
		}

		public boolean isStarted() {
			return mStarted;
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		public void stop() {
			if (this.mCodec != null) {
				this.mCodec.stop();
				this.mCodec.release();
				this.mCodec = null;
			}
			this.mStarted = false;
		}
	}
}
