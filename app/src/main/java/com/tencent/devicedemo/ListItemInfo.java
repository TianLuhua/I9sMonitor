package com.tencent.devicedemo;

public class ListItemInfo {
    public final static int LISTITEM_TYPE_BINDER = 1; //绑定者
    public final static int LISTITEM_TYPE_FRIEND = 2; //设备好友
    public final static int LISTITEM_TYPE_ADD_FRIEND = 3; //添加设备好友

    public String nick_name; //名称
    public String head_url; //头像url
    public long id; //tinyid或者din
    public int type; //类型：绑定者或者设备好友


    public ListItemInfo() {
        type = LISTITEM_TYPE_BINDER;
    }

}
