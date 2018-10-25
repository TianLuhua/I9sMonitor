package com.tencent.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetWorkUtils {
	public static final String TAG = "NetWorkUtils";
	private static ConnectivityManager mConnectivityManager;

	/**
	 * 检查当前网络是否可用
	 * @param context
	 * @return
	 */
	public static boolean isNetWorkAvailable(Context context){
		mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
		if(mNetworkInfo != null && mNetworkInfo.isConnected()){
			return mNetworkInfo.isAvailable();
		}else{
			return false;
		}
	}

	/**
	 *
     *判断当前是否使用的是 WIFI网络
	 */
	public static boolean isWifiActive(Context context){
		ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifiInfo = mConnectivityManager.getActiveNetworkInfo();
		if(mWifiInfo != null && mWifiInfo.isConnected()){
			return mWifiInfo.getType() == ConnectivityManager.TYPE_WIFI;
		}else {
			return false;
		}
	}
}
