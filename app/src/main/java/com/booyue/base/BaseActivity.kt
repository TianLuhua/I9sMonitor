package com.booyue.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.booyue.monitor.R

/**
 * Created by Tianluhua on 2018\10\26 0026.
 */
abstract class BaseActivity : Activity() {

    lateinit var mLayoutInflater: LayoutInflater

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.dialog_enter_anim_scale, R.anim.dialog_exit_anim_scale)
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        mLayoutInflater = LayoutInflater.from(this)
        setView()
        initView()
        initData()
    }


    abstract fun setView()

    abstract fun initView()

    abstract fun initData()
}