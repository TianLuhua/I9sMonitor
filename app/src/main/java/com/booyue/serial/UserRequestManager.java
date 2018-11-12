package com.booyue.serial;


import com.booyue.utils.LoggerUtils;

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
        LoggerUtils.Companion.d(TAG + "mac = " + mac);
        LoggerUtils.Companion.d(TAG + "channel = hhtjgs");
        LoggerUtils.Companion.d(TAG + "product = " + productModel);
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
        LoggerUtils.Companion.d(TAG + "versionName = " + versionName);
        RequestBody requestBody = new FormBody.Builder()
                .add("version", versionName)
                .build();
        LoggerUtils.Companion.d(TAG + "upgrade url = " + UPGRADE_URL + "version=" + versionName);
        RequestManager.post(UPGRADE_URL, requestBody, callback);
    }

    /**
     * 获取I6S串号请求
     *
     * @param mac 唯一标识（mac地址）
     * @return json字符串
     */
    public static void getI6SSerialNumber(String mac, Callback callback) {
        LoggerUtils.Companion.d(TAG + "mac = " + mac);
        String path = CHENXIN_GET_SERIAL_NUMBER + mac;
        LoggerUtils.Companion.d(TAG + "path= " + path);
        RequestManager.get(path, callback);
    }
}
