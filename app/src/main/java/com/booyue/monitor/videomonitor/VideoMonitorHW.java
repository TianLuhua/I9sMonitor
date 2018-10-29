package com.booyue.monitor.videomonitor;

import android.app.Service;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.booyue.monitor.BooyueVideoMonitorService;
import com.booyue.monitor.R;
import com.tencent.av.VideoController;
import com.tencent.av.mediacodec.surface2buffer.VideoEncoder;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Tianluhua on 2018\10\29 0029.
 */
public class VideoMonitorHW implements BooyueVideoMonitorService.IVideoMonitor {

    public static final String TAG = "VideoMonitorHW";

    private String mPeerId;
    private boolean mVideoConnected = false;
    private VideoEncoder mVideoEncoder = null;
    private Object mObjectMutex = new Object();
    private boolean mResetEncoder = false;

    private GLSurfaceView mGLSurfaceView;
    private SurfaceTexture mSurfaceTexture;
    private int mTextureID = -1;
    private Camera mCamera;

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";
    private static int encWidth = 640;
    private static int encHeight = 480;
    private static int encBitRate = 1000 * 350;
    private static int encFrameRate = 10;
    private static int encIFrameInterval = 5;

    @Override
    public void start(Service service, String peerId) {
        mPeerId = peerId;
        WindowManager mWindowManager = (WindowManager) service.getApplication()
                .getSystemService(service.getApplication().WINDOW_SERVICE);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = -100;
        wmParams.y = -100;
        wmParams.width = 0;
        wmParams.height = 0;

        LayoutInflater inflater = LayoutInflater.from(service.getApplication());
        LinearLayout mFloatLayout = (LinearLayout) inflater.inflate(R.layout.activity_videomonitor_hardcodec, null);
        mWindowManager.addView(mFloatLayout, wmParams);

        mGLSurfaceView = (GLSurfaceView) mFloatLayout.findViewById(R.id.camera_textureview_monitor);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        VideoController.getInstance().enableQosNotify(true);
    }

    @Override
    public void stop() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        synchronized (mObjectMutex) {
            if (mVideoEncoder != null) {
                mVideoEncoder.stopEncode();
                mVideoEncoder = null;
            }
        }

        VideoController.getInstance().enableQosNotify(false);
    }

    @Override
    public void resetEncoder(int width, int height, int bitrate, int fps) {
        if (encWidth != width || encHeight != height || encBitRate != bitrate || encFrameRate != fps) {
            encWidth = width;
            encHeight = height;
            encBitRate = bitrate;
            encFrameRate = fps;
            synchronized (mObjectMutex) {
                if (mVideoEncoder != null) {
                    mResetEncoder = true;
                }
            }
            mResetEncoder = true;
        }
    }

    @Override
    public void setVideoConnected(boolean videoConnected) {
        mVideoConnected = videoConnected;
    }

    private GLSurfaceView.Renderer mRender = new GLSurfaceView.Renderer() {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // TODO Auto-generated method stub
            mTextureID = VideoEncoder.createTextureID();
            mSurfaceTexture = new SurfaceTexture(mTextureID);
            mSurfaceTexture.setOnFrameAvailableListener(mFrameAvailableListener);
            openCamera(encWidth, encHeight);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onDrawFrame...");
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            mSurfaceTexture.updateTexImage();

            if (mVideoConnected == true) {
                synchronized (mObjectMutex) {
                    if (mVideoEncoder == null) {
                        mVideoEncoder = new VideoEncoder(Long.parseLong(mPeerId));
                        mVideoEncoder.prepareEncoder(MIME_TYPE, encWidth, encHeight, encBitRate, encFrameRate, encIFrameInterval);
                        mVideoEncoder.startEncoder(EGL14.eglGetCurrentContext(), mTextureID);
                    }

                    if (mResetEncoder) {
                        mVideoEncoder.stopEncode();
                        mVideoEncoder.prepareEncoder(MIME_TYPE, encWidth, encHeight, encBitRate, encFrameRate, encIFrameInterval);
                        mVideoEncoder.startEncoder(EGL14.eglGetCurrentContext(), mTextureID);
                        mResetEncoder = false;
                    }

                    mVideoEncoder.onFrameAvailable(mSurfaceTexture);
                }
            }
        }
    };

    private SurfaceTexture.OnFrameAvailableListener mFrameAvailableListener =
            new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture arg0) {
                    // TODO Auto-generated method stub
                    Log.i(TAG, "onFrameAvailable...");

                    mGLSurfaceView.requestRender();
                }
            };

    private void openCamera(int encWidth, int encHeight) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();
        choosePreviewSize(parms, encWidth, encHeight);
        chooseFrameRate(parms, encFrameRate * 1000);
        parms.setRecordingHint(true);
        mCamera.setParameters(parms);
        mCamera.setDisplayOrientation(0);
        Camera.Size size = parms.getPreviewSize();
        Log.d(TAG, "Camera preview size is " + size.width + "x" + size.height);

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Camera startPreview failed");
        }
    }

    private static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " + ppsfv.width + "x" + ppsfv.height);
        }

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
    }

    private static void chooseFrameRate(Camera.Parameters parms, int frameRate) {
        List<int[]> fpsRanges = parms.getSupportedPreviewFpsRange();
        for (int i = 0; i < fpsRanges.size(); ++i) {
            int[] range = fpsRanges.get(i);
            if (range != null) {
                int fpsMin = range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
                int fpsMax = range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
                if (fpsMin <= frameRate && frameRate <= fpsMax) {
                    parms.setPreviewFpsRange(fpsMin, fpsMax);
                    return;
                }
            }
        }

        if (fpsRanges.size() > 0) {
            int[] range = fpsRanges.get(0);
            if (range != null) {
                parms.setPreviewFpsRange(range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
                return;
            }
        }

        parms.setPreviewFpsRange(frameRate, frameRate);
    }
}
