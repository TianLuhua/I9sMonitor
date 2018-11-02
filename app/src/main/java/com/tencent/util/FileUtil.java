package com.tencent.util;


import com.booyue.MonitorApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 2018/3/7.09:18
 */

public class FileUtil {
    /**
     * 删除串号文件
     */
    public static void cleanSNFile() {
        if (isSNCached()) {//判断是否文件存在
            File file = new File(MonitorApplication.Companion.getContext().getFilesDir(), "serial.txt");
            if (file != null && file.exists()) {
                file.delete();
            }
        }

    }

    /**
     * 将串号写入文件
     *
     * @param serialNumber 串号
     */
    public static void saveSN(String serialNumber) {
        File file = new File(MonitorApplication.Companion.getContext().getFilesDir(), "serial.txt");
        FileOutputStream fileOutputStream = null;
        if (!file.exists()) {//如果不存在就写入文件
            try {
                file.createNewFile();
                fileOutputStream = new FileOutputStream(file);
                //将数据写入文件
                fileOutputStream.write(serialNumber.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 判断串号是否写入文件
     *
     * @return true 写入过，false 没有写入
     */
    public static boolean isSNCached() {
        File file = new File(MonitorApplication.Companion.getContext().getFilesDir(), "serial.txt");
        if (file != null && file.exists() && file.length() != 0) {
            return true;
        } else {
            return false;
        }
    }
}
