package com.tencent.av;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.booyue.monitor.BooyueVideoMonitorService;
import com.booyue.utils.LoggerUtils;
import com.tencent.av.camera.AndroidCamera;
import com.tencent.av.camera.VcCamera;
import com.tencent.av.core.AbstractNetChannel;
import com.tencent.av.core.IVideoEventListener;
import com.tencent.av.core.VcCapability;
import com.tencent.av.core.VcControllerImpl;
import com.tencent.av.core.VcSystemInfo;
import com.tencent.av.core.VideoConstants;
import com.tencent.av.opengl.GraphicRenderMgr;
import com.tencent.av.thread.FutureListener;
import com.tencent.av.thread.ThreadPool;
import com.tencent.av.thread.ThreadPool.Job;
import com.tencent.av.thread.ThreadPool.JobContext;
import com.tencent.av.utils.TraeHelper;
import com.tencent.device.FriendInfo;
import com.tencent.device.ITXDeviceService;
import com.tencent.device.QLog;
import com.tencent.device.TXDeviceService;
import com.tencent.sharp.jni.TraeAudioManager;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@SuppressLint("NewApi")
public class VideoController extends AbstractNetChannel implements IVideoEventListener {

    static String TAG = "VideoController-----";

    static {
        try {
            System.loadLibrary("stlport_shared");
            System.loadLibrary("txlancommunication");
            System.loadLibrary("TVLANCommunication");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public static final String ACTION_NETMONIOTR_INFO = "com.tencent.device.videocontroller.netmonitorinfo";
    public static final String ACTION_CHANNEL_READY = "com.tencent.device.videocontroller.channelready";
    public static final String ACTION_VIDEOFRAME_INCOME = "com.tencent.device.videocontroller.videoframeincome";
    public static final String ACTION_VIDEO_QOS_NOTIFY = "com.tencent.device.videocontroller.videoqosnotify";
    public static final String ACTION_NET_LEVEL = "com.tencent.device.videocontroller.videonetlevel";
    public static final String ACTION_NET_BAD = "com.tencent.device.videocontroller.videonetbad";


    public static final int DEFINITION_TYPE_FLUENT = 1;
    public static final int DEFINITION_TYPE_CLEAR = 2;


    private static final int QQ_DEVICE_CONNECT_TYPE = 1;

    private static boolean mEnableHWEncoder = false;
    private static boolean mEnableHWDecoder = false;

    //UIN_TYPE
    public final static int UINTYPE_QQ = 1;
    public final static int UINTYPE_DIN = 4;

    ThreadPool mThreadPool = new ThreadPool(1, 1);

    public String deviceName = null;
    public String[] strDeviceList = null;
    public String mAudioStateBeforePhoneCall = TraeAudioManager.DEVICE_NONE;

    public Timer tmCommunication = null;
    public TimerTask tmTaskCommunication = null;
    public String strPeerUin = null;
    public int nOnTimeCnt = 0;
    public static long TIME_TO_START_REPORT = 40 * 60;
    public static long TIME_INTERVAL_REPORT = 10 * 60;
    public static int COMMUNICATION_TYPE_VIDEO = 0;
    public static int COMMUNICATION_TYPE_AUDIO = 1;
    public boolean mIsAudioCall = false;

    TraeHelper mTraeHelper = null;

    // camera
    VcCamera mVcCamera = null;

    // controller
    VcControllerImpl mVcCtrl = null;

    Context mContext;

    String mSelfDin = null;
    Map<String, Boolean> mMapUinPending = new HashMap<>();
    Map<String, Integer> mMapUinType = new HashMap<>();

    static VideoController g_Instance = null;

    ITXDeviceService mTXDeviceService = null;

    private Handler handler = null;

    private boolean remoteMute;// 标记对方静音
    private boolean selfMute;// 标记自己静音
    private int mCurrentDefinitionType;

    private AudioManager audioManager;

    public static VideoController getInstance() {
        if (g_Instance == null) {
            g_Instance = new VideoController();
        }
        return g_Instance;
    }

    /**
     * 是否存在本地摄像头
     *
     * @return true 存在本地摄像头 false 不存在
     */
    public boolean isHasLocalCam() {
        boolean canUse = true;
        Camera mCamera = null;
        try {
            // TODO camera驱动挂掉,处理??
            mCamera = Camera.open();
        } catch (Exception e) {
            canUse = false;
        }

        if (canUse && mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        return canUse;
    }

    //bHwEnc和bHwDec可以组成四种组合：软编软解（支持）、软编硬解（不支持）、硬编硬解（支持）、硬编软解（支持�?
    public void initVcController(Context context, boolean bHwEnc, boolean bHwDec) {


        //获取音频服务
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        //设置声音模式
        audioManager.setMode(AudioManager.STREAM_MUSIC);


        //定时器
        tmCommunication = new Timer("communication_timer", true);
        Log.d("initVcController", "create timer ok");
        tmTaskCommunication = new TimerTask() {
            @Override
            public void run() {
                Log.d("initVcController", "OnTimer");
                long peerUin = Long.parseLong(strPeerUin);

                //到了40分钟，每个10分钟报一次消息
                long curSecond = TIME_TO_START_REPORT + nOnTimeCnt * TIME_INTERVAL_REPORT;
                nOnTimeCnt++;

                int uinType = UINTYPE_QQ;
                if (mMapUinType.containsKey(String.valueOf(peerUin))) {
                    uinType = mMapUinType.get(String.valueOf(peerUin));
                }

                if (uinType == UINTYPE_DIN) {
                    int CommType = COMMUNICATION_TYPE_VIDEO;
                    if (mIsAudioCall) CommType = COMMUNICATION_TYPE_AUDIO;
                    if (TXDeviceService.VideoProcessEnable) {
                        Log.d("initVcController", "enabled");
                        try {
                            if (mTXDeviceService != null) {
                                mTXDeviceService.reportCommunicationTime(peerUin, curSecond, CommType);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        TXDeviceService.reportCommunicationTime(peerUin, curSecond, CommType);
                    }
                }

            }
        };

        // 判断设备系统版本
        if (Build.VERSION.SDK_INT < 18) {
            bHwEnc = false;
            bHwDec = false;
        }

        // 不支持软件硬解
        if (bHwEnc == false && bHwDec == true) {
            bHwDec = false;
        }

        mEnableHWEncoder = bHwEnc;
        mEnableHWDecoder = bHwDec;

        String selfDin = GetSelfDin();
        if (TextUtils.isEmpty(selfDin)) {
            return;
        }

        if (mSelfDin != null && mVcCtrl != null) {
            if (mSelfDin.equalsIgnoreCase(selfDin)) {
                //do nothing
            } else {
                mSelfDin = selfDin;
                mVcCtrl.UpdateSelfUin(selfDin);
                GraphicRenderMgr.getInstance().setAccountUin(selfDin);
            }
            return;
        }

        mContext = context;

        WindowManager mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displaysMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(displaysMetrics);
        int screenWidth = displaysMetrics.widthPixels;
        int screenHeight = displaysMetrics.heightPixels;
        int rotation = mWindowManager.getDefaultDisplay().getRotation();
        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, "screen info width:" + screenWidth + ", height:" + screenHeight + ", rotation:" + rotation);
        }

        // 解决横屏时黑边问�?(手Q默认是竖屏的没有类似问题)
        // 这里的screenHeight、screenWidth会传到手Q端，用于决策手Q的黑边填充逻辑
        // 假如这里的高宽比，小于手q端视频源的高宽比，则会产生黑�?
        // 物联的TVSDK, 屏幕足够显示手Q端传过来的视频大小，所以将高宽比设置调整为大于1，这样手Q端不会设置黑�?

        if (screenHeight < screenWidth) {
            int tempWidth = screenHeight;
            screenHeight = screenWidth;
            screenWidth = tempWidth;
        }


//		TraeAudioManager.init(getContext());
//		UITools.initTrae(getContext());

        try {
            int apnType = getApn();
            byte[] signature = getVideoChatSignature();
            VcControllerImpl vcCtrl = new VcControllerImpl(getContext(),
                    "537039075", this, this, screenWidth,
                    screenHeight, apnType, mEnableHWEncoder, mEnableHWDecoder);

            vcCtrl.init(getContext(), Long.parseLong(selfDin), "", Build.MODEL,
                    "537039075", "30",
                    "4B2163B5E79E88BB",
                    getDeviceIMEI(), VcSystemInfo.getDeviceName(),
                    Build.VERSION.RELEASE, Build.VERSION.INCREMENTAL,
                    Build.MANUFACTURER, VcSystemInfo.getCPUName(), apnType);
            vcCtrl.updateSignature(Long.parseLong(selfDin), signature);

            mVcCtrl = vcCtrl;
            mSelfDin = selfDin;

            mTraeHelper = TraeHelper.createInstanse(context, this);

            ApplicationInfo appInfo = context.getApplicationInfo();
            String libDir = appInfo.nativeLibraryDir;
            String dataDir = appInfo.dataDir;
            if (Build.VERSION.SDK_INT >= 9) {
                libDir = appInfo.nativeLibraryDir;
            } else {
                libDir = appInfo.dataDir + "/lib";
            }
            GraphicRenderMgr.getInstance().initDeviceInfo(libDir, dataDir);
            GraphicRenderMgr.getInstance().setAccountUin(selfDin);
            initLANCommunication(Long.parseLong(selfDin));
            setEncodeDecodePtr(false);
            setSenderRenderPtr();

            handler = new Handler(Looper.getMainLooper());
        } catch (UnsatisfiedLinkError e) {
            mVcCtrl = null;
        }
    }

    /**
     * 获取自身的din
     *
     * @return
     */
    public String GetSelfDin() {
        String strSelfDin = String.valueOf(nativeGetSelfDin());
        return strSelfDin;
    }

    public void updateSignature() {
        if (mVcCtrl != null) {
            byte[] signature = getVideoChatSignature();
            mVcCtrl.updateSignature(Long.parseLong(GetSelfDin()), signature);
        }
    }

    public void setTXDeviceService(ITXDeviceService service) {
        mTXDeviceService = service;
    }

    public static boolean isHardwareEncoderEnabled() {
        return mEnableHWEncoder;
    }

    public static boolean isHardwareDecoderEnabled() {
        return mEnableHWDecoder;
    }

    public void enableQosNotify(boolean notify) {
        if (mVcCtrl != null) {
            mVcCtrl.enableQosNotify(notify);
        }
    }

    public void enableVideoRecvNotify(boolean notify) {
        if (mVcCtrl != null) {
            mVcCtrl.enableVideoRecvNotify(notify);
        }
    }

    public int updateSelfUin(String uin) {
        if (mVcCtrl != null) {
            return mVcCtrl.UpdateSelfUin(uin);
        }
        return -1;
    }

    public Context getContext() {
        return mContext;
    }

    int getApn() {
        return VcCapability.AP_INTERNET;
    }

    // Modify by shawn, 原先的逻辑在三星i9300中崩�?debug的逻辑才走到这�?
    String getDeviceIMEI() {
        try {
            return ((TelephonyManager) getContext().getSystemService(
                    Context.TELEPHONY_SERVICE)).getDeviceId();
        } catch (Exception e) {
        }

        return "1234567890";
    }

    public VcCamera getCamera() {
        if (mVcCamera == null) {
            if (mContext != null) {
                mVcCamera = new VcCamera(this);
            }
        }
        return mVcCamera;
    }

    /**
     * 判断是否正在视频
     *
     * @return true 正在视频  false 未在视频
     */
    public boolean hasPendingChannel() {
        return mMapUinPending.size() > 0;
    }

    // AppType
    public final static int AppType_Audio = 0;
    public final static int AppType_Video = 1;
    public final static int AppType_Audio_SwitchTer = 1;
    public final static int AppType_Video_SwitchTer = 0;
    // RelationType
    public final static int RelationType_Friends = 1;
    public final static int RelationType_Discuss = 2;
    public final static int RelationType_Group = 3;
    public final static int RelationType_Temp = 4;

    private static final byte[] NULL = null;

    public int request(String peerUin, int uinType) {
        if (mVcCtrl == null) {
            return -1;
        }
        mMapUinPending.put(peerUin, false);
        mMapUinType.put(peerUin, uinType);
        requestAudioFocus();
        return mVcCtrl.requestVideo(peerUin, 0, VcCapability.AP_INTERNET, AppType_Video, RelationType_Friends,
                "", "", "", 9500, "", "", 0, null, "", "");
    }

    public int requestAudio(String peerUin, int uinType) {
        if (mVcCtrl == null) {
            return -1;
        }
        mMapUinPending.put(peerUin, false);
        mMapUinType.put(peerUin, uinType);
        requestAudioFocus();
        mIsAudioCall = true;
        return mVcCtrl.requestVideo(peerUin, 0, VcCapability.AP_INTERNET, AppType_Audio, RelationType_Friends,
                "", "", "", 9500, "", "", 0, null, "", "");
    }

    public int acceptRequest(String peerUin) {
        if (mVcCtrl == null) {
            return -1;
        }
        mMapUinPending.put(peerUin, false);
        abandonAudioFocus();
        return mVcCtrl.acceptVideo(peerUin, 0, VcCapability.AP_INTERNET, AppType_Video, RelationType_Friends);
    }

    public int acceptRequestAudio(String peerUin) {
        if (mVcCtrl == null) {
            return -1;
        }
        mMapUinPending.put(peerUin, false);
        abandonAudioFocus();
        mIsAudioCall = true;
        return mVcCtrl.acceptVideo(peerUin, 0, VcCapability.AP_INTERNET, AppType_Audio, RelationType_Friends);
    }

    public int rejectRequestAudio(String peerUin) {
        return rejectRequest(peerUin);
    }

    public int rejectRequest(String peerUin) {
        if (mVcCtrl == null) {
            return -1;
        }
        mMapUinPending.remove(peerUin);
        abandonAudioFocus();
        return mVcCtrl.rejectVideo(peerUin, getApn(), VideoConstants.VOIP_REASON_REJECT_BY_SELF);
    }

    public int ignoreRequest(String peerUin) {
        if (mVcCtrl == null) {
            return -1;
        }
        mMapUinPending.remove(peerUin);
        abandonAudioFocus();
        return mVcCtrl.ignoreVideo(peerUin, getApn());
    }

    public int closeAudio(String peerUin) {
        mIsAudioCall = false;
        return closeVideo(peerUin);
    }

    public int closeVideo(String peerUin) {
        if (mVcCtrl == null) {
            return -1;
        }
        mMapUinPending.remove(peerUin);
        abandonAudioFocus();

        if (tmCommunication != null) {
            tmCommunication.cancel();
            tmCommunication = null;
        }

        return mVcCtrl.closeVideo(peerUin, VideoConstants.VOIP_REASON_CLOSED_BY_SELF);
    }

    public boolean isSharp() {
        if (mVcCtrl == null) {
            return false;
        }
        return mVcCtrl.isSharp();
    }

    public void pauseVideo(String peerUin) {
        if (mVcCtrl == null) {
            mVcCtrl.pauseVideo(peerUin);
        }
    }

    public void resumeVideo(String peerUin) {
        GraphicRenderMgr.getInstance().clearCameraFrames();
        if (mVcCtrl == null) {
            mVcCtrl.resumeVideo(peerUin);
        }
    }

    public void setEncodeDecodePtr(boolean clean) {
        GraphicRenderMgr graphicRenderMgr = GraphicRenderMgr.getInstance();
        if (null != mVcCtrl) {
            int ptrDecoder = clean ? 0 : graphicRenderMgr.getRecvDecoderFrameFunctionptr();
            mVcCtrl.setProcessDecoderFrameFunctionptr(ptrDecoder);
            int ptrEncoder = clean ? 0 : mVcCtrl.getEncodeFrameFunctionPtrFunPtr();
            graphicRenderMgr.setProcessEncodeFrameFunctionPtr(ptrEncoder);
        }
    }

    public void setSenderRenderPtr() {
        VideoController.setVideoFrameSenderFuncPtr(mVcCtrl != null ? mVcCtrl.getVideoFrameSenderFuncPtr() : 0);
        VideoController.setVideoFrameRenderFuncPtr(GraphicRenderMgr.getInstance().getVideoFrameRenderFuncPtr());
    }

    public void OnPreviewData(byte[] data, int angle, long SPF, boolean isFront) {
        if (data == null) {
            return;
        }
        int datalen = data.length;

        if (datalen != AndroidCamera.PREVIEW_WIDTH
                * AndroidCamera.PREVIEW_HEIGHT * 3 / 2) {
            if (QLog.isColorLevel())
                QLog.d("OnPreviewData", QLog.CLR, "datalen != preview size");
            if (datalen == 640 * 480 * 3 / 2) {
                AndroidCamera.PREVIEW_WIDTH = 640;
                AndroidCamera.PREVIEW_HEIGHT = 480;
            }

            if (datalen == 320 * 240 * 3 / 2) {
                AndroidCamera.PREVIEW_WIDTH = 320;
                AndroidCamera.PREVIEW_HEIGHT = 240;
            }
        }
        QLog.d("OnPreviewData", QLog.CLR, "format:" + AndroidCamera.PREVIEW_FORMAT + " width:" + AndroidCamera.PREVIEW_WIDTH + " height:" + AndroidCamera.PREVIEW_HEIGHT);
        GraphicRenderMgr.getInstance().sendCameraFrame2Native(data,
                AndroidCamera.PREVIEW_FORMAT, AndroidCamera.PREVIEW_WIDTH,
                AndroidCamera.PREVIEW_HEIGHT, angle,
                System.currentTimeMillis(), isFront);
    }

    ////////////////////////////////////Sharp Msg Channel begin /////////////////////////////////////////
    private void sendVideoCall(long peerUin, int uinType, byte[] msg) {
        if (TXDeviceService.VideoProcessEnable) {
            try {
                if (mTXDeviceService != null) {
                    mTXDeviceService.sendVideoCall(peerUin, uinType, msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            TXDeviceService.nativeSendVideoCall(peerUin, uinType, msg);
        }
    }

    private void sendVideoCallM2M(long peerUin, int uinType, byte[] msg) {
        if (TXDeviceService.VideoProcessEnable) {
            try {
                if (mTXDeviceService != null) {
                    mTXDeviceService.sendVideoCallM2M(peerUin, uinType, msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            TXDeviceService.nativeSendVideoCallM2M(peerUin, uinType, msg);
        }
    }

    private void sendVideoCMD(long peerUin, int uinType, byte[] msg) {
        if (TXDeviceService.VideoProcessEnable) {
            try {
                if (mTXDeviceService != null) {
                    mTXDeviceService.sendVideoCMD(peerUin, uinType, msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            TXDeviceService.nativeSendVideoCMD(peerUin, uinType, msg);
        }
    }

    private long nativeGetSelfDin() {
        if (TXDeviceService.VideoProcessEnable) {
            try {
                if (mTXDeviceService != null) {
                    return mTXDeviceService.getSelfDin();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return TXDeviceService.nativeGetSelfDin();
        }
        return 0;
    }

    private byte[] getVideoChatSignature() {
        if (TXDeviceService.VideoProcessEnable) {
            try {
                if (mTXDeviceService != null) {
                    return mTXDeviceService.getVideoChatSignature();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return TXDeviceService.nativeGetVideoChatSignature();
        }
        return null;
    }

    public byte onSendVideoCall(byte[] msg) {
        if (g_Instance != null) {
            return g_Instance.receiveVideoCall(nativeGetSelfDin(), msg, NULL);
        }
        return RecvFail;
    }

    public byte onSendVideoCallM2M(byte[] msg) {
        if (g_Instance != null) {
            return g_Instance.receiveVideoCallM2M(nativeGetSelfDin(), msg, NULL);
        }
        return RecvFail;
    }

    public byte onSendVideoCMD(byte[] msg) {
        if (g_Instance != null) {
            return g_Instance.receiveSharpVideoAck(nativeGetSelfDin(), msg, NULL);
        }
        return RecvFail;
    }

    public byte onReceiveVideoBuffer(byte[] msg, long sendUin, int sendUinType) {
        mMapUinType.put(String.valueOf(sendUin), sendUinType);
        if (g_Instance != null) {
            return g_Instance.receiveSharpVideoCall(nativeGetSelfDin(), msg, NULL);
        }
        return RecvFail;
    }
    ////////////////////////////////////Sharp Msg Channel end /////////////////////////////////////////

    //////////////////////////////////// LANCommunication begin ///////////////////////////////////////
    public static final String OnRecvDeviceRespond = "OnRecvDeviceRespond";
    public static final String OnDeviceScanTimeOut = "OnDeviceScanTimeOut";
    public static final String OnDeviceConnTimeOut = "OnDeviceConnTimeOut";
    public static final String OnDeviceConnSuccess = "OnDeviceConnSuccess";

    public static final int LanDeviceConnectType_ZQL = 1;        //for 朱雀�?

    public native void initLANCommunication(long din);

    public static native void startDeviceScan(int timeout);

    public static native void notifyDeviceConnect(long din, int timeout, int nType);

    public static native void closeDeviceChannel(long din);

    public static native void startVideoStream(long din);

    public static native void stopVideoStream(long din);

    public static native void startAudioStream(long din, int nSample);

    public static native void stopAudioStream(long din);

    public static native void setVideoBitrate(long din, int nBitrate);

    public static native void requestIFrame(long din);

    public static native void recvLANCommunicationReply(byte[] buffer);

    public static native void setVideoFrameSenderFuncPtr(int ptr);

    public static native void setVideoFrameRenderFuncPtr(int ptr);

    public static native void setCurrentPeerTinyId(long tinyid);


    private void onRecvDeviceRespond(long din, byte[] remark, byte[] headUrl) {
        String strRemark = "";
        String strHeadUrl = "";
        try {
            strRemark = new String(remark, "UTF-8");
            strHeadUrl = new String(headUrl, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent();
        intent.setAction(VideoController.OnRecvDeviceRespond);
        intent.putExtra("din", din);
        intent.putExtra("remark", strRemark);
        intent.putExtra("headUrl", strHeadUrl);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }

    private void onDeviceScanTimeOut() {
        Intent intent = new Intent();
        intent.setAction(VideoController.OnDeviceScanTimeOut);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }

    private void onDeviceConnectTimeOut(long din, int type) {
        Intent intent = new Intent();
        intent.setAction(VideoController.OnDeviceConnTimeOut);
        intent.putExtra("din", din);
        intent.putExtra("type", type);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }

    private void onDeviceConnectSuccess(long din, int type) {
        Intent intent = new Intent();
        intent.setAction(VideoController.OnDeviceConnSuccess);
        intent.putExtra("din", din);
        intent.putExtra("type", type);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }

    private void onLANAVRecvAudioData(long din, byte[] data) {
        if (data != null && data.length > 0) {
            VideoController.getInstance().writeAudioData(data);
        }
    }

    private void onSendLANCommunicationCMD(byte[] data) {
        if (TXDeviceService.VideoProcessEnable) {
            try {
                if (mTXDeviceService != null) {
                    mTXDeviceService.sendLANCommunicationCMD(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            TXDeviceService.sendLANCommunicationCMD(data);
        }
    }


    public void setVideoBitrate(int nBitrate) {
        setVideoBitrate(mConnectedCameraDin, nBitrate);
    }

    public void requestIFrame() {
        requestIFrame(mConnectedCameraDin);
    }

    public void closeLocalAVSession() {
        stopVideoStream(mConnectedCameraDin);
        stopAudioStream(mConnectedCameraDin);
        emptyAudioData();
        closeDeviceChannel(mConnectedCameraDin);
        setConnectedCameraDin(0);
    }

    ////////////////////////////////////LANCommunication end //////////////////////////////////////

    ////////////////////////////////////AbstractNetChannel begin/////////////////////////////////////////
    @Override
    public void sendVideoCall(byte[] msg, long peerUin) {
        //过滤后台解析错误的sharp�?
//		int uinType = UINTYPE_QQ;
//		if (mMapUinType.containsKey(String.valueOf(peerUin))){
//			uinType = mMapUinType.get(String.valueOf(peerUin));
//		}
//		sendVideoCall(peerUin, uinType, msg);
    }

    @Override
    public void sendVideoCallM2M(byte[] msg, long peerUin) {
        //过滤后台解析错误的sharp�?
//		int uinType = UINTYPE_QQ;
//		if (mMapUinType.containsKey(String.valueOf(peerUin))){
//			uinType = mMapUinType.get(String.valueOf(peerUin));
//		}
//		sendVideoCallM2M(peerUin, uinType, msg);
    }

    @Override
    public void sendVideoConfigReq(byte[] msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void sendSharpCMD(byte[] msg, long peerUin) {
        int uinType = UINTYPE_QQ;
        if (mMapUinType.containsKey(String.valueOf(peerUin))) {
            uinType = mMapUinType.get(String.valueOf(peerUin));
        }
        sendVideoCMD(peerUin, uinType, msg);
    }

    @Override
    public void sendMultiVideoCMD(long groupId, long csCmd, byte[] msg) {
        // TODO Auto-generated method stub

    }

    ////////////////////////////////////AbstractNetChannel end/////////////////////////////////////////


    ////////////////////////////////////IVideoEventListener begin/////////////////////////////////////////
    @Override
    public void onRequestVideo(int uinType, String fromUin, String extraUin,
                               byte[] sig, boolean onlyAudio, String bindID, int bindType) {
        requestAudioFocus();
        LoggerUtils.Companion.d(TAG + "onRequestVideo");

        if (hasPendingChannel()) {
            rejectRequest(fromUin);
        } else {
            if (mContext != null) {

                QLog.d(TAG, QLog.CLR, "recv video monitor request");
                Intent intent = new Intent(mContext, BooyueVideoMonitorService.class);
                intent.putExtra("peerid", fromUin);
                LoggerUtils.Companion.d(TAG + "onRequestVideo VideoMonitorService");
                mContext.startService(intent);

//                if (Long.parseLong(bindID) == 4100) {
//                    QLog.d(TAG, QLog.CLR, "recv video monitor request");
//                    Intent intent = new Intent(mContext, VideoMonitorService.class);
//                    intent.putExtra("peerid", fromUin);
//                    LoggerUtils.d(TAG + "onRequestVideo VideoMonitorService");
//                    mContext.startService(intent);
//                } else if (onlyAudio) {
////                    Intent intent = new Intent(mContext, AudioChatActivity.class);
//                    Intent intent = new Intent(mContext, BooyueAudioChatActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    intent.putExtra("receive", true);
//                    intent.putExtra("peerid", fromUin);
//                    intent.putExtra("dinType", uinType);
//                    LoggerUtils.d(TAG + "onRequestVideo AudioChatActivity");
//                    mContext.startActivity(intent);
//                } else {
//                    QLog.d(TAG, QLog.CLR, "recv video chat request");
//                    Intent intent = null;
//                    if (VideoController.getInstance().isHasLocalCam()) {
//                        if (VideoController.isHardwareEncoderEnabled()) {
//                            intent = new Intent(mContext, VideoChatActivityHW.class);
//                            LoggerUtils.d(TAG + "onRequestVideo VideoChatActivityHW");
//                        } else {
////                            intent = new Intent(mContext, VideoChatActivitySF.class);
//                            intent = new Intent(mContext, BooyueVideoChatActivitySF.class);
////                            intent = new Intent(mContext, BooyueFriendInfoActivity.class);
//                            LoggerUtils.d(TAG + "onRequestVideo VideoChatActivitySF");
//                        }
//                    } else {
//                        intent = new Intent(mContext, VideoChatActivityNFC.class);
//                        LoggerUtils.d(TAG + "onRequestVideo VideoChatActivityNFC");
//                    }
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    intent.putExtra("receive", true);
//                    intent.putExtra("peerid", fromUin);
//                    mContext.startActivity(intent);
//                }
            }

            mMapUinPending.put(fromUin, false);
        }
    }

    @Override
    public void onRejectVideo(String fromUin) {

    }

    @Override
    public void onCancelRequest(String fromUin) {

    }

    @Override
    public void onAcceptedVideo(String fromUin) {

    }

    @Override
    public void onChannelReady(final String fromUin) {
//		//声音从听筒放�?
//		mTraeHelper.startService(TraeAudioManager.VOICECALL_CONFIG);
//		mTraeHelper.connectDevice(TraeAudioManager.DEVICE_EARPHONE);

        //声音喇叭放出
        mTraeHelper.startService(TraeAudioManager.VIDEO_CONFIG);
        mTraeHelper.connectDevice(TraeAudioManager.DEVICE_SPEAKERPHONE);

        mMapUinPending.put(fromUin, true);
        Intent intent = new Intent(VideoController.ACTION_CHANNEL_READY);
        intent.putExtra("uin", fromUin);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }

        /**
         * 发送指�?是否支持开锁功能，operation = 6, value = 0x01)
         * Value是各属性值取或运算，例如�?x01代表支持开锁，0x02代表支持屏幕，则value=0x03
         */
        sendTransferMsg(Long.parseLong(fromUin), 6, 0x01);
        Log.d("VideoController", "onChannelReady");
        strPeerUin = fromUin;
        nOnTimeCnt = 0;
        if (tmCommunication != null) {
            tmCommunication.schedule(tmTaskCommunication, TIME_TO_START_REPORT * 1000, TIME_INTERVAL_REPORT * 1000);
        }
    }

    @Override
    public void onRecvVideoData(String fromUin, byte[] data, int frmAngle,
                                int width, int height, int colorFmt) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCloseVideo(String fromUin, int reason, long extraParam) {
        mTraeHelper.stopSerivce();
        abandonAudioFocus();
        Intent intent = new Intent();
        intent.setAction(VideoConstants.ACTION_STOP_VIDEO_CHAT);
        intent.putExtra("uin", fromUin);
        intent.putExtra("reason", reason);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }

        mMapUinPending.remove(fromUin);
    }

    @Override
    public void onPauseVideo(String fromUin) {

    }

    @Override
    public void onResumeVideo(String fromUin) {

    }

    @Override
    public void onPauseAudio(String fromUin) {

    }

    @Override
    public void onResumeAudio(String fromUin) {

    }

    @Override
    public void onApptypeNotSuit(String fromUin) {

    }

    @Override
    public void onNeedShowPeerVideo() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAnotherHaveReject(String fromUin) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAnotherHaveAccept(String fromUin) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConfigSysDealDone(String fromUin) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAVShiftEvent(int type, String fromUin) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAnotherIsRing(String fromUin, boolean isCalling) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOldRequestNotSupportSharp(String fromUin) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNotRecvAudioData(boolean bNotRecv) {
        // TODO Auto-generated method stub
        remoteMute = true;
    }

    @Override
    public void onRecvFirstAudioData(boolean recvFirstAudio, String fromUin) {
        remoteMute = false;
    }

    @Override
    public void onMediaCameraNotify(byte[] detail, long info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onInviteReached(String peerUin, int friend_state,
                                long extraParam0, byte[] detail) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDetectAudioDataIssue(int issueType) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOtherTerminalChatingStatus(String fromUin, long roomid,
                                             int type) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPeerSwitchTerninal(String fromUin) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSyncOtherTerminalChatStatus(String fromUin, int time) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSwitchTerminalSuccess(String fromUin, int info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPeerSwitchTerminalFail(String fromUin, int info) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onChangePreviewSize(int w, int h) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSendC2CMsg(String fromUin) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNetworkDisconnect(String fromUin) {
        if (QLog.isColorLevel())
            QLog.d(TAG, QLog.CLR, "onNetworkDisconnect fromUin = " + fromUin);
        Intent intent = new Intent();
        intent.setAction(VideoConstants.ACTION_STOP_VIDEO_CHAT);
        intent.putExtra("uin", fromUin);
        intent.putExtra("reason", VideoConstants.VOIP_REASON_NETWORK_DISCONNECT);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public int getAPAndGateWayIP() {
        return 0;
    }

    @Override
    public void onNetworkMonitorInfo(String fromUin, byte[] detail, long info) {
        String msg = null;
        if (info == 1) {
            try {
                msg = new String(detail, "GBK");
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (info == 0) {
            msg = new String(detail);
        }

        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, msg);
        }

        Intent intent = new Intent(VideoController.ACTION_NETMONIOTR_INFO);
        intent.putExtra("uin", fromUin);
        intent.putExtra("msg", msg);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void dataTransfered(int direction, long size) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNetworkInfo_S2C(String fromUin, byte[] detail, long flag) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSwitchGroup(String fromUin, byte[] detail, long flag) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSelfNetLevel(int level) {

    }

    @Override
    public void onNetLevel_S2C(String fromUin, int level) {
        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, "s2c level : " + level);
        }
        if (level < 0) {
            return;
        }
        /**
         * 服务端下发网络等级字段level
         * 5-7位表示本端网络
         * 8-10位表示对端网络
         * 0x01良好 0x10一般 0x11较差
         * 0x00 初始值，一般不会出现，出现就认为良好
         */
        int s2cSelfNetLevel = (level >> 4) & 0x7;
        int s2cPeerNetLevel = (level >> 7) & 0x7;

        // 一方关闭了麦克风就会导致另一方数据不准确了,静音就忽略
        if (remoteMute) {
            s2cSelfNetLevel = 1;
        }
        if (selfMute) {
            s2cPeerNetLevel = 1;
        }

        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, "s2c by changed self level : " + s2cSelfNetLevel + ",peer level:" + s2cPeerNetLevel);
        }

        String displayNetState = null;

        if (s2cSelfNetLevel == 3) {
            displayNetState = "你的信号不稳定";
        } else if (s2cPeerNetLevel == 3) {
            displayNetState = "对方信号不稳定";
        }
        if (displayNetState != null) {
            QLog.d(TAG, QLog.CLR, displayNetState);
        }

        if (mContext != null) {
            Intent intent = new Intent(VideoController.ACTION_NET_LEVEL);
            intent.putExtra("selfNetLevel", s2cSelfNetLevel);
            intent.putExtra("peerNetLevel", s2cPeerNetLevel);
            intent.putExtra("displayNetState", displayNetState);
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void onReceiveVideoFrame(byte[] buffer, int angle) {
        Log.d(TAG, "ReceiveVideoFrame: len = " + buffer.length + " angle = " + angle);

        Intent intent = new Intent(VideoController.ACTION_VIDEOFRAME_INCOME);
        intent.putExtra("angle", angle);
        intent.putExtra("buffer", buffer);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void onNotifyVideoQos(int width, int height, int bitrate, int fps) {
        Log.d(TAG, "onNotifyVideoQos: width =" + width + " height =" + height + " bitrate = " + bitrate + " fps = " + fps);
        Intent intent = new Intent(VideoController.ACTION_VIDEO_QOS_NOTIFY);
        intent.putExtra("width", width);
        intent.putExtra("height", height);
        intent.putExtra("bitrate", bitrate);
        intent.putExtra("fps", fps);
        if (mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void receiverTransferMsg(final String fromUin, int type, byte[] buffer) {
        if (type == QQ_DEVICE_CONNECT_TYPE) {
            VcControllerImpl.DeviceCMDTLV cmdtlv = mVcCtrl.unpackTLV(buffer);
            Log.d(TAG, "receiverTransferMsg: operation =" + cmdtlv.operation + " value =" + cmdtlv.opvalue);
        }
    }

    @Override
    public void onFrameCountInLast3Second(int frameCount) {
        if (frameCount < 25) {
            if (mContext != null) {
                Intent intent = new Intent(VideoController.ACTION_NET_BAD);
                mContext.sendBroadcast(intent);
            }
        }
    }

    //###################### Audio Focus ###########################

    AudioManager mAudioMgr = null;
    AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = null;

    @SuppressLint("NewApi")
    void requestAudioFocus() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
            return;
        }
        if (mContext == null) {
            return;
        }
        if (mAudioFocusChangeListener == null) {
            mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        // Stop playback
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        // Pause playback
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        // Lower the volume
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // Rusume playback or Raise it back normal
                    }
                }
            };
        }
        if (mAudioMgr == null) {
            mAudioMgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        if (mAudioMgr != null) {
            int ret = mAudioMgr.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (ret != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (QLog.isColorLevel())
                    QLog.d("AudioManager", QLog.CLR, "request audio focus fail. " + ret);
            }
        }
    }

    @SuppressLint("NewApi")
    void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
            return;
        }
        if (mAudioMgr != null) {
            mAudioMgr.abandonAudioFocus(mAudioFocusChangeListener);
            mAudioMgr = null;
        }
    }

    // 异步执行的专用线�?
    public void execute(final Runnable r) {
        mThreadPool.submit(new Job<Boolean>() {
            @Override
            public Boolean run(JobContext jc) {
                r.run();
                return true;
            }
        });
    }

    public void execute(final Runnable r, FutureListener<Boolean> listener) {
        mThreadPool.submit(new Job<Boolean>() {
            @Override
            public Boolean run(JobContext jc) {
                r.run();
                return true;
            }
        }, listener);
    }

    // 播放铃声：使用MediaPlayer
    MediaPlayer mediaPlayer = null;
    int loopCount = 0;

    public void startRing(int resId, final int loop, final OnCompletionListener listener) {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    return;
                } else {
                    try {
                        mediaPlayer.release();
                    } catch (Exception e) {
                    } finally {
                        mediaPlayer = null;
                    }
                }
            }

            if (mContext != null) {
                mediaPlayer = MediaPlayer.create(mContext, resId);
            }

            if (mediaPlayer == null) {
                return;
            }

            loopCount = loop;
            if (loopCount != 0) {
                loopCount--;
            }

            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (loopCount == 0) {
                        if (listener != null) {
                            listener.onCompletion(mp);
                        }
                    } else {
                        loopCount--;
                        mediaPlayer.start();
                    }
                }
            });
            mediaPlayer.start();
            mediaPlayer.setLooping(false);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    public void stopRing() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (Exception e) {

        }
        mediaPlayer = null;
    }

    // 播放铃声：使用SoundPool
    SoundPool mSoundPool = null;
    HashMap<Integer, Integer> mSoundRes = new HashMap<Integer, Integer>();
    int mResIdPlaying = -1;

    public void startRing2(int resId, final int loop) {
        if (null == mSoundPool) {
            mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        }

        int soundId = -1;
        if (mSoundRes.containsKey(resId)) {
            soundId = mSoundRes.get(resId);
        } else {
            soundId = mSoundPool.load(getContext(), resId, 1);
            mSoundRes.put(resId, soundId);
        }

        mResIdPlaying = mSoundPool.play(soundId, 1, 1, 1, loop, 1);
    }

    public void stopRing2() {
        if (mSoundPool != null) {
            if (mResIdPlaying != -1) {
                mSoundPool.stop(mResIdPlaying);
            }
        }

        mResIdPlaying = -1;
    }

    public void startShake() {
        if (mTraeHelper != null && mContext != null) {
            mTraeHelper.startShake(mContext, true);
        }
    }

    public void stopShake() {
        if (mTraeHelper != null && mContext != null) {
            mTraeHelper.stopShake(mContext);
        }
    }

    public void sendEncodedVideoFrame(long peerUin, byte[] data, int frameType, int gopIndex, int frameIndex, long timestamp) {
        if (peerUin != 0 && mMapUinPending.containsKey(String.valueOf(peerUin)) && mMapUinPending.get(String.valueOf(peerUin)) == true) {
            if (mVcCtrl != null) {
                mVcCtrl.sendVideoFrame(peerUin, data, frameType, gopIndex, frameIndex, timestamp);
            }
        }
    }

    public void exitProcess() {
        new Thread() {
            public void run() {
                try {
                    sleep(800);
                    if (mContext != null) {
                        mContext.stopService(new Intent(mContext, VideoService.class));
                    }
                    Process.killProcess(Process.myPid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private long mConnectedCameraDin = 0;

    public void setConnectedCameraDin(long din) {
        if (din == 0) {
            mConnectedCameraDin = din;
        } else if (mConnectedCameraDin == 0) {
            mConnectedCameraDin = din;
            startAudioStream(mConnectedCameraDin, mSuggestedAudioSampleRate);
        }
    }

    private int mSuggestedAudioSampleRate = 0;

    public void setSuggestedAudioSampleRate(int rate) {
        if (rate == 0) {
            mSuggestedAudioSampleRate = rate;
        } else if (mSuggestedAudioSampleRate == 0) {
            mSuggestedAudioSampleRate = rate;
            startAudioStream(mConnectedCameraDin, mSuggestedAudioSampleRate);
        }
    }

    private int mBufMaxLength = 200 * 320;
    private byte[] mCacheBuf = new byte[mBufMaxLength];
    private int mCacheBufIndexStart = 0;
    private int mCacheBufIndexEnd = 0;
    private Object mSyncObject = new Object();
    private Semaphore mSemp = new Semaphore(0);

    public void emptyAudioData() {
        synchronized (mSyncObject) {
            mCacheBufIndexStart = 0;
            mCacheBufIndexEnd = 0;
            mCacheBuf = null;
            mCacheBuf = new byte[mBufMaxLength];
            mNeedWakeup = true;
        }
        mReadDataCnt = 0;
        mLastReadDataTime = 0;
        QLog.d("lanaudiodata", QLog.DEV, "emptyAudioData, clear all data");
    }

    public void writeAudioData(byte[] audioData) {
        //QLog.d("lanaudiodata", QLog.DEV, "writeAudioData to lock");
        synchronized (mSyncObject) {
            //QLog.d("lanaudiodata", QLog.DEV, "writeAudioData locked");
            if (mCacheBufIndexEnd + audioData.length >= mBufMaxLength) {
                mCacheBufIndexEnd = mCacheBufIndexEnd - mCacheBufIndexStart;
                System.arraycopy(mCacheBuf, mCacheBufIndexStart, mCacheBuf, 0, mCacheBufIndexEnd);
                mCacheBufIndexStart = 0;
                if (mCacheBufIndexEnd + audioData.length >= mBufMaxLength) {
                    // 数据太多直接丢弃这一�?
                    QLog.d("lanaudiodata", QLog.DEV, "writeAudioData, cache data failed, data cache is full");
                } else {
                    System.arraycopy(audioData, 0, mCacheBuf, mCacheBufIndexEnd, audioData.length);
                    mCacheBufIndexEnd = mCacheBufIndexEnd + audioData.length;
                }
            } else {
                System.arraycopy(audioData, 0, mCacheBuf, mCacheBufIndexEnd, audioData.length);
                mCacheBufIndexEnd = mCacheBufIndexEnd + audioData.length;
            }

            if (mNeedWakeup) {
                mSemp.release();
                QLog.d("lanaudiodata", QLog.DEV, "writeAudioData, release semp");
                mNeedWakeup = false;
            }
            //QLog.d("lanaudiodata", QLog.DEV, "writeAudioData, cache size:"+((mCacheBufIndexEnd - mCacheBufIndexStart)/320));
        }
        //QLog.d("lanaudiodata", QLog.DEV, "writeAudioData to unlock");
    }

    private long mLastReadDataTime = 0;
    private long mReadDataCnt = 0;
    private long mLogCnt = 1;
    private int mCacheCnt = 10;
    private boolean mNeedWakeup = true;

    public int readAudioData(byte[] audioData, int sizeInBytes) {
        return readAudioData2(audioData, sizeInBytes);
    }

    public int readAudioData2(byte[] audioData, int sizeInBytes) {
        //动态调整，初始缓存100ms数据，在100ms附近动态调整，线性增加与减少sleep时间
        //for first cache
        if (0 == mLastReadDataTime) {
            if (mCacheBufIndexEnd < 320) {
                //QLog.d("lanaudiodata", QLog.DEV, "readAudioData, mCacheBufIndexEnd:"+mCacheBufIndexEnd);
                try {
                    if (!mSemp.tryAcquire(1, TimeUnit.SECONDS)) {
                        QLog.d("lanaudiodata", QLog.DEV, "readAudioData, tryAcquire1 failed");
                        return 0;
                    }
                    //Thread.sleep(1);//sleep 0.1ms
                } catch (Exception e) {
                }
            } else {
                mLastReadDataTime = System.nanoTime() / 100000;
                QLog.d("lanaudiodata", QLog.DEV, "readAudioData, FirstCacheOverAtTime:" + mLastReadDataTime + " mCacheBufIndexEnd:" + mCacheBufIndexEnd);
            }
        }

        //for time control
        long nowReadDataTime = 0;
        while (true) {
            nowReadDataTime = System.nanoTime() / 100000;
            long diffTime = (nowReadDataTime - mLastReadDataTime);
            long temp = (mCacheBufIndexEnd - mCacheBufIndexStart) / 320;
            temp = temp - mCacheCnt;

            long needSleepTime = 0;
            if (temp > 0) {//y=-1.5x+100   x[10,40] y[100,40] unit:0.1ms
                double sleepTime = -1.5 * temp + 100;
                needSleepTime = (long) sleepTime;
                if (needSleepTime < 10) needSleepTime = 10;
            } else {
                needSleepTime = -10 * temp + 100;
            }
            //QLog.d("lanaudiodata", QLog.DEV, "readAudioData, needSleepTime:"+needSleepTime+"  diffTime:" + diffTime);
            if (diffTime >= needSleepTime) {
                break;
            } else {
                try {
                    Thread.sleep(0, 100000);
                    //return 0;
                } catch (Exception e) {
                }
            }
        }

        boolean bShouldWait = false;
        synchronized (mSyncObject) {
            if (mCacheBufIndexStart + sizeInBytes > mCacheBufIndexEnd) {
                QLog.d("lanaudiodata", QLog.DEV, "readAudioData, read failed1, no data to read, read cnt:" + mReadDataCnt);
                mNeedWakeup = true;
                bShouldWait = true;
            } else {
                mNeedWakeup = false;
                System.arraycopy(mCacheBuf, mCacheBufIndexStart, audioData, 0, sizeInBytes);
                mCacheBufIndexStart = mCacheBufIndexStart + sizeInBytes;
                mReadDataCnt++;
            }
        }

        if (bShouldWait) {
            try {
                if (!mSemp.tryAcquire(3, TimeUnit.SECONDS)) {
                    QLog.d("lanaudiodata", QLog.DEV, "readAudioData, tryAcquire2 failed");
                    return 0;
                }
            } catch (Exception e) {
            }

            synchronized (mSyncObject) {
                //QLog.d("lanaudiodata", QLog.DEV, "readAudioData locked");
                if (mCacheBufIndexStart + sizeInBytes > mCacheBufIndexEnd) {
                    QLog.d("lanaudiodata", QLog.DEV, "readAudioData, read failed2, no data to read, read cnt:" + mReadDataCnt);
                    return 0;
                }

                System.arraycopy(mCacheBuf, mCacheBufIndexStart, audioData, 0, sizeInBytes);
                mCacheBufIndexStart = mCacheBufIndexStart + sizeInBytes;
                mReadDataCnt++;
                //QLog.d("lanaudiodata", QLog.DEV, "readAudioData, read cnt:"+mReadDataCnt+"cache size:" + (mCacheBufIndexEnd - mCacheBufIndexStart));
            }
        }

        //for statistic
        long jitterNum = (mCacheBufIndexEnd - mCacheBufIndexStart) / 320;
        nowReadDataTime = System.nanoTime() / 100000;
        float diffTime = (nowReadDataTime - mLastReadDataTime) / 10;
        if (jitterNum > 20 || jitterNum < 5) {
            mLogCnt++;
            if (0 == mLogCnt % 5) {
                QLog.d("lanaudiodata", QLog.DEV, "readAudioData unlock jitterNum[" + (jitterNum) + "] diff:[" + diffTime + "ms]");
            }
        }
        mLastReadDataTime = nowReadDataTime;

        return sizeInBytes;
    }

    /**
     * 双人音视频信令发
     */
    public void sendTransferMsg(long llFriendUIN, int operation, int value) {
        try {
            byte[] buffer = mVcCtrl.createTLVpackage(llFriendUIN, "", (byte) operation, value);
            mVcCtrl.sendTransferMsg(llFriendUIN, QQ_DEVICE_CONNECT_TYPE, buffer);
            QLog.e("lanaudiodata", QLog.DEV, "sendTransferMsg");
        } catch (Throwable e) {

        }
    }

    public void setVideoModeType(long llFriendUIN, int type) {
        mCurrentDefinitionType = type;
        mVcCtrl.setVideoModeType(llFriendUIN, type);
    }

    // Toast提示
    private void showToastMessage(final String strMsg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Toast.makeText(mContext, strMsg, Toast.LENGTH_SHORT).show();
            }

        });
    }

    public FriendInfo getFriendInfo(String uin) {
        if (TXDeviceService.VideoProcessEnable) {
            try {
                if (mTXDeviceService != null) {
                    return mTXDeviceService.getFriendInfo(uin);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return TXDeviceService.getFriendInfo(uin);
        }
        return new FriendInfo(uin);
    }

    /**
     * 如果自己静音了，应该调用这个方法
     *
     * @param mute
     */
    public void setSelfMute(boolean mute) {
        selfMute = mute;
    }

    public boolean isSelfMute() {
//        return selfMute;
        return audioManager.isMicrophoneMute();
    }

    /**
     * tencen提供的setSelfMute无作用
     *
     * @param mute
     */
    public void setSelfMute2(boolean mute) {
        selfMute = mute;
        audioManager.setMicrophoneMute(mute);
    }
}
