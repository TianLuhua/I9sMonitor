package com.tencent.util;

import android.util.Log;

public class LoggerUtils {
	
	private static boolean isOpen = true;
	private static String TAG = "LoggerUtils";

	public static void i(String msg){
		if(isOpen && msg!= null){
			Log.i(TAG, msg);
		}
	}
	public static void d(String msg){
		if(isOpen&& msg!= null){
			Log.d(TAG, msg);
		}
	}

		public static void d(String tag,String msg){
		if(isOpen&& msg!= null){
			Log.d(TAG, tag + "---->" + msg);

		}
	}


	public static void e(String msg){
		if(isOpen&& msg!= null){
			Log.e(TAG, msg);
		}
	}
	public static void format_debug(String format,Object... args){
		if(isOpen&& args!= null && format != null){
			Log.d(TAG, String.format(format,args));
		}
	}
	public static void setDebug(boolean debug){
		isOpen = debug;
	}
}

