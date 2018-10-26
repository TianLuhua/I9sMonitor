package com.booyue.serial;

import com.tencent.util.LoggerUtils;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2018/2/24.17:40
 */

public class UserRequestManager {
    /**
     * 从服务器获取文件
     *
     * @param urlStr 请求文件的url
     * @return 返回流
     * @throws IOException
     */
    public static final String TAG = "UserRequestManager-->";
    public static final String CHENXIN_GET_SERIAL_NUMBER = "http://dapi.orangelive.tv:10085/api/v1/mac?mac=";
    public static final String GET_SERIAL_NUMBER = "https://cloud.alilo.com.cn/baby/api/s1/external/getSerialNumber?";
    public static final String UPGRADE_URL = "http://cloud.alilo.com.cn/baby/api/s1/external/checkVideoVersion?";

    /**
     * 获取串号
     *
     * @param mac          机器唯一值
     * @param callback     回调
     * @param productModel 产品id（L1,T6,I6S+）
     */
    public static void getSerialNumber(String mac, Callback callback, String productModel) {
        LoggerUtils.d(TAG + "mac = " + mac);
        LoggerUtils.d(TAG + "channel = hhtjgs");
        LoggerUtils.d(TAG + "product = " + productModel);
        RequestBody requestBody = new FormBody.Builder()
                .add("channel", "hhtjgs")
                .add("mac", mac)
                .add("product", productModel)
                .build();
        RequestManager.post(GET_SERIAL_NUMBER, requestBody, callback);
    }

    /**
     * 版本升级
     *
     * @param versionName 版本名
     * @param callback    回调
     */
    public static void checkUpgrade(String versionName, Callback callback) {
        LoggerUtils.d(TAG + "versionName = " + versionName);
        RequestBody requestBody = new FormBody.Builder()
                .add("version", versionName)
                .build();
        LoggerUtils.d(TAG + "upgrade url = " + UPGRADE_URL + "version=" + versionName);
        RequestManager.post(UPGRADE_URL, requestBody, callback);
    }

    /**
     * 获取I6S串号请求
     *
     * @param mac 唯一标识（mac地址）
     * @return json字符串
     */
    public static void getI6SSerialNumber(String mac, Callback callback) {
        LoggerUtils.d(TAG + "mac = " + mac);
        String path = CHENXIN_GET_SERIAL_NUMBER + mac;
        LoggerUtils.d(TAG + "path= " + path);
        RequestManager.get(path, callback);
    }


//    public static String getI6SSerialNumber(String mac) {
//        try {
//            String path = CHENXIN_GET_SERIAL_NUMBER + mac;
//            URL url = new URL(path);
//            LoggerUtils.d(TAG + "getI6SSerialNumber");
//            LoggerUtils.d(TAG + "url = " + path);
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            // 设置连接超时为5秒
//            conn.setConnectTimeout(10000);
//            // 设置请求类型为Get类型
//            conn.setRequestMethod("GET");
//            conn.connect();
//            // 判断请求Url是否成功
//            if (conn.getResponseCode() != 200) {
//                throw new RuntimeException("请求url失败");
//            }
//            // 获取响应的输入流对象
//            InputStream is = conn.getInputStream();
//            // 创建字节输出流对象
//            ByteArrayOutputStream message = new ByteArrayOutputStream();
//            // 定义读取的长度
//            int len = 0;
//            // 定义缓冲区
//            byte buffer[] = new byte[1024];
//            // 按照缓冲区的大小，循环读取
//            while ((len = is.read(buffer)) != -1) {
//                // 根据读取的长度写入到os对象中
//                message.write(buffer, 0, len);
//            }
//            message.flush();
//            // 释放资源
//            is.close();
//            message.close();
//            // 返回字符串
//            String msg = new String(message.toByteArray());
//            LoggerUtils.d(TAG + "msg = " + msg);
//            return msg;
//        } catch (Exception e) {
//            LoggerUtils.d(TAG + "异常 ：：" + e.getMessage());
//            e.printStackTrace();
//        }
//        return "";
//    }
}
