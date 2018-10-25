package com.tencent.util;

import android.os.StatFs;

/**
 * Created by Administrator on 2017/5/27.
 */
public class TimeUtils {

    public static String long2TimeFormat(long startTime) {
        long currentTime = System.currentTimeMillis();
        long time = (currentTime - startTime) / 1000;
        int hour = (int) (time / 3600);
        StringBuilder sb = new StringBuilder();
        if (hour >= 1) {
            if (hour < 10) {
                sb.append("0");
            }
            sb.append(hour);
            sb.append(":");
        }

        int min = (int) (time / 60);
        int sec = (int) (time % 60);
        if (min < 10) {
            sb.append("0");
        }
        sb.append(min);
        sb.append(":");
        if (sec < 10) {
            sb.append("0");
        }
        sb.append(sec);
        return sb.toString();
    }

    public static long getAvailableSize(String path){
        StatFs sf = new StatFs(path);
//		StatFs sf = new StatFs(fullPath);
        long blockSize = sf.getBlockSize();
        long availableBlocks = sf.getAvailableBlocks();
        return blockSize * availableBlocks;
//		return Formatter.formatFileSize(context, blockSize*availableBlocks);
    }
}
