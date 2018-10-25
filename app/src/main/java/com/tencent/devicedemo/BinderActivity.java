package com.tencent.devicedemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.av.VideoController;
import com.tencent.device.MsgPack;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDataPoint;
import com.tencent.device.TXDeviceService;

import org.json.JSONObject;

import com.booyue.monitor.R;

public class BinderActivity extends Activity implements OnClickListener {

    private EditText et_msg;
    private MsgListAdapter adapter;
    private NotifyReceiver notifyReceiver;

    private long peerTinyId = 0;
    private int m_dinType = VideoController.UINTYPE_QQ;
    public static final int SEND_MSG = 0;
    public static final int RECEIVE_MSG = 1;

    public static final int OTA_ON_NEW_PKG_COME = 2;
    public static final int OTA_ON_DOWNLOAD_PROGRESS = 3;
    public static final int OTA_ON_DOWNLOAD_COMPLETE = 4;
    public static final int OTA_ON_UPDATE_CONFIRM = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binder);

        Intent intent = getIntent();
        peerTinyId = intent.getLongExtra("tinyid", 0);
        int type = intent.getIntExtra("type", 0);

        TextView tvBinderName = (TextView) findViewById(R.id.binder_name);
        tvBinderName.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if (ListItemInfo.LISTITEM_TYPE_BINDER == type) {
            m_dinType = VideoController.UINTYPE_QQ;
            tvBinderName.setText("绑定者：" + intent.getStringExtra("nickname"));
        } else if (ListItemInfo.LISTITEM_TYPE_FRIEND == type) {
            m_dinType = VideoController.UINTYPE_DIN;
            tvBinderName.setText("好友：" + intent.getStringExtra("nickname"));
        }

        Button btn_audio = (Button) findViewById(R.id.open_audio);
        Button btn_video = (Button) findViewById(R.id.open_video);
        Button btn_send = (Button) findViewById(R.id.msgsend_button);
        Button btn_record = (Button) findViewById(R.id.video_message);
        Button btn_file = (Button) findViewById(R.id.filesend_button);
        btn_send.setOnClickListener(this);
        btn_record.setOnClickListener(this);
        btn_video.setOnClickListener(this);
        btn_file.setOnClickListener(this);
        btn_audio.setOnClickListener(this);

        et_msg = (EditText) findViewById(R.id.msg_editor);
        ListView lv_msghistory = (ListView) findViewById(R.id.msghistory_lv);
        adapter = new MsgListAdapter(this);
        lv_msghistory.setAdapter(adapter);

        registryNotifyReceiver();
    }

    private void registryNotifyReceiver() {
        // 注册广播事件监听：发送消息返回结果，接收到消息
        IntentFilter filter = new IntentFilter();
        filter.addAction(TXDeviceService.OTAOnNewPkgCome);
        filter.addAction(TXDeviceService.OTAOnDownloadProgress);
        filter.addAction(TXDeviceService.OTAOnDownloadComplete);
        filter.addAction(TXDeviceService.OTAOnUpdateConfirm);
        filter.addAction(TXDeviceService.BinderListChange);
        filter.addAction(TXDeviceService.OnEraseAllBinders);
        filter.addAction(TXDeviceService.OnReceiveDataPoint);
        notifyReceiver = new NotifyReceiver();
        registerReceiver(notifyReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(notifyReceiver);
    }

    public class NotifyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == TXDeviceService.OTAOnNewPkgCome) {
                Message msg = new Message();
                msg.what = OTA_ON_NEW_PKG_COME;
                msg.obj = intent;
                mHandler.sendMessage(msg);
            } else if (intent.getAction() == TXDeviceService.OTAOnDownloadProgress) {
                Message msg = new Message();
                msg.what = OTA_ON_DOWNLOAD_PROGRESS;
                msg.obj = intent;
                mHandler.sendMessage(msg);
            } else if (intent.getAction() == TXDeviceService.OTAOnDownloadComplete) {
                Message msg = new Message();
                msg.what = OTA_ON_DOWNLOAD_COMPLETE;
                msg.obj = intent;
                mHandler.sendMessage(msg);
            } else if (intent.getAction() == TXDeviceService.OTAOnUpdateConfirm) {
                Message msg = new Message();
                msg.what = OTA_ON_UPDATE_CONFIRM;
                msg.obj = intent;
                mHandler.sendMessage(msg);
            } else if (intent.getAction() == TXDeviceService.BinderListChange) {
                boolean bFind = false;
                Parcelable[] listBinder = intent.getExtras().getParcelableArray("binderlist");
                for (int i = 0; i < listBinder.length; ++i) {
                    TXBinderInfo binder = (TXBinderInfo) (listBinder[i]);
                    if (binder.tinyid == peerTinyId) {
                        bFind = true;
                        break;
                    }
                }
                if (bFind == false) {
                    finish();
                }
            } else if (intent.getAction() == TXDeviceService.OnEraseAllBinders) {
                finish();
            } else if (intent.getAction() == TXDeviceService.OnReceiveDataPoint) {
                Long from = intent.getExtras().getLong("from", 0);
                if (from == peerTinyId) {
                    Parcelable[] arrayDataPoint = intent.getExtras().getParcelableArray("datapoint");
                    for (int i = 0; i < arrayDataPoint.length; ++i) {
                        TXDataPoint dp = (TXDataPoint) (arrayDataPoint[i]);
                        try {
                            JSONObject dpValue = new JSONObject(dp.property_val);
                            final String txtMsg = dpValue.getString("text");
                            if (dp.property_id == 10000) {    // 10000：文本消息
                                mHandler.post(new Runnable() {
                                    public void run() {
                                        MsgPack msgPack = new MsgPack();
                                        msgPack.bIsSelf = false;
                                        msgPack.strText = txtMsg;
                                        adapter.addMsgPack(msgPack);
                                    }

                                    ;
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SEND_MSG) {

            } else if (msg.what == OTA_ON_NEW_PKG_COME) {
                Intent intent1 = (Intent) msg.obj;
                MsgPack msgPack = new MsgPack();
                msgPack.initReceivedMsg(0, intent1.getLongExtra("from", 0), "收到升级通知：" + intent1.getStringExtra("title"));
                adapter.addMsgPack(msgPack);
            } else if (msg.what == OTA_ON_DOWNLOAD_PROGRESS) {
                Intent intent2 = (Intent) msg.obj;
                MsgPack msgPack = new MsgPack();
                msgPack.initReceivedMsg(0, intent2.getLongExtra("from", 0), "下载进度:" + intent2.getLongExtra("current", 0) + "/" + intent2.getLongExtra("count", 0));
                adapter.addMsgPack(msgPack);
            } else if (msg.what == OTA_ON_DOWNLOAD_COMPLETE) {
                Intent intent3 = (Intent) msg.obj;
                MsgPack msgPack = new MsgPack();
                msgPack.initReceivedMsg(0, intent3.getLongExtra("from", 0), "下载完成：" + (intent3.getIntExtra("resultCode", -1) == 0 ? "success" : "failed"));
                adapter.addMsgPack(msgPack);
            } else if (msg.what == OTA_ON_UPDATE_CONFIRM) {
                final Intent intent4 = (Intent) msg.obj;
                MsgPack msgPack = new MsgPack();
                msgPack.initReceivedMsg(0, intent4.getLongExtra("from", 0), "进入重启升级...");
                adapter.addMsgPack(msgPack);

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        TXDeviceService.ackOtaResult(0, "success");
                        MsgPack msgPack = new MsgPack();
                        msgPack.initReceivedMsg(0, intent4.getLongExtra("from", 0), "升级完成，上报升级结果...");
                        adapter.addMsgPack(msgPack);
                    }

                    ;
                }, 10000);
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.msgsend_button: {//发送信息
                sendMsg();
                //TXDeviceService.ackOtaResult(0, null);
                //TXDeviceService.sendFileTo(peerTinyId, "/storage/emulated/0/Tencent/MobileQQ/data/srvAddr.ini",null, TXDeviceService.strFileMsgService);
            }
            break;
            case R.id.open_video: {//打开视频
                if (BinderActivity.isNetworkAvailable(this)) {
                    if (false == VideoController.getInstance().hasPendingChannel()) {
                        TXDeviceService.getInstance().startVideoChatActivity(peerTinyId, m_dinType);
                    } else {
                        Toast.makeText(BinderActivity.this.getApplicationContext(), "视频中", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(BinderActivity.this.getApplicationContext(), "当前网络不可用，请连接网络", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.video_message: {//视频留言
                Intent videoMessage = new Intent(BinderActivity.this, VideoMessageActivity.class);
                videoMessage.putExtra("tinyid", peerTinyId);
                startActivity(videoMessage);
                break;
            }
            case R.id.filesend_button: {//发送文件
                Intent videoMessage = new Intent(BinderActivity.this, FileMsgActivity.class);
                videoMessage.putExtra("tinyid", peerTinyId);
                startActivity(videoMessage);
                break;
            }
            case R.id.open_audio: {//打开音频
                if (BinderActivity.isNetworkAvailable(this)) {
                    if (false == VideoController.getInstance().hasPendingChannel()) {
                        TXDeviceService.getInstance().startAudioChatActivity(peerTinyId, m_dinType);
                    } else {
                        Toast.makeText(BinderActivity.this.getApplicationContext(), "视频中", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(BinderActivity.this.getApplicationContext(), "当前网络不可用，请连接网络", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default:
                break;
        }
    }

    private void sendMsg() {
        String msgContext = et_msg.getText().toString();
        long[] targetIds = {peerTinyId};
        TXDeviceService.sendTextMsg(et_msg.getText().toString(), 3, targetIds);
        et_msg.setText("");

        MsgPack msgPack = new MsgPack();
        msgPack.bIsSelf = true;
        msgPack.strText = msgContext;
        adapter.addMsgPack(msgPack);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (cm.getActiveNetworkInfo() != null) {
                return cm.getActiveNetworkInfo().isAvailable();
            }
        }
        return false;
    }
}
