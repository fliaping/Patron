/*
 * Copyright (C) 2011-2014 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.streaming.rtp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.acl.LastOwnerException;
import java.util.logging.Handler;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Looper;
import android.util.Log;

import net.majorkernelpanic.streaming.gl2cameraeye.AsyncQueue;
import net.majorkernelpanic.streaming.gl2cameraeye.FrameData;

/**
 * An InputStream that uses data from a MediaCodec.
 * The purpose of this class is to interface existing RTP packetizers of
 * libstreaming with the new MediaCodec API. This class is not thread safe !  
 */
@SuppressLint("NewApi")
public class MediaCodecInputStream extends InputStream {

	public final String TAG = "MediaCodecInputStream"; 

	private MediaCodec mMediaCodec = null;
	private BufferInfo mBufferInfo = new BufferInfo();
	private ByteBuffer[] mBuffers = null;
	private ByteBuffer mBuffer = null;
	private int mIndex = -1;
	private boolean mClosed = false;

	private OutputStream outputStream = null;
	private byte[] outBuff = null;


	AsyncQueue asyncQueue = null;
	AsyncQueue.Handler handler = null;


	public MediaFormat mMediaFormat;
	private Looper mLooper;

	public MediaCodecInputStream(MediaCodec mediaCodec) {
		mMediaCodec = mediaCodec;
		mBuffers = mMediaCodec.getOutputBuffers();

		handler = new AsyncQueue.Handler<H264File>() {
			@Override
			public void onStart() {
				try {
					outputStream = new FileOutputStream(new File("/storage/sdcard0/rec/XX_"+System.currentTimeMillis()+".h264"));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

			}

			@Override
			public void onData(H264File h264File) {
				try {
					if(outputStream != null){
						outputStream.write(h264File.data,h264File.offset,h264File.length);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFinish() {
				if(outputStream != null){
					try {
						outputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
		};

	}

	@Override
	public void close() {
		mClosed = true;
		if(asyncQueue != null)
			asyncQueue.stop();
	}

	@Override
	protected void finalize() throws Throwable {
		asyncQueue.stop();
		super.finalize();

	}

	@Override
	public int read() throws IOException {
		return 0;
	}

	@Override
	public synchronized int  read(byte[] buffer, int offset, int length) throws IOException {
		int min = 0;

		/*try {
			if (mBuffer==null) {
				while (!Thread.interrupted() && !mClosed) {

					mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 500000);
					if (mIndex>=0 ){
						//Log.d(TAG,"Index: "+mIndex+" Time: "+mBufferInfo.presentationTimeUs+" size: "+mBufferInfo.size);
						mBuffer = mBuffers[mIndex];
						mBuffer.position(0);
						break;
					} else if (mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						mBuffers = mMediaCodec.getOutputBuffers();
					} else if (mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						mMediaFormat = mMediaCodec.getOutputFormat();
						Log.i(TAG,mMediaFormat.toString());
					} else if (mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
						Log.v(TAG,"No buffer available...");
						//return 0;
					} else {
						Log.e(TAG,"Message: "+mIndex);
						//return 0;
					}
				}			
			}
			
			if (mClosed) throw new IOException("This InputStream was closed");
			
			min = length < mBufferInfo.size - mBuffer.position() ? length : mBufferInfo.size - mBuffer.position(); 
			mBuffer.get(buffer, offset, min);

			if(asyncQueue == null){
				asyncQueue = new AsyncQueue();
				asyncQueue.setHandler(handler);
				asyncQueue.start();
			}
			H264File h264File = new H264File();
			h264File.data = buffer;
			h264File.offset = offset;
			h264File.length = min;
			asyncQueue.put(h264File);
			//System.arraycopy(buffer,offset,outBuff,0,min);
			//outputStream.write(outBuff, 0, min);

			if (mBuffer.position()>=mBufferInfo.size) {
				mMediaCodec.releaseOutputBuffer(mIndex, false);
				mBuffer = null;
			}


		} catch (RuntimeException e) {
			e.printStackTrace();
		}*/


		ByteBuffer outputBuffer = null;

		while (!Thread.interrupted() && !mClosed) {
			int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 100);
			if (encoderStatus >= 0) {

				outputBuffer = mMediaCodec.getOutputBuffer(encoderStatus);
				if (outputBuffer == null) {
					throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
				}
				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
					Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
					mBufferInfo.size = 0;
				}
				if (mBufferInfo.size != 0) {
					outputBuffer.position(mBufferInfo.offset);
					outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
					if (mBufferInfo.presentationTimeUs < 0) {
						mBufferInfo.presentationTimeUs = 0;
					}

					min = length < mBufferInfo.size - outputBuffer.position() ? length : mBufferInfo.size - outputBuffer.position();
					outputBuffer.get(buffer, offset, min);

					if(asyncQueue == null){
						asyncQueue = new AsyncQueue();
						asyncQueue.setHandler(handler);
						asyncQueue.start();
					}
					H264File h264File = new H264File();
					h264File.data = buffer;
					h264File.offset = offset;
					h264File.length = min;
					asyncQueue.put(h264File);

				}
				mMediaCodec.releaseOutputBuffer(encoderStatus, false);
				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.e(TAG, "BUFFER_FLAG_END_OF_STREAM...");
					break;
				}
		   }

		}



		Log.e(TAG,"MediaCodecInputStream return "+min);
		return min;
	}
	
	public int available() {
		if (mBuffer != null) 
			return mBufferInfo.size - mBuffer.position();
		else 
			return 0;
	}

	public BufferInfo getLastBufferInfo() {
		return mBufferInfo;
	}


}
