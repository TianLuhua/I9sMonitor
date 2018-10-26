package com.tencent.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.MyApp;
import com.tencent.devicedemo.ListItemInfo;
import com.booyue.monitor.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Administrator on 2017/5/26.
 */
public class ImageUtils {
    private String TAG = this.getClass().getSimpleName();

    private String mHeadPicPath;

    private Handler mHandler;

    private Set<Long> mSetFetching = new HashSet<>();

    public ImageUtils(Handler handler,Set<Long> mSetFetching) {
        this.mHandler = handler;
        this.mSetFetching = mSetFetching;
        mHeadPicPath = MyApp.getContext().getCacheDir().getAbsolutePath() + "/head";
        File file = new File(mHeadPicPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 获取头像
     *
     * @param uin
     * @param type
     * @return
     */
    public Bitmap getBinderHeadPic(long uin, int type) {
        Bitmap bitmap = null;
        try {
            if (ListItemInfo.LISTITEM_TYPE_ADD_FRIEND == type) {
                bitmap = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.drawable.add_more);
            } else {
                String strHeadPic = mHeadPicPath + "/" + uin + ".png";
                bitmap = BitmapFactory.decodeFile(strHeadPic);
            }
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
        return bitmap;
    }

    /**
     * 保存头像
     *
     * @param uin
     * @param bitmap
     */
    public void saveBinderHeadPic(long uin, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        String strHeadPic = mHeadPicPath + "/" + uin + ".png";
        File file = new File(strHeadPic);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    /**
     * 从网络获取头像
     *
     * @param uin    标识码
     * @param strUrl 头像url
     */
    public void fetchBinderHeadPic(final long uin, final String strUrl) {
        synchronized (mSetFetching) {
            if (mSetFetching.contains(uin)) {
                return;
            } else {
                mSetFetching.add(uin);
            }
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(strUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    saveBinderHeadPic(uin, bitmap);
                    synchronized (mSetFetching) {
                        mSetFetching.remove(uin);
                    }
                    mHandler.sendEmptyMessage(0);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }
            }
        }.start();
    }
}
