package com.tencent.devicedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.tencent.device.TXFriendInfo;

import com.booyue.monitor.R;

public class NewFriendReqActivity extends Activity {
    private TXFriendInfo mFriendInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_newfriendreq);

        Intent intent = getIntent();
        mFriendInfo = intent.getParcelableExtra("FriendInfo");


    }

    public void showdetail(View v) {

    }
}
