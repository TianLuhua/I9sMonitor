package com.tencent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.av.VideoController;
import com.tencent.av.core.VideoConstants;
import com.tencent.device.FriendInfo;
import com.tencent.device.QLog;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;
import com.tencent.devicedemo.ListItemInfo;
import com.booyue.monitor.R;
import com.tencent.util.ImageUtils;
import com.tencent.util.LoggerUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lingyuhuang on 2016/10/8.
 * 语音聊天demo
 * 音频聊天界面
 */

public class BooyueAudioChatActivity extends BaseActivity {

    public static final String TAG = "BooyueAudioChatActivity";

//    private ImageView ivBtnAccept;
//    private ImageView ivBtnHangup;

    private boolean mIsReceiver;// 是发起方
    //伙伴id
    private String mPeerId;
    //din类型
    private int mDinType;
    //自身的din
    private String mSelfDin;
    //开始通话时间
    private long startTime;

    private BroadcastHandler mBroadcastHandler;

    private Handler mHandler = new Handler();

    //扬声器切换
    private ImageButton ibSpeakerSwitcher;
    private CircleImageView ivAvatar;
    private TextView tvName;
    private TextView tvStateDesc;
    private TextView tvDuration;
    private ImageButton ibCancel;
    private LinearLayout llReciever;
    private ImageButton ibHangup;
    private ImageButton ibReceive;
    private Handler mHandler1;
    private Set<Long> mSetFetching = new HashSet<>();
    private ImageUtils imageUtils;
    //是否接通
    private boolean isChannelReady = false;


    @Override
    public void setView() {
        setContentView(R.layout.activity_audio_chat_booyue);

        Intent intent = getIntent();

        mPeerId = intent.getStringExtra("peerid");
        mDinType = intent.getIntExtra("dinType", VideoController.UINTYPE_QQ);
        mSelfDin = VideoController.getInstance().GetSelfDin();
        //判断是发起还是接收
        mIsReceiver = intent.getBooleanExtra("receive", false);
        if (Long.parseLong(mPeerId) == 0 || Long.parseLong(mSelfDin) == 0) {
            QLog.e(TAG, QLog.CLR, "invalid peerId: " + mPeerId + " invalid selfDin: " + mSelfDin);
            finish();
        }
        //                FriendListAdapter.this.notifyDataSetChanged();
    }

    @Override
    public void initView() {
        tvName = (TextView) findViewById(R.id.tv_name);
        tvDuration = (TextView) findViewById(R.id.tv_duration);
        tvDuration.setVisibility(View.GONE);

        final FriendInfo friendInfo = VideoController.getInstance().getFriendInfo(mPeerId);
        tvName.setText(friendInfo.getName());
        String devName = friendInfo.devName;
        int devType = friendInfo.devType;
        LoggerUtils.d(TAG + "deviceName = " + devName +",deviceType = " + devType);


        /**
         * 先隐藏，只有接通之后才显示
         */
//        ibSpeaker = (ImageButton) findViewById(R.id.ib_speaker);
        ibSpeakerSwitcher = (ImageButton) findViewById(R.id.ib_speaker_switcher);
//        ibSpeaker.setVisibility(View.GONE);
        ibSpeakerSwitcher.setVisibility(View.GONE);
        //扬声器切换
        /**modify by : 2018/6/22 9:09 由于扬声器无效，去掉图标*/
//        ibSpeakerSwitcher.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (VideoController.getInstance().isSelfMute()) {//如果是静音
//                    //切换成非静音
//                    VideoController.getInstance().setSelfMute(false);
//                    ibSpeakerSwitcher.setImageResource(R.drawable.button_speaker_hi);
//                } else {//如果是静音
//                    //切换成静音
//                    VideoController.getInstance().setSelfMute(true);
//                    ibSpeakerSwitcher.setImageResource(R.drawable.button_speaker_nr);
//                }
//            }
//        });
        tvStateDesc = (TextView) findViewById(R.id.tv_state_desc);
        ivAvatar = (CircleImageView) findViewById(R.id.iv_avatar);
        //针对T6机器头像有点偏下，所以需要稍微调整一下
        if (TextUtils.equals(SerialNumberManager.T6_ID, Build.ID)) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) ivAvatar.getLayoutParams();
            layoutParams.topMargin = (int)getResources().getDimension(R.dimen.dimen_34);
            ivAvatar.setLayoutParams(layoutParams);
        }
        mHandler1 = new Handler(this.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
//                FriendListAdapter.this.notifyDataSetChanged();
                setBitmapToImageView(friendInfo);
            }
        };
        imageUtils = new ImageUtils(mHandler1, mSetFetching);
        setBitmapToImageView(friendInfo);


        ibCancel = (ImageButton) findViewById(R.id.tv_cancel);
        llReciever = (LinearLayout) findViewById(R.id.ll_recieve_before);
        ibHangup = (ImageButton) findViewById(R.id.tv_hangup);
        ibReceive = (ImageButton) findViewById(R.id.tv_receive);
        if (!mIsReceiver) {
            llReciever.setVisibility(View.GONE);
        } else {
            ibCancel.setVisibility(View.GONE);
        }
        //取消
        ibCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsReceiver) {
                    VideoController.getInstance().rejectRequestAudio(mPeerId);
                }
                finish();

            }
        });
        //挂断
        ibHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsReceiver) {
                    VideoController.getInstance().rejectRequestAudio(mPeerId);
                }
                finish();

            }
        });
        //接听
        ibReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ibReceive.setVisibility(View.GONE);
                VideoController.getInstance().acceptRequestAudio(mPeerId);
            }
        });


    }

    @Override
    public void initData() {

        mBroadcastHandler = new BroadcastHandler();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT);
        filter.addAction(VideoController.ACTION_CHANNEL_READY);
        filter.addAction(TXDeviceService.BinderListChange);
        registerReceiver(mBroadcastHandler, filter);

        if (!mIsReceiver) {
            VideoController.getInstance().requestAudio(mPeerId, mDinType);
            tvStateDesc.setText(R.string.wait_friend_receive_audio);
        }

    }

    public void setBitmapToImageView(FriendInfo friendInfo) {
        Bitmap bitmap = imageUtils.getBinderHeadPic(Long.parseLong(friendInfo.uin), ListItemInfo.LISTITEM_TYPE_BINDER);
        if (bitmap == null) {
            imageUtils.fetchBinderHeadPic(Long.parseLong(friendInfo.uin), friendInfo.headUrl);
        } else {
            ivAvatar.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        //startRing必须和stopRing成对调用（使用MediaPlayer）； startRing2必须和stopRing2成对调用（使用SoundPool）；
            //解决音频已接通，播放音频问题
        if(isChannelReady){//
            return;
        }
        if (mIsReceiver) {
               VideoController.getInstance().startRing(R.raw.qav_video_incoming, -1, null);
        } else {
            VideoController.getInstance().startRing(R.raw.qav_video_request, -1, null);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        VideoController.getInstance().closeAudio(mPeerId);
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess();
        }
        mHandler.removeCallbacksAndMessages(null);
        super.unregisterReceiver(mBroadcastHandler);
        super.onDestroy();
    }

    class BroadcastHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(VideoConstants.ACTION_STOP_VIDEO_CHAT)) {
                int reason = intent.getIntExtra("reason", VideoConstants.VOIP_REASON_OTHERS);
                if (reason == VideoConstants.VOIP_REASON_REJECT_BY_FRIEND) {
                    //发起请求之后，对方拒绝
                } else if (reason == VideoConstants.VOIP_REASON_SELF_WAIT_RELAYINFO_TIMEOUT) {
                    //发起请求之后，对方一直不接听，最后超时
                } else if (reason == VideoConstants.VOIP_REASON_CLOSED_BY_FRIEND) {
                    //连通之后，对方主动关闭
                } else {
                    //其它原因
                }
                Log.d(TAG, "recv broadcast : AVSessionClose reason = " + reason);
                finish();
            } else if (intent.getAction().equalsIgnoreCase(VideoController.ACTION_CHANNEL_READY)) {
                VideoController.getInstance().stopRing();
                VideoController.getInstance().stopShake();
                VideoController.getInstance().startShake();
                isChannelReady = true;
                startTime = System.currentTimeMillis();//音频接通的时间
                mHandler.post(updateTime);
                if (tvStateDesc.getVisibility() == View.VISIBLE) {
                    tvStateDesc.setVisibility(View.GONE);
                }
                if (tvDuration.getVisibility() == View.GONE) {
                    tvDuration.setVisibility(View.VISIBLE);
                }
//                if (ibSpeaker.getVisibility() == View.GONE && ibSpeakerSwitcher.getVisibility() == View.GONE) {
//                    ibSpeaker.setVisibility(View.VISIBLE);
//                    ibSpeakerSwitcher.setVisibility(View.VISIBLE);
//                }
                /**modify by : 2018/6/22 9:08*/
//                if(ibSpeakerSwitcher.getVisibility() == View.GONE){
//                    ibSpeakerSwitcher.setVisibility(View.VISIBLE);
//                }

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
            }
        }
    }

    /**
     * 更新时间的线程
     */
    Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long time = (currentTime - startTime) / 1000;
            int min = (int) (time / 60);
            int sec = (int) (time % 60);
            StringBuilder sb = new StringBuilder();
            if (min < 10) {
                sb.append("0");
            }
            sb.append(min);
            sb.append(":");
            if (sec < 10) {
                sb.append("0");
            }
            sb.append(sec);
            tvDuration.setText(sb.toString());
            //不断的更新时间
            mHandler.postDelayed(updateTime, 1000);
        }
    };
}
