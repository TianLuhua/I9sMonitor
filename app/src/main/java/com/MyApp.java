package com;

import android.app.Application;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.util.LoggerUtils;

import java.io.File;

import static com.booyue.Constant.Bugly.APP_ID;

/**
 * Created by Administrator on 2017/5/26.
 */
public class MyApp extends Application {
    private static MyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化Bugly
        CrashReport.initCrashReport(getApplicationContext(), APP_ID, false);
        instance = this;
        File file = new File(getFilesDir(), "debug.txt");
        if (file.exists()) {
            LoggerUtils.setDebug(true);
        }
    }

    public static Context getContext() {
        if (instance == null) {
            instance = new MyApp();
        }
        return instance.getApplicationContext();
    }
}
