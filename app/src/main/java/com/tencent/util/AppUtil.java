package com.tencent.util;

import android.app.ActivityManager;
import android.content.Context;

import com.tencent.MyApp;

import java.util.List;

/**
 * Created by Administrator on 2017/8/31.08:50
 */

public class AppUtil {

    public static boolean isAppOnForeground() {
        // Returns a list of application processes that are running on the
        // device

        ActivityManager activityManager = (ActivityManager) MyApp.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = MyApp.getContext().getPackageName();

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(packageName)
                    && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

}
