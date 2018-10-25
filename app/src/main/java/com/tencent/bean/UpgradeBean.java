package com.tencent.bean;

/**
 * Created by Administrator on 2018/4/26.10:49
 * 升级实体类
 */

public class UpgradeBean {
    public String ret;
    public String msg;
    public Content content;
    public class Content{
        public String apk;
        public String newVersion;
        public String tips;
    }
}
