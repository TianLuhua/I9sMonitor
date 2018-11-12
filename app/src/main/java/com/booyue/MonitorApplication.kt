package com.booyue

import android.app.Application
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.booyue.monitor.R
import com.booyue.serial.SerialNumberManager
import com.booyue.ui.friend.BooyueFriendListActivity
import com.booyue.utils.LoggerUtils
import com.booyue.utils.ToastUtils
import com.booyue.utils.Utils
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.device.TXDeviceService
import com.tencent.util.FileUtil
import java.io.File

/**
 * Created by Tianluhua on 2018\11\2 0002.
 */
class MonitorApplication : Application() {

    companion object {
        val TAG = "MonitorApplication"
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


        //启动Tencent核心服务
        SerialNumberManager.readSerialNumber(this@MonitorApplication) {
            LoggerUtils.d(BooyueFriendListActivity.TAG + "onSerailNumberListener Code:" + it)
            startTencentCoreService()
        }
        // 晨芯方案商&串号没有写入文件，
        // 不需要做任何操作，此时启动线程网络获取串号并写入文件，操作执行在接口回调中（line 60）
        /**modify by : 2018/3/5 16:17*/ //T6改用服务端获取数据
        if (!FileUtil.isSNCached() && SerialNumberManager.matchDevice()) {
            //启动线程通过回调获取
            LoggerUtils.d("$TAG !FileUtil.isSNCached() && SerialNumberManager.matchDevice()")
        } else {
            startTencentCoreService()
        }
    }

    /**
     * 启动Tencent 核心服务
     */
    private fun startTencentCoreService() {
        if (PRODUCT_ID == 0L || TextUtils.isEmpty(LICENSE) || TextUtils.isEmpty(SERIAL_NUMBER)
                || TextUtils.isEmpty(SERVER_PUBLIC_KEY)) {
            LoggerUtils.d(resources.getText(R.string.unique_identifier).toString())
//            ToastUtils.showLongToast(R.string.unique_identifier)
        } else {
            val startIntent = Intent(applicationContext, TXDeviceService::class.java)
            startService(startIntent)
        }
    }
}