package com.booyue

import android.app.Application
import android.content.Context
import com.booyue.utils.LoggerUtils
import com.booyue.utils.Utils
import com.tencent.bugly.crashreport.CrashReport
import java.io.File

/**
 * Created by Tianluhua on 2018\11\2 0002.
 */
class MonitorApplication : Application() {

    companion object {
        private var instance: MonitorApplication? = null
        fun getContext(): Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        Utils.init(this@MonitorApplication)
        instance = this@MonitorApplication
        //初始化Bugly
        CrashReport.initCrashReport(applicationContext, APP_ID, false)
        //debug 开关
        val file = File(filesDir, "debug.txt")
        if (file.exists()) {
            LoggerUtils.setDebug(true)
        }
    }
}