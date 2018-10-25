package com.tencent.device;

import android.text.TextUtils;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Created by lingyuhuang on 2016/9/19.
 */

public class FriendInfo implements Parcelable {

    public static final int TYPE_BINDER_OWNER = 1;
    public static final int TYPE_BINDER_SHARER = 2;
    public static final int TYPE_FRIEND_DEVICE = 3;
    public static final int TYPE_FRIEND_QQ = 4;// QQ好友发私聊消息应该存储的用户

    public static final int TYPE_OTHERS = -1;

    public int type;
    public String uin;//对面的uin或者din或者openid
    public String headUrl; //设备头像url
    public String nickName; //绑定者QQ昵称或备注
    public String devName; //如果type为TYPE_FRIEND_DEVICE，有好友的设备名称
    public int devType;// 如果type为TYPE_FRIEND_DEVICE，有设备的类型
    public String devDiscript;   //设备类型（字符串描述，如电视）

    public FriendInfo() {

    }

    public FriendInfo(String uin) {
        this.uin = uin;
    }

    protected FriendInfo(Parcel in) {
        type = in.readInt();
        uin = in.readString();
        headUrl = in.readString();
        nickName = in.readString();
        devName = in.readString();
        devType = in.readInt();
        devDiscript = in.readString();
    }

    public static final Creator<FriendInfo> CREATOR = new Creator<FriendInfo>() {
        @Override
        public FriendInfo createFromParcel(Parcel in) {
            return new FriendInfo(in);
        }

        @Override
        public FriendInfo[] newArray(int size) {
            return new FriendInfo[size];
        }
    };

    public String getContent() {
        switch (type) {
            case TYPE_BINDER_OWNER:
                return "[主人]";
            case TYPE_BINDER_SHARER:
                return "[授权者]";
            case TYPE_FRIEND_DEVICE:
                return "[设备好友]";
        }
        return "";
    }

    public String getName() {
        if (!TextUtils.isEmpty(devName)) {
            return devName;
        }
        if (!TextUtils.isEmpty(nickName)) {
            if (type == TYPE_FRIEND_DEVICE) {
                return nickName + "的" + devDiscript;
            }
            return nickName;
        }
        return uin + "";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FriendInfo) {
            FriendInfo info = (FriendInfo) o;
            return info.uin == uin;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(type);
        parcel.writeString(uin);
        parcel.writeString(headUrl);
        parcel.writeString(nickName);
        parcel.writeString(devName);
        parcel.writeInt(devType);
        parcel.writeString(devDiscript);
    }
}
