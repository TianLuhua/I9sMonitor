package com.tencent;

/**
 * Created by Administrator on 2018/6/21.16:12
 */

public class BooyueVideoMonitorManager {
    private static BooyueVideoMonitorManager sBooyueVideoMonitorManager;
    private BooyueVideoMonitorManager(){

    }
    public static BooyueVideoMonitorManager getInstance(){
        if (sBooyueVideoMonitorManager == null){
            synchronized (BooyueVideoMonitorManager.class){
                if(sBooyueVideoMonitorManager == null){
                    sBooyueVideoMonitorManager = new BooyueVideoMonitorManager();
                }
            }
        }
        return sBooyueVideoMonitorManager;
    }


}
