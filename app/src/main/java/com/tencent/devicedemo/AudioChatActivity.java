package com.tencent.devicedemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.av.VideoController;
import com.tencent.av.core.VideoConstants;
import com.tencent.device.FriendInfo;
import com.tencent.device.QLog;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;
import com.booyue.monitor.R;

import static com.tencent.av.mediacodec.AndroidCodec.TAG;

/**
 * Created by lingyuhuang on 2016/10/8.
 * 语音聊天demo
 */

public class AudioChatActivity extends Activity {

    private TextView tvNick;
    private TextView tvTime;
    private ImageView ivBtnAccept;
    private ImageView ivBtnHangup;

    private boolean mIsReceiver;// 是发起方
    private String mPeerId;
    private int mDinType;
    private String mSelfDin;
    private long startTime;

    private BroadcastHandler mBroadcastHandler;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_chat);

        Intent intent = getIntent();

        mPeerId = intent.getStringExtra("peerid");
        mDinType = intent.getIntExtra("dinType", VideoController.UINTYPE_QQ);
        mSelfDin = VideoController.getInstance().GetSelfDin();
        mIsReceiver = intent.getBooleanExtra("receive", false);
        if (Long.parseLong(mPeerId) == 0 || Long.parseLong(mSelfDin) == 0) {
            QLog.e(TAG, QLog.CLR, "invalid peerId: " + mPeerId + " invalid selfDin: " + mSelfDin);
            finish();
        }

        tvNick = (TextView) findViewById(R.id.tv_nick);
        FriendInfo friendInfo = VideoController.getInstance().getFriendInfo(mPeerId);
        tvNick.setText(friendInfo.getName());

        tvTime = (TextView) findViewById(R.id.tv_time);

        ivBtnAccept = (ImageView) findViewById(R.id.iv_btn_accept);
        ivBtnHangup = (ImageView) findViewById(R.id.iv_btn_hangup);
        if (!mIsReceiver) {
            ivBtnAccept.setVisibility(View.GONE);
        }
        ivBtnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ivBtnAccept.setVisibility(View.GONE);
                VideoController.getInstance().acceptRequestAudio(mPeerId);
            }
        });
        ivBtnHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsReceiver) {
                    VideoController.getInstance().rejectRequestAudio(mPeerId);
                }
                finish();
            }
        });

        mBroadcastHandler = new BroadcastHandler();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT);
        filter.addAction(VideoController.ACTION_CHANNEL_READY);
        filter.addAction(TXDeviceService.BinderListChange);
        registerReceiver(mBroadcastHandler, filter);

        if (!mIsReceiver) {
            VideoController.getInstance().requestAudio(mPeerId, mDinType);
            tvTime.setText("正在呼叫对方");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        //startRing必须和stopRing成对调用（使用MediaPlayer）； startRing2必须和stopRing2成对调用（使用SoundPool）；
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
                startTime = System.currentTimeMillis();
                mHandler.post(updateTime);
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
            tvTime.setText(sb.toString());
            mHandler.postDelayed(updateTime, 1000);
        }
    };
}
