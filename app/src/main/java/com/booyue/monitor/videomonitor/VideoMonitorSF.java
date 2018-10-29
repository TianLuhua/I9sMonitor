package com.booyue.monitor.videomonitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.booyue.monitor.BooyueVideoMonitorService;
import com.booyue.videochat.BooyueVideoChatActivitySF;
import com.tencent.av.VideoController;
import com.tencent.av.camera.VcCamera;
import com.tencent.device.QLog;
import com.tencent.device.TXDeviceService;
import com.tencent.util.LoggerUtils;

/**
 * Created by Tianluhua on 2018\10\29 0029.
 */
public class VideoMonitorSF implements BooyueVideoMonitorService.IVideoMonitor {

    public static String TAG = "VideoMonitorSF";

    private VcCamera mCamera;
    private String mPeerId;
    private boolean mIsReceiver;

    private Handler mHandler = new Handler();
    private int counter = 0;
    private Context mContext;

    private SurfaceView mSurfaceView;

    @Override
    public void start(Service service, String peerId) {
//        WindowManager mWindowManager = (WindowManager) service.getApplication()
//                .getSystemService(Context.WINDOW_SERVICE);
//        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
//        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
//        wmParams.format = PixelFormat.RGBA_8888;
//        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
//        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
//        wmParams.x = -100;
//        wmParams.y = -100;
//        wmParams.width = 0;
//        wmParams.height = 0;
//
//        LayoutInflater inflater = LayoutInflater.from(service.getApplication());
//        LinearLayout mFloatLayout = (LinearLayout) inflater.inflate(R.layout.activity_videomonitor_softcodec, null);
//        mWindowManager.addView(mFloatLayout, wmParams);
        this.mContext = service.getApplicationContext();
        this.mPeerId = peerId;
        mIsReceiver = true;
        mCamera = VideoController.getInstance().getCamera();
//
//        mSurfaceView = (SurfaceView) mFloatLayout.findViewById(R.id.camera_surfaceView_monitor);
//        SurfaceHolder holder = mSurfaceView.getHolder();
//        holder.addCallback(mSurfaceHolderListener);
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        mSurfaceView.setZOrderMediaOverlay(true);
//
//        GLRootView rootView = (GLRootView) mFloatLayout.findViewById(R.id.av_video_gl_root_view_monitor);
//        GLVideoView glPeerVideoView = new GLVideoView(service);
//        rootView.setContentPane(glPeerVideoView);
//        glPeerVideoView.setIsPC(false);
//        glPeerVideoView.enableLoading(false);
//        glPeerVideoView.setMirror(true);
//        glPeerVideoView.setNeedRenderVideo(true);
//        glPeerVideoView.setVisibility(GLView.VISIBLE);
//        glPeerVideoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//        glPeerVideoView.setBackground(R.drawable.qav_video_bg_s);
//
//        GraphicRenderMgr.getInstance().setGlRender(VideoController.getInstance().GetSelfDin(), glPeerVideoView.getYuvTexture());
        VideoController.getInstance().execute(openCamera, null);
    }

    @Override
    public void stop() {
        terminateVideo();
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
            VideoController.getInstance().execute(new AsyncOpenCamera());
        }
    };

    class AsyncOpenCamera implements Runnable {

        @Override
        public void run() {
            try {
                if (mCamera == null || !mCamera.openCameraWithSilent()) {
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera failed to start camera.");
                    }
                    return;
                } else {
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera success.");
                    }
                    mHandler.post(updateTime);
                }
            } catch (Exception e) {
                LoggerUtils.e("AsyncOpenCamera  fail，" + e.getMessage());
            }
        }
    }


    private void terminateVideo() {
        VideoController.getInstance().execute(closeCamera, null);
        VideoController.getInstance().stopRing();
        if (mIsReceiver) {
            VideoController.getInstance().closeVideo(mPeerId);
        }
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess();
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


    Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            counter++;
            if (counter == 5) {
                LoggerUtils.e("counter:" + counter);
                mHandler.removeCallbacks(updateTime);
                Intent intent = new Intent(mContext, BooyueVideoChatActivitySF.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("receive", true);
                intent.putExtra("peerid", mPeerId);
                mContext.startActivity(intent);
                return;
            }
            mHandler.postDelayed(updateTime, 1000);
        }
    };


}
