package com.booyue.uils

import android.app.ActivityManager
import android.content.Context
import com.booyue.MonitorApplication

/**
 * Created by Tianluhua on 2018\11\8 0008.
 */
class SystemServiceUtil {
    companion object {
        //判断目标Service是否处于运行状态
        fun isTagServiceRunning(tagService: String): Boolean {
            var isRunning = false
            val activityManager = MonitorApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val services = activityManager.getRunningServices(Integer.MAX_VALUE)
            services.forEach {
                when (it.service.className) {
                    tagService -> isRunning = true
                }
            }
            return isRunning
        }
    }
}