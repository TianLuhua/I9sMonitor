package com.tencent.devicedemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.av.VideoController;
import com.tencent.av.camera.VcCamera;
import com.tencent.av.core.VideoConstants;
import com.tencent.av.opengl.GLVideoView;
import com.tencent.av.opengl.GraphicRenderMgr;
import com.tencent.av.opengl.ui.GLRootView;
import com.tencent.av.opengl.ui.GLView;
import com.tencent.av.opengl.ui.GLViewGroup;
import com.tencent.device.QLog;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;

import com.booyue.monitor.R;

import static com.tencent.av.VideoController.ACTION_VIDEO_QOS_NOTIFY;

public class VideoChatActivitySF extends Activity {
    private static final String TAG = "VideoChatActivitySF";
    String mPeerId;
    int mDinType;
    String mSelfDin;
    boolean mIsReceiver = false;
    boolean mVideoConnected = false;
    long mSwitchVideoIndex = 0;

    GLRootView mGlRootView;
    GLViewGroup mGlpanelView;
    GLVideoView mGlSmallVideoView;
    GLVideoView mGlBigVideoView;

    Button mAccept;
    Button mReject;
    Button mClose;
    Button mSwitch;

    TextView mLogInfo;
    VcCamera mCamera;

    BroadcastHandler mBroadcastHandler;
    private Button mSwitchDefinition;
    private int currentDefinition = 1;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.setContentView(R.layout.activity_videochat_softcodec);
        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent intent = super.getIntent();
        mPeerId = intent.getStringExtra("peerid");
        mDinType = intent.getIntExtra("dinType", VideoController.UINTYPE_QQ);
        mSelfDin = VideoController.getInstance().GetSelfDin();
        mIsReceiver = intent.getBooleanExtra("receive", false);
        if (Long.parseLong(mPeerId) == 0 || Long.parseLong(mSelfDin) == 0) {
            QLog.e(TAG, QLog.CLR, "invalid peerId: " + mPeerId + " invalid selfDin: " + mSelfDin);
            finish();
        }

        initQQGlView();

        initCameraPreview();

        mLogInfo = (TextView) findViewById(R.id.logInfo);
        mCamera = VideoController.getInstance().getCamera();

        mBroadcastHandler = new BroadcastHandler();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT);
        filter.addAction(VideoController.ACTION_NETMONIOTR_INFO);
        filter.addAction(ACTION_VIDEO_QOS_NOTIFY);
        filter.addAction(VideoController.ACTION_CHANNEL_READY);
        filter.addAction(VideoController.ACTION_NET_LEVEL);
        filter.addAction(VideoController.ACTION_NET_BAD);
        filter.addAction(TXDeviceService.BinderListChange);
        filter.addAction(TXDeviceService.OnEraseAllBinders);
        registerReceiver(mBroadcastHandler, filter);

        mAccept = (Button) findViewById(R.id.av_video_accept);
        mReject = (Button) findViewById(R.id.av_video_reject);
        mClose = (Button) findViewById(R.id.av_video_close);
        mSwitch = (Button) findViewById(R.id.av_video_switch);
        mSwitchDefinition = (Button) findViewById(R.id.av_video_switch_definition);
        mSwitchDefinition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDefinition == 1) {
                    currentDefinition = VideoController.DEFINITION_TYPE_CLEAR;
                    mSwitchDefinition.setText("优先流畅");
                } else {
                    currentDefinition = VideoController.DEFINITION_TYPE_FLUENT;
                    mSwitchDefinition.setText("优先高清");
                }
                Toast.makeText(VideoChatActivitySF.this, "正在切换", Toast.LENGTH_SHORT).show();
                VideoController.getInstance().setVideoModeType(Long.valueOf(mPeerId), currentDefinition);// 切换时间为0-2s
                mSwitchDefinition.setEnabled(false);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwitchDefinition.setEnabled(true);
                    }
                }, 2000);

            }
        });

        if (mIsReceiver) {
            mAccept.setVisibility(View.VISIBLE);
            mReject.setVisibility(View.VISIBLE);
        } else {
            mSwitch.setVisibility(View.VISIBLE);
            mSwitchDefinition.setVisibility(View.VISIBLE);
            mClose.setVisibility(View.VISIBLE);
            mClose.setText("取消");
            if (Long.parseLong(mPeerId) != 0) {
                VideoController.getInstance().request(mPeerId, mDinType);
            }
        }

        VideoController.getInstance().enableQosNotify(true);

        //startRing必须和stopRing成对调用（使用MediaPlayer）； startRing2必须和stopRing2成对调用（使用SoundPool）；
        if (mIsReceiver) {
            VideoController.getInstance().startRing(R.raw.qav_video_incoming, -1, null);
            //VideoController.getInstance().startRing2(R.raw.qav_video_incoming, -1);
        } else {
            VideoController.getInstance().startRing(R.raw.qav_video_request, -1, null);
            //VideoController.getInstance().startRing2(R.raw.qav_video_request, -1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");


    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");


    }

    void initQQGlView() {
        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, "initQQGlView");
        }

        mGlRootView = (GLRootView) findViewById(R.id.av_video_gl_root_view);

        mGlpanelView = new GLViewGroup(this);
        mGlRootView.setContentPane(mGlpanelView);

        //在这里设置背景
        mGlpanelView.setBackgroundColor(Color.DKGRAY);
        //mGlpanelView.setBackground(R.drawable.qav_video_bg_s);

        mGlBigVideoView = new GLVideoView(this);
        mGlpanelView.addView(mGlBigVideoView);
        mGlBigVideoView.setIsPC(false);
        mGlBigVideoView.enableLoading(false);
        mGlBigVideoView.setMirror(true);
        mGlBigVideoView.setNeedRenderVideo(true);
        mGlBigVideoView.setVisibility(GLView.VISIBLE);
        mGlBigVideoView.setScaleType(ScaleType.FIT_CENTER);// 可以避免裁剪导致的花屏问题。建议显示手Q传过来的视频的控件用这个
        mGlBigVideoView.setOnTouchListener(mTouchListener);
        //设置客人区背景
        mGlBigVideoView.setBackground(R.drawable.qav_video_bg_s);
        //设置边框颜色和边框宽度，不需要可以注释下面这两行代码
        mGlBigVideoView.setPaddingColor(Color.YELLOW);
        mGlBigVideoView.setPaddings(2, 2, 2, 2);

        mGlSmallVideoView = new GLVideoView(this);
        mGlpanelView.addView(mGlSmallVideoView);
        mGlSmallVideoView.setIsPC(false);
        mGlSmallVideoView.enableLoading(false);
        mGlSmallVideoView.setMirror(true);
        mGlSmallVideoView.setNeedRenderVideo(true);
        mGlSmallVideoView.setVisibility(GLView.VISIBLE);
        mGlSmallVideoView.setScaleType(ScaleType.CENTER_CROP);
        //设置主人区背景
        mGlSmallVideoView.setBackground(R.drawable.qav_video_bg_s);
        //设置边框颜色和边框宽度，不需要可以注释下面这两行代码
        mGlSmallVideoView.setPaddingColor(Color.WHITE);
        mGlSmallVideoView.setPaddings(2, 2, 2, 2);

        GraphicRenderMgr.getInstance().setGlRender(mSelfDin, mGlSmallVideoView.getYuvTexture());
        GraphicRenderMgr.getInstance().setGlRender(mPeerId, mGlBigVideoView.getYuvTexture());
    }

    private long mTimeClick = 0;
    private GLView.OnTouchListener mTouchListener = new GLView.OnTouchListener() {
        @Override
        public boolean onTouch(GLView view, MotionEvent event) {
            // TODO Auto-generated method stub
            long timeNow = System.currentTimeMillis();
            if (MotionEvent.ACTION_DOWN == event.getAction()) {
                if (timeNow - mTimeClick < 600) {
                    if (mLogInfo != null) {
                        mLogInfo.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (mLogInfo != null) {
                        mLogInfo.setVisibility(View.INVISIBLE);
                    }
                }
                mTimeClick = timeNow;
            }
            return true;
        }
    };

    // 这里对大小两个画面进行排布
    void layoutGlVideoView() {
//		int width  = mGlRootView.getWidth();
//		int height = mGlRootView.getHeight();
//
//		int margin = 30;
//		int widthBigVideo 	= (width * 2 / 3 - margin * 2);
//		int widthSmallVideo 	= (width * 1 / 3 - margin * 2);
//
//		int heightBigVideo	= (height - margin * 4);
//		int heightSmallVideo = (height * 2 / 3 - margin * 2);
//
//		//layout的四个参数定义： public void layout(int left, int top, int right, int bottom)
//		mGlBigVideoView.layout(margin, margin, margin + widthBigVideo, margin + heightBigVideo);
//		mGlBigVideoView.invalidate();
//
//		//layout的四个参数定义： public void layout(int left, int top, int right, int bottom)
//		mGlSmallVideoView.layout(width * 2 / 3 + margin, (height - heightSmallVideo) / 2, width - margin, (height - heightSmallVideo) / 2 + heightSmallVideo);
//		mGlSmallVideoView.invalidate();

        int width = mGlRootView.getWidth();

        //layout的四个参数定义： public void layout(int left, int top, int right, int bottom)
        mGlBigVideoView.layout(0, 0, width / 2, width / 2);
        mGlBigVideoView.invalidate();

        //layout的四个参数定义： public void layout(int left, int top, int right, int bottom)
        mGlSmallVideoView.layout(width / 2, 0, width, width / 2);
        mGlSmallVideoView.invalidate();
    }

    void initCameraPreview() {
        SurfaceView localVideo = (SurfaceView) findViewById(R.id.av_video_surfaceView);
        SurfaceHolder holder = localVideo.getHolder();
        holder.addCallback(mSurfaceHolderListener);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        localVideo.setZOrderMediaOverlay(true);
    }

    void locateCameraPreview() {
        SurfaceView localVideo = (SurfaceView) findViewById(R.id.av_video_surfaceView);
        if (localVideo != null) {
            MarginLayoutParams params = (MarginLayoutParams) localVideo.getLayoutParams();
            params.leftMargin = -3000;
            localVideo.setLayoutParams(params);
        }
    }

    SurfaceHolder.Callback mSurfaceHolderListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "surfaceCreated");
            }
            locateCameraPreview();
            layoutGlVideoView();
            VideoController.getInstance().execute(openCamera, null);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "surfaceChanged");
            }
            if (holder.getSurface() == null) {
                return;
            }
            holder.setFixedSize(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "surfaceDestroyed");
            }
        }
    };

    // 恢复视频
    Runnable resumeVideo = new Runnable() {
        @Override
        public void run() {
            if (mPeerId != null && mPeerId.length() > 0) {
                VideoController.getInstance().resumeVideo(mPeerId);
            }
        }
    };

    // 暂停视频
    Runnable pauseVideo = new Runnable() {
        @Override
        public void run() {
            if (mPeerId != null && mPeerId.length() > 0) {
                VideoController.getInstance().pauseVideo(mPeerId);
            }
        }
    };

    Runnable openCamera = new Runnable() {
        @Override
        public void run() {
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "resumeCamera begin.");
            }

            SurfaceView localVideo = (SurfaceView) findViewById(R.id.av_video_surfaceView);
            SurfaceHolder holder = localVideo.getHolder();

            VideoController.getInstance().execute(new AsyncOpenCamera(holder));

            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "resumeCamera end.");
            }
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
                if (QLog.isColorLevel()) {
                    QLog.d(TAG, QLog.CLR, "asyncOpenCamera start.");
                }
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
                if (QLog.isColorLevel()) {
                    QLog.d(TAG, QLog.CLR, "asyncOpenCamera end.");
                }
            } catch (Exception e) {
                if (QLog.isColorLevel()) {
                    QLog.d(TAG, QLog.CLR, "asyncOpenCamera", e);
                }
            }
        }
    }

    Runnable closeCamera = new Runnable() {
        @Override
        public void run() {
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "closeCamera begin.");
            }
            if (mCamera != null) {
                mCamera.closeCamera();
            }
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "closeCamera end.");
            }
        }
    };

    @Override
    protected void onDestroy() {
        VideoController.getInstance().execute(closeCamera, null);
        VideoController.getInstance().stopRing();

        if (mIsReceiver) {
            if (mVideoConnected) {
                VideoController.getInstance().closeVideo(mPeerId);
            } else {
                VideoController.getInstance().rejectRequest(mPeerId);
            }
        } else {
            VideoController.getInstance().closeVideo(mPeerId);
        }

        GraphicRenderMgr.getInstance().setGlRender(mPeerId, null);
        GraphicRenderMgr.getInstance().setGlRender(mSelfDin, null);


        super.unregisterReceiver(mBroadcastHandler);

        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess();
        }
    }

    @Override
    public void finish() {
        Log.i(TAG, "finish");
        super.finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                showQuitDialog();
                return true;
            case KeyEvent.KEYCODE_SEARCH:
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                break;
            case KeyEvent.KEYCODE_MENU:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    void showQuitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("确认退出吗?");
        builder.setTitle("提示");
        builder.setPositiveButton("确认", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                VideoChatActivitySF.this.finish();
            }
        });

        builder.setNegativeButton("取消", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    class BroadcastHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(VideoConstants.ACTION_STOP_VIDEO_CHAT)) {
                int reason = intent.getIntExtra("reason", VideoConstants.VOIP_REASON_OTHERS);
                if (reason == VideoConstants.VOIP_REASON_REJECT_BY_FRIEND) {
                    //发起视频请求之后，对方拒绝
                } else if (reason == VideoConstants.VOIP_REASON_SELF_WAIT_RELAYINFO_TIMEOUT) {
                    //发起视频请求之后，对方一直不接听，最后超时
                } else if (reason == VideoConstants.VOIP_REASON_CLOSED_BY_FRIEND) {
                    //连通之后，对方主动关闭
                } else {
                    //其它原因
                }

                Log.d(TAG, "recv broadcast : AVSessionClose reason = " + reason);
                finish();
            } else if (intent.getAction().equalsIgnoreCase(VideoController.ACTION_NETMONIOTR_INFO)) {
                String msg = intent.getStringExtra("msg");
                if (mLogInfo != null) {
                    mLogInfo.setText(msg);
                }
            } else if (intent.getAction().equalsIgnoreCase(VideoController.ACTION_CHANNEL_READY)) {
                VideoController.getInstance().stopRing();
                VideoController.getInstance().stopShake();
                VideoController.getInstance().startShake();
                mVideoConnected = true;
                mClose.setText("关闭");
            } else if (intent.getAction() == TXDeviceService.BinderListChange) {
                boolean bFind = false;
                Parcelable[] listBinder = intent.getExtras().getParcelableArray("binderlist");
                for (int i = 0; i < listBinder.length; ++i) {
                    TXBinderInfo binder = (TXBinderInfo) (listBinder[i]);
                    if (binder.tinyid == Long.parseLong(mPeerId)) {
                        bFind = true;
                        break;
                    }
                }
                if (bFind == false) {
                    finish();
                }
            } else if (intent.getAction() == TXDeviceService.OnEraseAllBinders) {
                finish();
            } else if (intent.getAction().equals(ACTION_VIDEO_QOS_NOTIFY)) {
                int width = intent.getIntExtra("width", VcCamera.PREVIEW_WIDTH);
                int height = intent.getIntExtra("height", VcCamera.PREVIEW_HEIGHT);
                if (width != VcCamera.PREVIEW_WIDTH || height != VcCamera.PREVIEW_HEIGHT) {
                    // 需要更新采集的分辨率，重启摄像头
                    VcCamera.PREVIEW_WIDTH = width;
                    VcCamera.PREVIEW_HEIGHT = height;
                    VideoController.getInstance().execute(closeCamera, null);
                    VideoController.getInstance().execute(openCamera, null);
                }
            } else if (intent.getAction().equals(VideoController.ACTION_NET_LEVEL)) {
                // 双方网络分级selfNetLevel和peerNetLevel表示自己和对方的网络状况，1-3依次为好，一般，差
                String tips = intent.getStringExtra("displayNetState");
                if (tips != null) {
                    Toast.makeText(context, tips, Toast.LENGTH_SHORT).show();
                }
            } else if (intent.getAction().equals(VideoController.ACTION_NET_BAD)) {
                // 任意一方网络不太好，每3s计算一次
                if (currentDefinition == VideoController.DEFINITION_TYPE_CLEAR) {
                    Toast.makeText(context, "网络环境较差，请切换到流畅试试", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    interface QQGLRenderListenerType {
        final static int PEER = 0;
        final static int LOCAL = 1;
    }

    public void onBtnClose(View view) {
        finish();
    }

    public void onBtnReject(View view) {
        finish();
    }

    public void onBtnAccept(View view) {
        VideoController.getInstance().stopRing();

        if (Long.parseLong(mPeerId) != 0) {
            VideoController.getInstance().acceptRequest(mPeerId);
            mAccept.setVisibility(View.GONE);
            mReject.setVisibility(View.GONE);
            mClose.setVisibility(View.VISIBLE);
            mSwitch.setVisibility(View.VISIBLE);
            mSwitchDefinition.setVisibility(View.VISIBLE);
        }
    }

    public void onBtnSwitchVideo(View view) {
//		GraphicRenderMgr.getInstance().setGlRender(mSelfDin, null);
//		mGlRootView.requestRenderForced();	
        mSwitchVideoIndex++;
        String key1 = mPeerId;
        String key2 = mSelfDin;

        if (mSwitchVideoIndex % 2 == 0) {
            GraphicRenderMgr.getInstance().setGlRender(key1, mGlBigVideoView.getYuvTexture());
            GraphicRenderMgr.getInstance().setGlRender(key2, mGlSmallVideoView.getYuvTexture());
        } else {
            GraphicRenderMgr.getInstance().setGlRender(key2, mGlBigVideoView.getYuvTexture());
            GraphicRenderMgr.getInstance().setGlRender(key1, mGlSmallVideoView.getYuvTexture());
        }
    }

    public void onBtnScanDevice(View view) {

    }
}