package com.tencent.devicedemo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.tencent.device.TXDeviceService;
import com.tencent.device.TXFriendInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.booyue.monitor.R;

public class FriendInfoActivity extends Activity {
    public static int ACTIVITY_TYPE_ADD_FRIEDN = 1;
    public static int ACTIVITY_TYPE_NEW_FRIEND_REQ = 2;

    private static String TAG = "FriendInfoActivity";
    private TXFriendInfo mFriendInfo;
    private int mType = ACTIVITY_TYPE_ADD_FRIEDN;
    private String mHeadPicPath;
    private Handler mHandler = null;
    private Set<Long> mSetFetching = new HashSet<Long>();
    private String mDevName = "";
    private String mValidationMsg = "";
    private NotifyReceiver mNotifyReceiver;
    private Button mBtn;
    private TextView mConfirmTextView;

    private EditText mRemarkNameEditText;
    private EditText mValidationEditText;
    private TextView mRemarkNameTextView;
    private Long mSocialNumber = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friendinfo);

        Intent intent = getIntent();
        mFriendInfo = intent.getParcelableExtra("FriendInfo");
        mType = intent.getIntExtra("type", ACTIVITY_TYPE_ADD_FRIEDN);

        mRemarkNameEditText = (EditText) findViewById(R.id.addfirend_remark_editText);
        mValidationEditText = (EditText) findViewById(R.id.addfirend_validation_msg_editText);
        mRemarkNameTextView = (TextView) findViewById(R.id.addfirend_remark_text);

        mSocialNumber = intent.getLongExtra("SocialNumber", 0L);
        if (mType == ACTIVITY_TYPE_ADD_FRIEDN) {
            mValidationMsg = "我是隔壁老王（可修改），我希望与您的" + mFriendInfo.getDeviceName() + "成为设备好友！";
            mValidationEditText.setText(mValidationMsg, TextView.BufferType.EDITABLE);
        } else {
            mValidationMsg = intent.getStringExtra("ValidationMsg");

            mRemarkNameEditText.setVisibility(View.INVISIBLE);
            mRemarkNameTextView.setVisibility(View.INVISIBLE);

            mValidationEditText.setText(mValidationMsg);
            mValidationEditText.setFocusable(false);


            mDevName = mFriendInfo.getDeviceName();
        }


        mHeadPicPath = this.getCacheDir().getAbsolutePath() + "/head";
        File file = new File(mHeadPicPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        mHandler = new Handler(this.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Bitmap bitmap = getBinderHeadPic(mFriendInfo.friend_din);
                ImageView head = (ImageView) findViewById(R.id.friendinfo_headpic);
                if (bitmap != null) {
                    head.setImageBitmap(bitmap);
                }
            }
        };

        refreshHeadImage(mFriendInfo.friend_din, mFriendInfo.head_url);

        TextView devNameTextView = (TextView) findViewById(R.id.friendinfo_nick_name);
        devNameTextView.setText(mDevName + "(" + Long.toString(mSocialNumber) + ")");

        mBtn = (Button) findViewById(R.id.friendinfo_confirm_btn);
        mConfirmTextView = (TextView) findViewById(R.id.friendinfo_confirm_text);

        if (ACTIVITY_TYPE_NEW_FRIEND_REQ == mType) {
            mConfirmTextView.setText("以下设备请求与你的设备加为好友，互相通话");
            mBtn.setText("添加");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(TXDeviceService.OnConfirmAddFriend);
        filter.addAction(TXDeviceService.OnReqAddFriend);

        mNotifyReceiver = new NotifyReceiver();
        registerReceiver(mNotifyReceiver, filter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNotifyReceiver);
    }

    public void confirmadd(View v) {
        if (ACTIVITY_TYPE_ADD_FRIEDN == mType) {
            mValidationMsg = mValidationEditText.getText().toString();
            String remarkName = mRemarkNameEditText.getText().toString();
            if (remarkName.length() != 0) {
                mDevName = remarkName;
            } else {
                mDevName = mFriendInfo.getDeviceName() + "(" + Long.toString(mSocialNumber) + ")";
            }

            TXDeviceService.reqAddFriend(mFriendInfo.friend_din, mDevName, mValidationMsg);
        } else {
            Log.d("Confirmadd", "mDevName:" + mDevName);
            TXDeviceService.confirmAddFriend(mFriendInfo.friend_din, mDevName, true);
        }

        mBtn.setEnabled(false);
    }

    public class NotifyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == TXDeviceService.OnReqAddFriend || intent.getAction() == TXDeviceService.OnConfirmAddFriend) {
                int result = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 != result) {
                    showAlert("添加好友失败", "添加好友失败，错误码：" + result);
                    mBtn.setEnabled(true);
                    return;
                }

                if (ACTIVITY_TYPE_ADD_FRIEDN == mType) {
                    Intent friendFinishIntent = new Intent(FriendInfoActivity.this, AddFriendFinishActivity.class);
                    startActivity(friendFinishIntent);
                }
                FriendInfoActivity.this.finish();
            }
        }
    }

    private void showAlert(String strTitle, String strMsg) {
        // TODO Auto-generated method stub
        AlertDialog dialogError;
        Builder builder = new AlertDialog.Builder(this).setTitle(strTitle).setMessage(strMsg).setPositiveButton("取消", null).setNegativeButton("确定", null);
        dialogError = builder.create();
        dialogError.show();
    }

    private void refreshHeadImage(long uin, String headUrl) {
        ImageView head = (ImageView) findViewById(R.id.friendinfo_headpic);
        Bitmap bitmap = getBinderHeadPic(uin);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.binder_default_head);
            head.setImageBitmap(bitmap);
            if (headUrl != null && headUrl.length() > 0) {
                fetchBinderHeadPic(uin, headUrl);
            }
        } else {
            head.setImageBitmap(bitmap);
        }
    }

    //TODO:可以将这里与FriendListAdapter的图片拉取与保存逻辑封装一个类供其他模块公用
    public Bitmap getBinderHeadPic(long uin) {
        Bitmap bitmap = null;
        try {
            String strHeadPic = mHeadPicPath + "/" + uin + ".png";
            bitmap = BitmapFactory.decodeFile(strHeadPic);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
        return bitmap;
    }

    public void saveBinderHeadPic(long uin, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        String strHeadPic = mHeadPicPath + "/" + uin + ".png";
        File file = new File(strHeadPic);
        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    public void fetchBinderHeadPic(final long uin, final String strUrl) {
        synchronized (mSetFetching) {
            if (mSetFetching.contains(uin)) {
                return;
            } else {
                mSetFetching.add(uin);
            }
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(strUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    saveBinderHeadPic(uin, bitmap);
                    synchronized (mSetFetching) {
                        mSetFetching.remove(uin);
                    }
                    mHandler.sendEmptyMessage(0);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }
            }
        }.start();
    }

}
