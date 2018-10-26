package com.booyue.base;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;

import com.booyue.monitor.R;


/**
 * Created by Administrator on 2017/5/23.
 */
public abstract class BaseActivity extends Activity {

    public LayoutInflater mLayoutInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.dialog_enter_anim_scale, R.anim.dialog_exit_anim_scale);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mLayoutInflater = LayoutInflater.from(this);
        setView();
        initView();
        initData();
    }

    public abstract void setView();

    public abstract void initView();

    public abstract void initData();
}
