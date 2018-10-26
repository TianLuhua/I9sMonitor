package com.booyue.serial;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2018/4/26.10:59
 */

public class RequestManager {

    public static void post(String url, RequestBody requestBody, Callback callback){
        Request req = new Request.Builder().url(url).post(requestBody).build();
        Call call = new OkHttpClient().newCall(req);
        call.enqueue(callback);
    }

    public static void get(String url,Callback callback){
        Request request = new Request.Builder().url(url).build();
        Call call = new OkHttpClient().newCall(request);
        call.enqueue(callback);
    }
}
