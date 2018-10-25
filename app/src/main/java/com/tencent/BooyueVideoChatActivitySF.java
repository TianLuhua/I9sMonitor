package com.tencent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
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
import com.tencent.device.FriendInfo;
import com.tencent.device.QLog;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;
import com.booyue.monitor.R;
import com.tencent.util.LoggerUtils;
import com.tencent.util.TimeUtils;

import static android.media.AudioManager.ADJUST_MUTE;
import static android.media.AudioManager.ADJUST_UNMUTE;
import static com.tencent.av.VideoController.ACTION_VIDEO_QOS_NOTIFY;

/**
 * 视频聊天页面
 */

public class BooyueVideoChatActivitySF extends Activity {
    private static final String TAG = "BooyueVideoChatActivity";
    String mPeerId;
    int mDinType;
    String mSelfDin;
    boolean mIsReceiver = false;
    boolean mVideoConnected = false;
    long mSwitchVideoIndex = 0;//切换对方页面和自己页面
    long mSwitchCameraIndex = 0;//关闭camera

    GLRootView mGlRootView;
    GLViewGroup mGlpanelView;
    GLVideoView mGlSmallVideoView;
    GLVideoView mGlBigVideoView;

    Button mAccept;
    Button mReject;
    Button mClose;
    Button mSwitch;

    //    TextView mLogInfo;
    VcCamera mCamera;

    BroadcastHandler mBroadcastHandler;
    private Button mSwitchDefinition;
    private int currentDefinition = 1;

    private Handler mHandler = new Handler();
    private ImageButton ibSwitchDefinition;
    private ImageButton ibSwitchCamera;
    private ImageButton ibSwitchVoice;
    private ImageButton ibHangup;
    private TextView tvTime;
    private long startTime;
    private ImageButton ibReceive;
    private ImageButton ibRefuse;
    private TextView tvName;
    private LinearLayout llReceiveBefore;
    private LinearLayout llReceiveAfter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.setContentView(R.layout.activity_videochat_softcodec_booyue);
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

//        mLogInfo = (TextView) findViewById(R.id.logInfo);
        mCamera = VideoController.getInstance().getCamera();
        initView();

//        mAccept = (Button) findViewById(R.id.av_video_accept);
//        mReject = (Button) findViewById(R.id.av_video_reject);
//        mClose = (Button) findViewById(R.id.av_video_close);
//        mSwitch = (Button) findViewById(R.id.av_video_switch);
//        mSwitchDefinition = (Button) findViewById(R.id.av_video_switch_definition);
//        mClose.setVisibility(View.GONE);
//        mSwitch.setVisibility(View.GONE);
//        mSwitchDefinition.setVisibility(View.GONE);
//        /**
//         * 清晰度切换
//         */
//        mSwitchDefinition.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (currentDefinition == 1) {
//                    currentDefinition = VideoController.DEFINITION_TYPE_CLEAR;
//                    mSwitchDefinition.setText(R.string.smooth_first);
//                } else {
//                    currentDefinition = VideoController.DEFINITION_TYPE_FLUENT;
//                    mSwitchDefinition.setText(R.string.definition_first);
//                }
//                Toast.makeText(BooyueVideoChatActivitySF.this, R.string.switching, Toast.LENGTH_SHORT).show();
//                VideoController.getInstance().setVideoModeType(Long.valueOf(mPeerId), currentDefinition);// 切换时间为0-2s
//                mSwitchDefinition.setEnabled(false);
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mSwitchDefinition.setEnabled(true);
//                    }
//                }, 2000);
//
//            }
//        });

        if (mIsReceiver) {
//            mAccept.setVisibility(View.VISIBLE);
//            mReject.setVisibility(View.VISIBLE);
            beforeReceive();
        } else {
//            mSwitch.setVisibility(View.VISIBLE);
//            mSwitchDefinition.setVisibility(View.VISIBLE);
//            mClose.setVisibility(View.VISIBLE);
//            mClose.setText(R.string.cancel);
            afterReceive();
            tvTime.setVisibility(View.GONE);
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

    private void initView() {
        llReceiveBefore = (LinearLayout) findViewById(R.id.ll_recieve_before);
        llReceiveAfter = (LinearLayout) findViewById(R.id.ll_receive_after);

        //接听
        ibReceive = (ImageButton) findViewById(R.id.btn_receive);
        //拒绝
        ibRefuse = (ImageButton) findViewById(R.id.btn_refuse);
        //对方的名字
        tvName = (TextView) findViewById(R.id.tv_name);
        final FriendInfo friendInfo = VideoController.getInstance().getFriendInfo(mPeerId);
        tvName.setText(friendInfo.getName());


        ibReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoController.getInstance().stopRing();
                if (Long.parseLong(mPeerId) != 0) {
                    VideoController.getInstance().acceptRequest(mPeerId);
//                    mAccept.setVisibility(View.GONE);
//                    mReject.setVisibility(View.GONE);
//                    mClose.setVisibility(View.VISIBLE);
//                    mSwitch.setVisibility(View.VISIBLE);
//                    mSwitchDefinition.setVisibility(View.VISIBLE);
                    afterReceive();
                }
            }
        });

        ibRefuse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ibSwitchDefinition = (ImageButton) findViewById(R.id.ib_speaker_switcher);
        ibSwitchCamera = (ImageButton) findViewById(R.id.ib_switch_camera);
        ibSwitchVoice = (ImageButton) findViewById(R.id.ib_switch_voice);
        ibHangup = (ImageButton) findViewById(R.id.ib_hangup);
        tvTime = (TextView) findViewById(R.id.tv_time);
        ibSwitchDefinition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDefinition == 1) {
                    currentDefinition = VideoController.DEFINITION_TYPE_CLEAR;
                    ibSwitchDefinition.setImageResource(R.drawable.button_smooth);
                } else {
                    currentDefinition = VideoController.DEFINITION_TYPE_FLUENT;
                    ibSwitchDefinition.setImageResource(R.drawable.button_hd);
                }
                Toast.makeText(BooyueVideoChatActivitySF.this, R.string.switching, Toast.LENGTH_SHORT).show();
                VideoController.getInstance().setVideoModeType(Long.valueOf(mPeerId), currentDefinition);// 切换时间为0-2s
//                mSwitchDefinition.setEnabled(false);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ibSwitchDefinition.setEnabled(true);
                    }
                }, 2000);

            }
        });


        ibSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitchCameraIndex++;
                if (mSwitchCameraIndex % 2 == 0) {//打开摄像头，对方和自己画面都显示
                    VideoController.getInstance().execute(openCamera, null);
                    mGlSmallVideoView.setVisibility(GLView.VISIBLE);
                    mGlBigVideoView.setVisibility(GLView.VISIBLE);
                    ibSwitchCamera.setImageResource(R.drawable.button_open_video);
                } else {//关闭摄像头，根据条件是来显示大画面还是显示小画面
                    VideoController.getInstance().execute(closeCamera, null);
                    if (mSwitchVideoIndex % 2 == 0) {
                        mGlSmallVideoView.setVisibility(GLView.INVISIBLE);
                        mGlBigVideoView.setVisibility(GLView.VISIBLE);
                    } else {
                        mGlBigVideoView.setVisibility(GLView.INVISIBLE);
                        mGlSmallVideoView.setVisibility(GLView.VISIBLE);
                    }
                    ibSwitchCamera.setImageResource(R.drawable.button_close_the_video);
                }
            }
        });
        ibSwitchVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (VideoController.getInstance().isSelfMute()) {
                    VideoController.getInstance().setSelfMute2(false);
                    ibSwitchVoice.setImageResource(R.drawable.button_speaker);
                } else {
                    VideoController.getInstance().setSelfMute2(true);
                    ibSwitchVoice.setImageResource(R.drawable.button_mute);
                }
            }
        });
        ibHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        beforeReceive();
    }

    /**
     * 接听之后的视图
     */
    public void beforeReceive() {
        llReceiveAfter.setVisibility(View.GONE);
        tvTime.setVisibility(View.GONE);

    }

    /**
     * 接听之后的视图
     */
    public void afterReceive() {
        llReceiveBefore.setVisibility(View.GONE);
        tvTime.setVisibility(View.VISIBLE);
        llReceiveAfter.setVisibility(View.VISIBLE);


    }

    private void initReceiver() {
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


        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mBroadcastHandler, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        LoggerUtils.d(TAG, "onResume: ");
        initReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        LoggerUtils.d(TAG, "onPause: ");
        terminateVideo();
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
        mGlBigVideoView.setBackground(R.drawable.bg);
        //设置边框颜色和边框宽度，不需要可以注释下面这两行代码
//        mGlBigVideoView.setPaddingColor(Color.YELLOW);
//        mGlBigVideoView.setPaddings(2, 2, 2, 2);

        mGlSmallVideoView = new GLVideoView(this);
        mGlpanelView.addView(mGlSmallVideoView);
        mGlSmallVideoView.setIsPC(false);
        mGlSmallVideoView.enableLoading(false);
        mGlSmallVideoView.setMirror(true);
        mGlSmallVideoView.setNeedRenderVideo(true);
        mGlSmallVideoView.setVisibility(GLView.VISIBLE);
        mGlSmallVideoView.setScaleType(ScaleType.CENTER_CROP);
        //设置主人区背景
        mGlSmallVideoView.setBackground(R.drawable.bg);

        mGlSmallVideoView.setOnTouchListener(new GLView.OnTouchListener() {
            @Override
            public boolean onTouch(GLView glView, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        switchVideo();
                        break;
                }
                return true;
            }
        });

        //设置边框颜色和边框宽度，不需要可以注释下面这两行代码
//        mGlSmallVideoView.setPaddingColor(Color.WHITE);
//        mGlSmallVideoView.setPaddings(2, 2, 2, 2);
        GraphicRenderMgr.getInstance().setGlRender(mSelfDin, mGlSmallVideoView.getYuvTexture());
        GraphicRenderMgr.getInstance().setGlRender(mPeerId, mGlBigVideoView.getYuvTexture());
    }

    private long mTimeClick = 0;

    private GLView.OnTouchListener mTouchListener = new GLView.OnTouchListener() {
        @Override
        public boolean onTouch(GLView view, MotionEvent event) {
            // TODO Auto-generated method stub
//            long timeNow = System.currentTimeMillis();
//            if (MotionEvent.ACTION_DOWN == event.getAction()) {
//                if (timeNow - mTimeClick < 600) {
//                    if (mLogInfo != null) {
//                        mLogInfo.setVisibility(View.VISIBLE);
//                    }
//                } else {
//                    if (mLogInfo != null) {
//                        mLogInfo.setVisibility(View.INVISIBLE);
//                    }
//                }
//                mTimeClick = timeNow;
//            }
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
        int height = mGlRootView.getHeight();

        //layout的四个参数定义： public void layout(int left, int top, int right, int bottom)
//        mGlBigVideoView.layout(0, 0, width / 2, width / 2);
        mGlBigVideoView.layout(0, 0, width, height);
        mGlBigVideoView.invalidate();

        //layout的四个参数定义： public void layout(int left, int top, int right, int bottom)
//        mGlSmallVideoView.layout(width / 2, 0, width, width / 2);
        mGlSmallVideoView.layout(16, 15, 238, 156);
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
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) localVideo.getLayoutParams();
            params.leftMargin = (int) getResources().getDimension(R.dimen.dimen__125);
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
                    LoggerUtils.d(TAG, "asyncOpenCamera failed to start camera.");
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera failed to start camera.");
                    }
                    return;
                } else {
                    LoggerUtils.d(TAG, "asyncOpenCamera success.");
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera success.");
                    }
                }
                LoggerUtils.d(TAG, "asyncOpenCamera end ");
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
                boolean flag = mCamera.closeCamera();
                LoggerUtils.d(TAG, "close camera successful : " + flag);
            }
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "closeCamera end.");
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        LoggerUtils.d(TAG, "onStop: ");
    }


    private void terminateVideo() {
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
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess();
        }
    }

    @Override
    protected void onDestroy() {
        LoggerUtils.d(TAG, "onDestory: ");
        super.onDestroy();
        Log.d(TAG, "onDestroy");

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
                BooyueVideoChatActivitySF.this.finish();
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
            /**
             * 停止视频聊天
             */
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
//                if (mLogInfo != null) {
//                    mLogInfo.setText(msg);
//                }
                /**
                 * 视频接通
                 */
            } else if (intent.getAction().equalsIgnoreCase(VideoController.ACTION_CHANNEL_READY)) {
                VideoController.getInstance().stopRing();
                VideoController.getInstance().stopShake();
                VideoController.getInstance().startShake();
                mVideoConnected = true;
//                mClose.setText(R.string.close);
                startTime = System.currentTimeMillis();
                mHandler.post(updateTime);
                /**
                 * 绑定列表改变
                 */
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
                /**
                 * 解除所有的绑定者
                 */
            } else if (intent.getAction() == TXDeviceService.OnEraseAllBinders) {
                finish();
                /**
                 * 视频质量服务改变
                 * {@link VideoController#onNotifyVideoQos(int, int, int, int)}
                 */


            } else if (intent.getAction().equals(ACTION_VIDEO_QOS_NOTIFY)) {
//                int width = intent.getIntExtra("width", VcCamera.PREVIEW_WIDTH);
//                int height = intent.getIntExtra("height", VcCamera.PREVIEW_HEIGHT);
//                LoggerUtils.d(TAG + "---------ACTION_VIDEO_QOS_NOTIFY: width = " + width + ",,height = " + height);
//                LoggerUtils.d(TAG + "---------ACTION_VIDEO_QOS_NOTIFY: VcCamera.PREVIEW_WIDTH = " + VcCamera.PREVIEW_WIDTH +
//                        ",,VcCamera.PREVIEW_HEIGHT = " + VcCamera.PREVIEW_HEIGHT);
//
//                // 需要更新采集的分辨率，重启摄像头
////                VcCamera.PREVIEW_WIDTH = width;
////                VcCamera.PREVIEW_HEIGHT = height;
//
//                if (width != VcCamera.PREVIEW_WIDTH || height != VcCamera.PREVIEW_HEIGHT) {
//                    // 需要更新采集的分辨率，重启摄像头
////                    VcCamera.PREVIEW_WIDTH = width;
////                    VcCamera.PREVIEW_HEIGHT = height;
//                    VideoController.getInstance().execute(closeCamera, null);
//                    VideoController.getInstance().execute(openCamera, null);
//                }
                /**
                 * 网络等级
                 */
            } else if (intent.getAction().equals(VideoController.ACTION_NET_LEVEL)) {
                // 双方网络分级selfNetLevel和peerNetLevel表示自己和对方的网络状况，1-3依次为好，一般，差
                String tips = intent.getStringExtra("displayNetState");
                if (tips != null) {
                    Toast.makeText(context, tips, Toast.LENGTH_SHORT).show();
                }
                /**
                 * 网络差的情况
                 */
            } else if (intent.getAction().equals(VideoController.ACTION_NET_BAD)) {
                // 任意一方网络不太好，每3s计算一次
                if (currentDefinition == VideoController.DEFINITION_TYPE_CLEAR) {
                    Toast.makeText(context, R.string.network_poor, Toast.LENGTH_SHORT).show();
                }
            } else if ((Intent.ACTION_CLOSE_SYSTEM_DIALOGS).equals(intent.getAction())) {
                String reasonKey = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reasonKey)) {
                    LoggerUtils.d(TAG, "press home key: ");
                    ///按下Home键
                    finish();
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

    }

    /**
     * 按钮点击切换窗口
     *
     * @param view
     */
    public void onBtnSwitchVideo(View view) {
//		GraphicRenderMgr.getInstance().setGlRender(mSelfDin, null);
//		mGlRootView.requestRenderForced();
        switchVideo();
    }

    /**
     * 窗口切换
     */
    public void switchVideo() {
        mSwitchVideoIndex++;
        String key1 = mPeerId;
        String key2 = mSelfDin;

        if (mSwitchVideoIndex % 2 == 0) {
            //mGlBigVideoView 显示对方 mGlSmallVideoView显示自己
            GraphicRenderMgr.getInstance().setGlRender(key1, mGlBigVideoView.getYuvTexture());
            GraphicRenderMgr.getInstance().setGlRender(key2, mGlSmallVideoView.getYuvTexture());
            //显示mpeerId的videoview显示出来
            mGlBigVideoView.setVisibility(GLView.VISIBLE);

        } else {
            //mGlBigVideoView 显示自己 mGlSmallVideoView显示对方
            GraphicRenderMgr.getInstance().setGlRender(key2, mGlBigVideoView.getYuvTexture());
            GraphicRenderMgr.getInstance().setGlRender(key1, mGlSmallVideoView.getYuvTexture());
            //显示mpeerId的videoview显示出来
            mGlSmallVideoView.setVisibility(GLView.VISIBLE);
        }
    }

    Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            if (tvTime.getVisibility() == View.GONE) {
                tvTime.setVisibility(View.VISIBLE);
            }
            tvTime.setText(TimeUtils.long2TimeFormat(startTime));
            mHandler.postDelayed(updateTime, 1000);
        }
    };
    //
    static public final String SYSTEM_DIALOG_REASON_KEY = "reason";
    //    static public final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
//    static public final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    static public final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";


}