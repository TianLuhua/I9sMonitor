package com.booyue.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.booyue.MonitorApplication
import java.lang.ref.WeakReference

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */
class Utils {

    constructor() {
        throw UnsupportedOperationException("u can't instantiate me...")
    }

    companion object {
        private var application: MonitorApplication? = null
        private var topActivityWeakRef: WeakReference<Activity>? = null
        private val activityList = arrayListOf<Activity>()
        private val mCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {
            }

            override fun onActivityResumed(activity: Activity) {
                setTopActivityWeakRef(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                setTopActivityWeakRef(activity)
            }

            override fun onActivityDestroyed(activity: Activity?) {
                activityList.remove(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
            }

            override fun onActivityStopped(activity: Activity?) {
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activityList.add(activity)
                setTopActivityWeakRef(activity)
            }

        }

        private fun setTopActivityWeakRef(activity: Activity) {
            if (topActivityWeakRef == null || !activity.equals(topActivityWeakRef?.get())) {
                topActivityWeakRef = WeakReference(activity)
            }

        }

        fun init(app: MonitorApplication) {
            this.application = app
            this.application?.registerActivityLifecycleCallbacks(mCallbacks)
        }

        fun getApp(): MonitorApplication? {
            if (application != null) {
                return application
            }
            throw NullPointerException("u should init first")
        }

    }

}