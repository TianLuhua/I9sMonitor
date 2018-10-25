package com.tencent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.av.VideoController;
import com.tencent.av.core.VideoConstants;
import com.tencent.device.FriendInfo;
import com.tencent.device.QLog;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;
import com.tencent.devicedemo.ListItemInfo;
import com.booyue.monitor.R;
import com.tencent.util.ImageUtils;

import java.util.HashSet;
import java.util.Set;

import static com.tencent.av.VideoController.ACTION_VIDEO_QOS_NOTIFY;
import static com.tencent.av.mediacodec.AndroidCodec.TAG;

/**
 * Created by lingyuhuang on 2016/10/8.
 * 语音聊天demo
 * 此Activity暂时没用
 */

public class BooyueFriendInfoActivity extends BaseActivity {

//    private ImageView ivBtnAccept;
//    private ImageView ivBtnHangup;

    private boolean mIsReceiver;// 是发起方
    private String mPeerId;
    private int mDinType;
    private String mSelfDin;
    private long startTime;

    private BroadcastHandler mBroadcastHandler;



//    private ImageButton ibSpeaker;
    private ImageButton ibInviteFriend;
    private CircleImageView ivAvatar;
    private TextView tvName;
    private TextView tvStateDesc;
    private TextView tvDuration;
    private ImageButton ibCancel;
    private LinearLayout llReciever;
    private ImageButton ibHangup;
    private ImageButton ibReceive;
    private Handler mHandler;
    private Set<Long> mSetFetching = new HashSet<>();
    private ImageUtils imageUtils;
    private TextView tvBack;


    @Override
    public void setView() {
        setContentView(R.layout.activity_friendinfo_booyue);

        Intent intent = super.getIntent();
        mPeerId = intent.getStringExtra("peerid");
        mDinType = intent.getIntExtra("dinType", VideoController.UINTYPE_QQ);
        mSelfDin = VideoController.getInstance().GetSelfDin();
        mIsReceiver = intent.getBooleanExtra("receive", false);
        if (Long.parseLong(mPeerId) == 0 || Long.parseLong(mSelfDin) == 0) {
            QLog.e(TAG, QLog.CLR, "invalid peerId: " + mPeerId + " invalid selfDin: " + mSelfDin);
            finish();
        }

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
    }

    @Override
    public void initView() {

        tvBack = (TextView) findViewById(R.id.tv_back);
        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        tvName = (TextView) findViewById(R.id.tv_name);
        tvDuration = (TextView) findViewById(R.id.tv_duration);
        tvDuration.setVisibility(View.GONE);

        final FriendInfo friendInfo = VideoController.getInstance().getFriendInfo(mPeerId);
        tvName.setText(friendInfo.getName());

        /**
         * 先隐藏，只有接通之后才显示
         */
//        ibSpeaker = (ImageButton) findViewById(R.id.ib_speaker);
        ibInviteFriend = (ImageButton) findViewById(R.id.ib_speaker_switcher);
//        ibSpeaker.setVisibility(View.GONE);
        ibInviteFriend.setVisibility(View.GONE);

        tvStateDesc = (TextView) findViewById(R.id.tv_state_desc);
        tvStateDesc.setText(R.string.wait_friend_receive_video);

        ivAvatar = (CircleImageView) findViewById(R.id.iv_avatar);
        mHandler = new Handler(this.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                setBitmapToImageView(friendInfo);
            }
        };
        imageUtils = new ImageUtils(mHandler,mSetFetching);
        setBitmapToImageView(friendInfo);


        ibCancel = (ImageButton) findViewById(R.id.tv_cancel);
        llReciever = (LinearLayout) findViewById(R.id.ll_recieve_before);
        ibHangup = (ImageButton) findViewById(R.id.tv_hangup);
        ibReceive = (ImageButton) findViewById(R.id.tv_receive);
        if (!mIsReceiver) {
            llReciever.setVisibility(View.GONE);
        }else {
            ibCancel.setVisibility(View.GONE);
        }

        ibCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsReceiver) {
                    VideoController.getInstance().onRejectVideo(mPeerId);
                }
                finish();
            }
        });

        ibHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsReceiver) {
                    VideoController.getInstance().onRejectVideo(mPeerId);
                }
                finish();
            }
        });
        ibReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               startVideoChatActivity();
            }
        });

//        VideoController.getInstance().enableQosNotify(true);

        //startRing必须和stopRing成对调用（使用MediaPlayer）； startRing2必须和stopRing2成对调用（使用SoundPool）；
        if (mIsReceiver) {
            VideoController.getInstance().startRing(R.raw.qav_video_incoming, -1, null);
            //VideoController.getInstance().startRing2(R.raw.qav_video_incoming, -1);
        } else {
            VideoController.getInstance().startRing(R.raw.qav_video_request, -1, null);
            //VideoController.getInstance().startRing2(R.raw.qav_video_request, -1);
        }
    }


    public void startVideoChatActivity(){
        Intent intent = new Intent(this,BooyueVideoChatActivitySF.class);
        intent.putExtra("peerid",mPeerId);
        intent.putExtra("dinType",mDinType);
        intent.putExtra("receive",mIsReceiver);
        startActivity(intent);
        finish();
    }


    @Override
    public void initData() {



    }

    public void setBitmapToImageView(FriendInfo friendInfo){
        Bitmap bitmap = imageUtils.getBinderHeadPic(Long.parseLong(friendInfo.uin), ListItemInfo.LISTITEM_TYPE_BINDER);
        if(bitmap == null){
            imageUtils.fetchBinderHeadPic(Long.parseLong(friendInfo.uin),friendInfo.headUrl);
        }else {
            ivAvatar.setImageBitmap(bitmap);
        }
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
//        VideoController.getInstance().closeAudio(mPeerId);
//        if (TXDeviceService.VideoProcessEnable) {
//            VideoController.getInstance().exitProcess();
//        }
//        mHandler.removeCallbacksAndMessages(null);
        super.unregisterReceiver(mBroadcastHandler);
        super.onDestroy();
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
//                mVideoConnected = true;
//                mClose.setText(R.string.close);
//                startTime = System.currentTimeMillis();
//                mHandler.post(updateTime);
                startVideoChatActivity();
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
                 */
            } else if (intent.getAction().equals(ACTION_VIDEO_QOS_NOTIFY)) {
//                int width = intent.getIntExtra("width", VcCamera.PREVIEW_WIDTH);
//                int height = intent.getIntExtra("height", VcCamera.PREVIEW_HEIGHT);
//                if (width != VcCamera.PREVIEW_WIDTH || height != VcCamera.PREVIEW_HEIGHT) {
//                    // 需要更新采集的分辨率，重启摄像头
//                    VcCamera.PREVIEW_WIDTH = width;
//                    VcCamera.PREVIEW_HEIGHT = height;
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
//                if (currentDefinition == VideoController.DEFINITION_TYPE_CLEAR) {
//                    Toast.makeText(context, R.string.network_poor, Toast.LENGTH_SHORT).show();
//                }
            }
        }
    }
}
