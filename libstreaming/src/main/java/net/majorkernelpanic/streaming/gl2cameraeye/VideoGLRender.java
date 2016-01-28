package net.majorkernelpanic.streaming.gl2cameraeye;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class VideoGLRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

	public interface OnFrameListener {
		public void onFrame(byte[] data, int data_size);
	}

	private Context mContext;
	private OnFrameListener mFrameListener;
	private Camera mCamera;
	private GLSurfaceView mGlSurfaceView;
	private int mWidth;
	private int mHeight;

	private boolean mStatusOk;
	private boolean mInitialized = false;

	private ByteBuffer mPixelBuf;
	private long mPreviousTimestamp = 0;

	private int[] mGlTextures = null;
	private int mCaptureTextureID;
	private int mRenderTextureID;
	private SurfaceTexture mCaptureSurfaceTexture;
	private int[] mFramebuffer = null;

	private int mProgram;
	private float[] mMVPMatrix = new float[16];
	private float[] mProjMatrix = new float[16];
	private float[] mVMatrix = new float[16];
	private float[] mSTMatrix = new float[16];
	private float[] mRotationMatrix = new float[16];

	private int muMVPMatrixHandle;
	private int muSTMatrixHandle;
	private int muCRatioHandle;
	private int maPositionHandle;
	private int maTextureHandle;
	private float[] mPos = new float[3];

	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
	private FloatBuffer mTriangleVertices;
    private final float[] mTriangleVerticesData = {
            // X ---- Y- Z -- U -- V
           -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
           -1.0f,  1.0f, 0, 0.f, 1.f,
            1.0f,  1.0f, 0, 1.f, 1.f,
    };

   /* 
    private final float[] mTriangleVerticesData = {
            // X ---- Y- Z -- U -- V
            -1.0f, -1.0f, 0, 0.f, 0.f,
             1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f,  1.0f, 0, 0.f, 1.f,
             1.0f,  1.0f, 0, 1.f, 1.f,
    };*/

    private static final String mVertexShader =
        "uniform mat4 uMVPMatrix;\n" +
        "uniform mat4 uSTMatrix;\n" +
        "uniform float uCRatio;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec4 aTextureCoord;\n" +
        "varying vec2 vTextureCoord;\n" +
        "void main() {\n" +
        "  vec4 scaledPos = aPosition;\n" +
        "  scaledPos.x = scaledPos.x * uCRatio;\n" +
        "  gl_Position = uMVPMatrix * scaledPos;\n" +
        "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
        "}\n";
    private static final String mFragmentShader =
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform samplerExternalOES sTexture;\n" +
        "void main() {\n" +
        "  gl_FragColor = texture2D(sTexture, vTextureCoord).rgba;\n" +  // (GL2CameraEye)
        "}\n";

    
	private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
	private static final String TAG = "VideoCaptureGlRender";

	public VideoGLRender(Context context, OnFrameListener frameListener, Camera camera, GLSurfaceView glSurfaceView,
			int width, int height, int renderTextureID) {
		Log.d(TAG, "constructor, using " + ((mRenderTextureID == -1) ? "Pbuffer" : "FBO"));
		mContext = context; // Needed for getting device orientation.
		mFrameListener = frameListener;
		mCamera = camera;
		mGlSurfaceView = glSurfaceView;
		mWidth = width;
		mHeight = height;
		mRenderTextureID = renderTextureID;
		mStatusOk = true;
		mInitialized = false;
	}

	public boolean shutdown() {
		GLES20.glDeleteTextures(1, mGlTextures, 0);
		if (mRenderTextureID != -1)
			deleteFrameBufferObject();
		// TODO delete fbo texture
		mCaptureSurfaceTexture.setOnFrameAvailableListener(null);
		return true;
	}

	public boolean getStatusOk() {
		return mStatusOk;
	}

	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		mGlSurfaceView.requestRender();
	}

	@Override
	public void onSurfaceCreated(GL10 gl_unused, EGLConfig config) {
		Log.d(TAG, "onSurfaceCreated");
		mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTriangleVertices.put(mTriangleVerticesData).position(0);

		Matrix.setIdentityM(mSTMatrix, 0);

		mPos[0] = 0.f;
		mPos[1] = 0.f;
		mPos[2] = 0.f;

		GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glClearColor(0.643f, 0.776f, 0.223f, 1.0f);
		GLES20.glClearDepthf(1.0f);

		// Set up shaders and handles to their variables.
		mProgram = createProgram(mVertexShader, mFragmentShader);
		if (mProgram == 0) {
			mStatusOk = false;
			return;
		}

		maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
		dumpGlError("glGetAttribLocation aPosition");
		if (maPositionHandle == -1) {
			mStatusOk = false;
			return;
		}
		maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
		dumpGlError("glGetAttribLocation aTextureCoord");
		if (maTextureHandle == -1) {
			mStatusOk = false;
			return;
		}

		muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		dumpGlError("glGetUniformLocation uMVPMatrix");
		if (muMVPMatrixHandle == -1) {
			mStatusOk = false;
			return;
		}
		muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
		dumpGlError("glGetUniformLocation uSTMatrix");
		if (muSTMatrixHandle == -1) {
			mStatusOk = false;
			return;
		}
		muCRatioHandle = GLES20.glGetUniformLocation(mProgram, "uCRatio");
		dumpGlError("glGetUniformLocation uCRatio");
		if (muCRatioHandle == -1) {
			mStatusOk = false;
			return;
		}

		Matrix.setLookAtM(mVMatrix, 0, 0, 0, 5f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

		GLES20.glViewport(0, 0, mWidth, mHeight);
		int ratio = (int) ((float) mWidth / (float) mHeight);
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

		if (mRenderTextureID != -1) {
			if (!createFramebufferObjectTexture(mRenderTextureID) || !createFramebufferObject(mRenderTextureID)) {
				mStatusOk = false;
				return;
			}
		}

		if (!createVideoCaptureSurfaceTexture()) {
			Log.e(TAG, " onSurfaceCreated: Capture surface texture failed");
			mStatusOk = false;
			return;
		}

		mCaptureSurfaceTexture.setOnFrameAvailableListener(this);
		try {
			mCamera.setPreviewTexture(mCaptureSurfaceTexture);
		} catch (IOException ex) {
			Log.e(TAG, " setPreviewTexture: " + ex);
			mStatusOk = false;
			return;
		}

		mPixelBuf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
		// Get a Handler to the current, rendering, EGL context owning thread,
		// to post tasks from onFrameAvailable() later on.
		Looper.prepare();
		// mGLThreadHandler = new Handler();
		Log.d(TAG, "onSurfaceCreated finished.");
		mInitialized = true;
	}

	@Override
	public void onSurfaceChanged(GL10 gl_unused, int width, int height) {
//		Log.d(TAG, "onSurfaceChanged: (" + width + "x" + height + ")");
//		GLES20.glViewport(0, 0, width, height);
//		float ratio = ((float)width  / (float)height );
		Log.d(TAG, "onSurfaceChanged: (" + width + "x" + height + ")");
		GLES20.glViewport(0, 0, width, height);
		float ratio = ((float)height  / (float) width);
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onDrawFrame(GL10 gl_unused) {
		if (!mInitialized) {
			return;
		}
		long timestamp = mCaptureSurfaceTexture.getTimestamp();
		Log.d(TAG, "frame received, updating texture, fps~="
				+ ((timestamp - mPreviousTimestamp != 0) ? (1000000000L / (timestamp - mPreviousTimestamp)) : 0L));
		mPreviousTimestamp = timestamp;

		if (mRenderTextureID != -1) {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer[0]);
			dumpGlError("glBindFramebuffer");

			GLES20.glActiveTexture(mRenderTextureID);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRenderTextureID);
		}

		mCaptureSurfaceTexture.updateTexImage();
		mCaptureSurfaceTexture.getTransformMatrix(mSTMatrix);

		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glUseProgram(mProgram);
		dumpGlError("glUseProgram");

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mCaptureTextureID);

		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
				mTriangleVertices);
		dumpGlError("glVertexAttribPointer maPosition");
		GLES20.glEnableVertexAttribArray(maPositionHandle);
		dumpGlError("glEnableVertexAttribArray maPositionHandle");

		mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
		GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
				mTriangleVertices);
		dumpGlError("glVertexAttribPointer maTextureHandle");
		GLES20.glEnableVertexAttribArray(maTextureHandle);
		dumpGlError("glEnableVertexAttribArray maTextureHandle");

		// Create a rotation for the geometry. The camera orientation in -90 deg
		// out of phase with what Android considers 0 deg.
		int orientation = getDeviceOrientation() - 90;
	//	Matrix.setRotateM(mRotationMatrix, 0, orientation, 0, 0, 1.0f);
		Matrix.setRotateM(mRotationMatrix, 0, orientation, 0, 0, 1.0f);

		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mRotationMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mVMatrix, 0);

		GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
		GLES20.glUniform1f(muCRatioHandle, (float) mWidth / mHeight);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		dumpGlError("glDrawArrays");

		// Retrieve the pixels and dump the approximate elapsed time.
		long timeBeforeGlReadPixels = SystemClock.elapsedRealtimeNanos();
		GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);
		mPixelBuf.rewind();
		long timeAfterGlReadPixels = SystemClock.elapsedRealtimeNanos();
		long elapsed_time = (timeAfterGlReadPixels - timeBeforeGlReadPixels) / 1000000;
//		Log.d(TAG, "glReadPixels elapsed time :" + elapsed_time + "ms");

		mFrameListener.onFrame(mPixelBuf.array(), mWidth * mHeight * 4);
	}

	// Create and allocate the special texture id and associated SurfaceTexture
	// for video capture. Special means type EXTERNAL_OES.
	private boolean createVideoCaptureSurfaceTexture() {
		mGlTextures = new int[1];
		GLES20.glGenTextures(1, mGlTextures, 0);
		mCaptureTextureID = mGlTextures[0];

		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mGlTextures[0]);
		dumpGlError("glBindTextures");
		GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

		mCaptureSurfaceTexture = new SurfaceTexture(mCaptureTextureID);
		return true;
	}

	private boolean createFramebufferObjectTexture(int renderTextureID) {
		Log.d(TAG, "createFramebufferObjectTexture, id:" + renderTextureID);
		// Create and allocate a normal texture, that will be used to render the
		// capture texture id onto. This is a hack but there's an explanation in
		// createEGLContext().
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTextureID);
		GLES20.glHint(renderTextureID, GLES20.GL_FASTEST);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		// The width and height must be a power of 2, and can be different.
		int width_texels = Integer.highestOneBit(mWidth) << 1;
		int height_texels = Integer.highestOneBit(mHeight) << 1;
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width_texels, height_texels, 0, GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE, null);

		return true;
	}

	private void deleteFrameBufferObjectTexture() {
		int[] textures = new int[1];
		textures[0] = mRenderTextureID;
		GLES20.glDeleteTextures(1, textures, 0);
	}

	private boolean createFramebufferObject(int renderTextureID) {
		Log.d(TAG, "createFramebufferObject, tex id:" + renderTextureID);

		mFramebuffer = new int[1];
		GLES20.glGenFramebuffers(1, mFramebuffer, 0);
		dumpGlError("glGenFramebuffers");
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer[0]);
		dumpGlError("glBindFramebuffer");
		// Qualcomm recommends clear after glBindFrameBuffer().
		GLES20.glClearColor(0.643f, 0.776f, 0.223f, 1.0f);
		GLES20.glClearDepthf(1.0f);

		// //////////////////////////////////////////////////////////////////////
		// Bind the texture to the generated Framebuffer Object.
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
				renderTextureID, 0);
		dumpGlError("glFramebufferTexture2D");
		if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			Log.e(TAG, " Created FBO and attached to texture");
		} else {
			Log.d(TAG, " FBO created and attached to texture.");
		}

		return true;
	}

	private void deleteFrameBufferObject() {
		GLES20.glDeleteFramebuffers(1, mFramebuffer, 0);
	}

	private int createProgram(String vertexSource, String fragmentSource) {
		Log.d(TAG, "createProgram");
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0)
			return 0;
		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if (pixelShader == 0)
			return 0;

		int program = GLES20.glCreateProgram();
		if (program != 0) {
			GLES20.glAttachShader(program, vertexShader);
			dumpGlError("glAttachShader");
			GLES20.glAttachShader(program, pixelShader);
			dumpGlError("glAttachShader");
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				Log.e(TAG, "Could not link program: ");
				Log.e(TAG, GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			} else {
				Log.d(TAG, GLES20.glGetProgramInfoLog(program));
			}
		}
		return program;
	}

	private int loadShader(int shaderType, String source) {
		Log.d(TAG, "loadShader " + shaderType);
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				Log.e(TAG, "Could not compile shader " + shaderType + ":");
				Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			} else {
				Log.d(TAG, GLES20.glGetShaderInfoLog(shader));
			}
		} else {
			Log.e(TAG, "Could not create shader " + shaderType + ":");
		}
		return shader;
	}

	public int getDeviceOrientation() {
		int orientation = 0;
		if (mContext != null) {
			WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			switch (wm.getDefaultDisplay().getRotation()) {
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

	private void dumpGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
			Log.e(TAG, "** " + op + ": glError " + error);
	}

}