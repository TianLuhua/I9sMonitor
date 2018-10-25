package com.tencent.devicedemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.tencent.device.TXDeviceService;

import java.util.Timer;
import java.util.TimerTask;

import com.booyue.monitor.R;

public class AddFriendActivity extends Activity {
    private NotifyReceiver mNotifyReceiver;
    private EditText mEditText;
    private Long mSocialNumberLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addfriend);

        IntentFilter filter = new IntentFilter();
        filter.addAction(TXDeviceService.OnFetchAddFriendInfo);

        mNotifyReceiver = new NotifyReceiver();
        registerReceiver(mNotifyReceiver, filter);

        mEditText = (EditText) findViewById(R.id.addfirend_editText);
        mEditText.setFocusableInTouchMode(true);
        mEditText.requestFocus();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                InputMethodManager inputManager =
                        (InputMethodManager) mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(mEditText, 0);
            }

        }, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNotifyReceiver);
    }

    public void confirmadd(View v) {
        long number = 0;
        try {
            number = Long.parseLong(mEditText.getText().toString());
        } catch (Exception e) {

        }
        if (0 == number) {
            showAlert("非法的设备号", "请输入正确的设备号");
            return;
        }

        mSocialNumberLong = number;
        TXDeviceService.fetchAddFriendInfo(number);
        Button btn = (Button) findViewById(R.id.addfirend_confirm_btn);
        btn.setEnabled(false);
    }

    private void showAlert(String strTitle, String strMsg) {
        // TODO Auto-generated method stub
        AlertDialog dialogError;
        Builder builder = new AlertDialog.Builder(this).setTitle(strTitle).setMessage(strMsg).setPositiveButton("取消", null).setNegativeButton("确定", null);
        dialogError = builder.create();
        dialogError.show();
    }


    public class NotifyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == TXDeviceService.OnFetchAddFriendInfo) {
                int result = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 != result) {
                    if (result == 37) {
                        showAlert("添加好友失败", "需要先绑定设备才能加好友");
                    } else {
                        showAlert("获取信息失败", "获取好友信息失败，错误码：" + result);
                    }
                    Button btn = (Button) findViewById(R.id.addfirend_confirm_btn);
                    btn.setEnabled(true);
                    return;
                }
                Parcelable parcel = intent.getExtras().getParcelable("FriendInfo");

                Intent friendIntent = new Intent(AddFriendActivity.this, FriendInfoActivity.class);
                friendIntent.putExtra("FriendInfo", parcel);

                friendIntent.putExtra("type", FriendInfoActivity.ACTIVITY_TYPE_ADD_FRIEDN);
                friendIntent.putExtra("SocialNumber", mSocialNumberLong);
                startActivity(friendIntent);
                AddFriendActivity.this.finish();
            }
        }
    }

}
