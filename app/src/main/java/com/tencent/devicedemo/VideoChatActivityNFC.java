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
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.av.VideoController;
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

public class VideoChatActivityNFC extends Activity implements CameraListAdapter.INotifyCameraConnect {
    private static final String TAG = "VideoChatActivityNFC";

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
    Button mScanDev;

    CameraListAdapter mCameraListData;
    ListView mCameraListView;

    TextView mLogInfo;

    BroadcastHandler mBroadcastHandler;

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
        mSelfDin = VideoController.getInstance().GetSelfDin();
        mIsReceiver = intent.getBooleanExtra("receive", false);
        mDinType = intent.getIntExtra("dinType", VideoController.UINTYPE_QQ);
        if (Long.parseLong(mPeerId) == 0 || Long.parseLong(mSelfDin) == 0) {
            QLog.e(TAG, QLog.CLR, "invalid peerId: " + mPeerId + " invalid selfDin: " + mSelfDin);
            finish();
        }

        initQQGlView();

        initCameraPreview();

        mLogInfo = (TextView) findViewById(R.id.logInfo);

        mBroadcastHandler = new BroadcastHandler();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT);
        filter.addAction(VideoController.ACTION_NETMONIOTR_INFO);
        filter.addAction(VideoController.ACTION_CHANNEL_READY);
        filter.addAction(VideoController.ACTION_VIDEO_QOS_NOTIFY);
        filter.addAction(TXDeviceService.BinderListChange);
        filter.addAction(TXDeviceService.OnEraseAllBinders);
        filter.addAction(VideoController.OnRecvDeviceRespond);
        filter.addAction(VideoController.OnDeviceScanTimeOut);
        filter.addAction(VideoController.OnDeviceConnTimeOut);
        filter.addAction(VideoController.OnDeviceConnSuccess);

        registerReceiver(mBroadcastHandler, filter);

        mAccept = (Button) findViewById(R.id.av_video_accept);
        mReject = (Button) findViewById(R.id.av_video_reject);
        mClose = (Button) findViewById(R.id.av_video_close);
        mSwitch = (Button) findViewById(R.id.av_video_switch);

        mScanDev = (Button) findViewById(R.id.textview_scandevice);
        mScanDev.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        mScanDev.setVisibility(View.VISIBLE);

        mCameraListData = new CameraListAdapter(this.getApplicationContext(), this);
        mCameraListView = (ListView) findViewById(R.id.listView_cameralist);
        mCameraListView.setAdapter(mCameraListData);

        VideoController.getInstance().enableQosNotify(true);

        if (mIsReceiver) {
            mAccept.setVisibility(View.VISIBLE);
            mReject.setVisibility(View.VISIBLE);
        } else {
            mSwitch.setVisibility(View.VISIBLE);
            mClose.setVisibility(View.VISIBLE);
            mClose.setText("取消");
            if (Long.parseLong(mPeerId) != 0) {
                VideoController.getInstance().request(mPeerId, mDinType);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

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
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

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
        GraphicRenderMgr.getInstance().clearCameraFrames();

        VideoController.getInstance().closeLocalAVSession();
        VideoController.getInstance().setSuggestedAudioSampleRate(0);

        VideoController.setCurrentPeerTinyId(0);

        VideoController.getInstance().enableQosNotify(false);

        finish();
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
        mGlBigVideoView.setScaleType(ScaleType.CENTER_CROP);
        mGlBigVideoView.setOnTouchListener(mTouchListener);
        //设置客人区背景
        mGlBigVideoView.setBackground(R.drawable.qav_video_bg_s);
        mGlBigVideoView.setPaddingColor(Color.YELLOW);
        mGlBigVideoView.setPaddings(2, 2, 2, 2);

        mGlSmallVideoView = new GLVideoView(this);
        mGlpanelView.addView(mGlSmallVideoView);
        mGlSmallVideoView.setIsPC(false);
        mGlSmallVideoView.enableLoading(false);
        mGlSmallVideoView.setMirror(false);
        mGlSmallVideoView.setNeedRenderVideo(true);
        mGlSmallVideoView.setVisibility(GLView.VISIBLE);
        mGlSmallVideoView.setScaleType(ScaleType.CENTER_CROP);
        //设置主人区背景
        mGlSmallVideoView.setBackground(R.drawable.qav_video_bg_s);
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
        int width = mGlRootView.getWidth();
        int height = mGlRootView.getHeight();

        int margin = 30;
        int widthBigVideo = (width * 2 / 3 - margin * 2);
        int widthSmallVideo = (width * 1 / 3 - margin * 2);

        int heightBigVideo = (height - margin * 4);
        int heightSmallVideo = (height * 2 / 3 - margin * 2);

        mGlBigVideoView.layout(margin, margin, margin + widthBigVideo, margin + heightBigVideo);
        mGlBigVideoView.invalidate();

        mGlSmallVideoView.layout(width * 2 / 3 + margin, (height - heightSmallVideo) / 2, width - margin, (height - heightSmallVideo) / 2 + heightSmallVideo);
        mGlSmallVideoView.invalidate();

        {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(widthSmallVideo, heightSmallVideo);
            layoutParams.topMargin = (height - heightSmallVideo) / 2;
            layoutParams.rightMargin = margin;
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            mCameraListView.setLayoutParams(layoutParams);
        }

        {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(widthSmallVideo, mScanDev.getHeight());
            layoutParams.topMargin = (height - heightSmallVideo) / 2 - mScanDev.getHeight() - 5;
            layoutParams.rightMargin = margin;
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            mScanDev.setLayoutParams(layoutParams);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        super.unregisterReceiver(mBroadcastHandler);
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
                VideoChatActivityNFC.this.finish();
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
                VideoController.getInstance().setCurrentPeerTinyId(Long.parseLong(mPeerId));
                VideoController.getInstance().requestIFrame();
            } else if (intent.getAction().equalsIgnoreCase(VideoController.ACTION_VIDEO_QOS_NOTIFY)) {
                int bitrate = intent.getIntExtra("bitrate", 0);
                if (bitrate != 0) {
                    VideoController.getInstance().setVideoBitrate(bitrate);
                }
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
            } else if (intent.getAction() == VideoController.OnRecvDeviceRespond) {
                long din = intent.getLongExtra("din", 0);
                String remark = intent.getStringExtra("remark");
                String headUrl = intent.getStringExtra("headUrl");
                mCameraListData.addCameraInfo(din, remark, headUrl);
            } else if (intent.getAction() == VideoController.OnDeviceScanTimeOut) {
                mScanDev.setEnabled(true);
                //showToastMessage("扫描结束");
            } else if (intent.getAction() == VideoController.OnDeviceConnTimeOut) {
                mCameraListView.setVisibility(View.VISIBLE);
                showToastMessage("连接失败");
            } else if (intent.getAction() == VideoController.OnDeviceConnSuccess) {
                mCameraListView.setVisibility(View.INVISIBLE);
                //设置连接成功的DIN
                long din = intent.getLongExtra("din", 0);
                VideoController.getInstance().setConnectedCameraDin(din);
                //启动视频流
                VideoController.getInstance().startVideoStream(din);
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
        }
    }

    public void onBtnSwitchVideo(View view) {
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

    public void onScanDevice(View view) {
        mCameraListData.clear();
        mScanDev.setEnabled(false);
        mCameraListView.setVisibility(View.VISIBLE);
        VideoController.getInstance().startDeviceScan(3);
    }

    @Override
    public void nofityCameraConnect(long din) {
        // TODO Auto-generated method stub
        GraphicRenderMgr.getInstance().clearCameraFrames();
        GraphicRenderMgr.getInstance().flushGlRender(mSelfDin);

        VideoController.getInstance().closeLocalAVSession();

        VideoController.getInstance().notifyDeviceConnect(din, 5, VideoController.LanDeviceConnectType_ZQL);
        mCameraListView.setVisibility(View.INVISIBLE);
    }

    ;

    private void showToastMessage(String strMsg) {
        int xOffset = mCameraListView.getLeft() + mCameraListView.getWidth() / 2 - mGlRootView.getWidth() / 2;
        int yOffset = mCameraListView.getTop() + mCameraListView.getHeight() / 2 - mGlRootView.getHeight() / 2;

        Toast toast = Toast.makeText(getApplicationContext(), strMsg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, xOffset, yOffset);
        toast.show();
    }
}

