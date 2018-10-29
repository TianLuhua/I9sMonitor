package com.booyue.monitor.videomonitor;

import android.app.Service;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.booyue.monitor.BooyueVideoMonitorService;
import com.booyue.monitor.R;
import com.tencent.av.VideoController;
import com.tencent.av.camera.VcCamera;
import com.tencent.av.opengl.GraphicRenderMgr;
import com.tencent.device.QLog;

/**
 * Created by Tianluhua on 2018\10\29 0029.
 */
public class VideoMonitorSF implements BooyueVideoMonitorService.IVideoMonitor {

    public static final String TAG = "VideoMonitorSF";

    private VcCamera mCamera;
    private SurfaceView mSurfaceView;


    @Override
    public void start(Service service, String peerId) {
        WindowManager mWindowManager = (WindowManager) service.getApplication()
                .getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;

        LayoutInflater inflater = LayoutInflater.from(service.getApplication());
        View rootView = inflater.inflate(R.layout.activity_videomonitor_softcodec, null);
        mWindowManager.addView(rootView, wmParams);

        mCamera = VideoController.getInstance().getCamera();

        mSurfaceView = rootView.findViewById(R.id.av_video_surfaceView);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(mSurfaceHolderListener);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.setZOrderMediaOverlay(true);

    }


    @Override
    public void stop() {
        VideoController.getInstance().execute(closeCamera, null);
        GraphicRenderMgr.getInstance().setGlRender(VideoController.getInstance().GetSelfDin(), null);
    }

    @Override
    public void resetEncoder(int width, int height, int bitrate, int fps) {

    }

    @Override
    public void setVideoConnected(boolean videoConnected) {

    }

    SurfaceHolder.Callback mSurfaceHolderListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            VideoController.getInstance().execute(openCamera, null);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            holder.setFixedSize(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    Runnable openCamera = new Runnable() {
        @Override
        public void run() {
            SurfaceHolder holder = mSurfaceView.getHolder();
            VideoController.getInstance().execute(new AsyncOpenCamera(holder));
        }
    };

    class AsyncOpenCamera implements Runnable {
        SurfaceHolder mHolder;

        public AsyncOpenCamera(SurfaceHolder holder) {
            mHolder = holder;
        }

        @Override
        public void run() {
            try {
                if (mCamera == null || !mCamera.openCamera(mHolder)) {
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera failed to start camera.");
                    }
                    return;
                } else {
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera success.");
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    Runnable closeCamera = new Runnable() {
        @Override
        public void run() {
            if (mCamera != null) {
                mCamera.closeCamera();
            }
        }
    };
}
