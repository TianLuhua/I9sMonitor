package com.tencent.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import com.booyue.serial.UserRequestManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Callback;

/**
 * Created by Administrator on 2018/4/26.10:54
 */

public class UpgradeUtil {

    /**
     * 检测版本升级
     * @param context 上下文
     */
    public static final String TAG = "UpgradeUtil-->";
    public static void checkUpgrade(Context context,Callback callback) {
        LoggerUtils.d(TAG + "checkUpgrade");
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getApplicationContext().getPackageName(), 0);
            String versionName = packageInfo.versionName;
            UserRequestManager.checkUpgrade(versionName, callback);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载apk文件
     * @param path apk文件路径
     * @param pd
     * @return
     * @throws Exception
     */
    public File getFileFromServer(String path, ProgressDialog pd) throws Exception {
        //如果相等的话表示当前的sdcard挂载在手机上并且是可用的
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            //获取到文件的大小
            pd.setMax(conn.getContentLength());
//            Message.obtain(handler,MSG_PROGRESS_MAX,conn.getContentLength()).sendToTarget();//发送文件总长度
            InputStream is = conn.getInputStream();
            File file = new File(Environment.getExternalStorageDirectory(), "video_chat.apk");
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[1024];
            int len;
            int total = 0;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                total += len;
                //获取当前下载量
                pd.setProgress(total);
//                Message.obtain(handler,MSG_PROGRESS,total).sendToTarget();//当前下载的文件长度
            }
            fos.close();
            bis.close();
            is.close();
            return file;
        } else {
            return null;
        }
    }
    /*
     *
	 * 弹出对话框通知用户更新程序
	 *
	 * 弹出对话框的步骤：
	 *  1.创建alertDialog的builder.
	 *  2.要给builder设置属性, 对话框的内容,样式,按钮
	 *  3.通过builder 创建一个对话框
	 *  4.对话框show()出来
	 */


    /*
     * 从服务器中下载APK
     */
    public void downLoadApk(final Context context, final String apkUrl) {
        final ProgressDialog pd;    //进度条对话框
        pd = new ProgressDialog(context);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage("正在下载更新");
        pd.show();
        new Thread() {
            @Override
            public void run() {
                try {
                    File file = getFileFromServer(apkUrl, pd);
//					sleep(3000);
                    if (file != null && file.exists()) {
                        installApk(file, context);
                    }
                    pd.dismiss(); //结束掉进度条对话框
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    //安装apk
    public void installApk(File file, Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);  //执行动作
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");  //执行的数据类型
        context.startActivity(intent);
    }


}
