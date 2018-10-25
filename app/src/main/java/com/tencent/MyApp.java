package com.tencent;

import android.app.Application;
import android.content.Context;

import com.tencent.util.LoggerUtils;

import java.io.File;

/**
 * Created by Administrator on 2017/5/26.
 */
public class MyApp extends Application {
    private static MyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
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
