package com.tencent.av;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

import com.booyue.audiochat.BooyueAudioChatActivity;
import com.booyue.uils.LoggerUtils;
import com.booyue.videochat.BooyueVideoChatActivitySF;
import com.tencent.device.ITXDeviceService;
import com.tencent.device.TXDeviceService;
import com.tencent.devicedemo.VideoChatActivityHW;
import com.tencent.devicedemo.VideoChatActivityNFC;

/**
 * 视频服务
 */

public class VideoService extends Service {

    private static String TAG = "VideoService-----";

    private ITXDeviceService mTXDeviceService = null;

    private boolean mInitedVideoEngine = false;

    public VideoService() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        /**
         * 绑定腾讯服务
         */
        Intent intent = new Intent();
        intent.setAction("com.tencent.device.RemoteTXDeviceService");//TXDeviceService
        intent.setPackage(this.getApplicationInfo().packageName);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        try {
            if (mTXDeviceService != null) {
                mTXDeviceService.notifyVideoServiceStarted();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    private ServiceConnection mConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            // TODO Auto-generated method stub
            try {
                IntentFilter filter = new IntentFilter();
                filter.addAction(TXDeviceService.OnSendVideoCall);
                filter.addAction(TXDeviceService.OnSendVideoCallM2M);
                filter.addAction(TXDeviceService.OnSendVideoCMD);
                filter.addAction(TXDeviceService.OnReceiveVideoBuffer);
                filter.addAction(TXDeviceService.StartVideoChatActivity);
                filter.addAction(TXDeviceService.StartAudioChatActivity);
                filter.addAction(TXDeviceService.BinderListChange);
                filter.addAction(TXDeviceService.OnRecvLANCommunicationCSReply);
                registerReceiver(mVideoReceiver, filter);
                mTXDeviceService = ITXDeviceService.Stub.asInterface(arg1);
                VideoController.getInstance().setTXDeviceService(mTXDeviceService);
                initVideoEngine();
                mTXDeviceService.notifyVideoServiceStarted();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
            mTXDeviceService = null;
            unregisterReceiver(mVideoReceiver);
        }
    };

    public void initVideoEngine() {
        if (false == mInitedVideoEngine) {
            if (Long.parseLong(VideoController.getInstance().GetSelfDin()) != 0) {
                //初始化音视频引擎
                //bHwEnc和bHwDec可以组成四种组合：软编软解（支持）、软编硬解（不支持）、硬编硬解（支持）、硬编软解（支持）
                //public void initVcController(Context context, boolean bHwEnc, boolean bHwDec)
                VideoController.getInstance().initVcController(VideoService.this, TXDeviceService.VideoHardEncodeEnable, TXDeviceService.VideoHardDecodeEnable);
                mInitedVideoEngine = true;
            }
        }
    }

    public BroadcastReceiver mVideoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            initVideoEngine();
            String action = intent.getAction();
            if (action == TXDeviceService.OnSendVideoCall) {
                VideoController.getInstance().onSendVideoCall(intent.getByteArrayExtra("msg"));
                LoggerUtils.Companion.d(TAG + "onReceive: OnSendVideoCall");

            } else if (action == TXDeviceService.OnSendVideoCallM2M) {
                VideoController.getInstance().onSendVideoCallM2M(intent.getByteArrayExtra("msg"));
                LoggerUtils.Companion.d(TAG + "onReceive: OnSendVideoCallM2M");


            } else if (action == TXDeviceService.OnSendVideoCMD) {
                VideoController.getInstance().onSendVideoCMD(intent.getByteArrayExtra("msg"));
                LoggerUtils.Companion.d(TAG + "onReceive: OnSendVideoCMD");


            } else if (action == TXDeviceService.OnReceiveVideoBuffer) {
                VideoController.getInstance().onReceiveVideoBuffer(intent.getByteArrayExtra("msg"),
                        intent.getLongExtra("uin", 0), intent.getIntExtra("uinType", 0));
                LoggerUtils.Companion.d(TAG + "onReceive: OnReceiveVideoBuffer");


            } else if (action == TXDeviceService.StartVideoChatActivity) {
                if (VideoController.getInstance().hasPendingChannel()) {
                    Toast.makeText(VideoService.this, "视频监控中，请稍后……", Toast.LENGTH_LONG).show();
                } else {
                    Intent videoIntent = null;
                    if (VideoController.getInstance().isHasLocalCam()) {
                        if (VideoController.isHardwareEncoderEnabled()) {
                            videoIntent = new Intent(VideoService.this, VideoChatActivityHW.class);
                            LoggerUtils.Companion.d(TAG + "onReceive: VideoChatActivityHW");

                        } else {
//                            videoIntent = new Intent(VideoService.this, VideoChatActivitySF.class);
                            videoIntent = new Intent(VideoService.this, BooyueVideoChatActivitySF.class);
//                            videoIntent = new Intent(VideoService.this, BooyueFriendInfoActivity.class);
                            LoggerUtils.Companion.d(TAG + "onReceive: VideoChatActivitySF");
                        }
                    } else {
                        videoIntent = new Intent(VideoService.this, VideoChatActivityNFC.class);
                        LoggerUtils.Companion.d(TAG + "onReceive: VideoChatActivityNFC");
                    }

                    videoIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                    videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    videoIntent.putExtra("peerid", String.valueOf(intent.getLongExtra("peerid", 0)));
                    videoIntent.putExtra("dinType", intent.getIntExtra("dinType", VideoController.UINTYPE_QQ));
                    startActivity(videoIntent);
                }
                LoggerUtils.Companion.d(TAG + "onReceive: StartVideoChatActivity");


            } else if (action == TXDeviceService.StartAudioChatActivity) {
//                Intent intent1 = new Intent(VideoService.this, AudioChatActivity.class);
                Intent intent1 = new Intent(VideoService.this, BooyueAudioChatActivity.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent1.putExtra("peerid", String.valueOf(intent.getLongExtra("peerid", 0)));
                intent1.putExtra("dinType", intent.getIntExtra("dinType", VideoController.UINTYPE_QQ));
                startActivity(intent1);
                LoggerUtils.Companion.d(TAG + "onReceive: StartAudioChatActivity");


            } else if (action == TXDeviceService.BinderListChange) {
                VideoController.getInstance().updateSignature();
                LoggerUtils.Companion.d(TAG + "onReceive: BinderListChange");


            } else if (action == TXDeviceService.OnRecvLANCommunicationCSReply) {
                VideoController.recvLANCommunicationReply(intent.getByteArrayExtra("buffer"));
                LoggerUtils.Companion.d(TAG + "onReceive: OnRecvLANCommunicationCSReply");
            }
        }
    };
}
