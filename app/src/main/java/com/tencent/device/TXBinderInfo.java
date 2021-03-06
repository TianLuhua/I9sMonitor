package com.tencent.device;

import android.os.Parcel;
import android.os.Parcelable;

public class TXBinderInfo implements Parcelable {
    public static int BINDER_TYPE_OWNER = 1;
    public static int BINDER_TYPE_SHARER = 2;
    public static int BINDER_GENDER_MALE = 0;
    public static int BINDER_GENDER_FEMALE = 1;

    public int binder_type;   //绑定者类型
    public long tinyid;       //绑定者的tinyid
    public byte[] nick_name;  //绑定者QQ昵称
    public int binder_gender; //绑定者性别
    public String head_url;   //绑定者QQ头像

    public TXBinderInfo() {
    }

    public TXBinderInfo(Parcel parcel) {
        this.binder_type = parcel.readInt();
        this.tinyid = parcel.readLong();

        String nickName = parcel.readString();
        if (nickName != null && nickName.length() > 0) {
            this.nick_name = nickName.getBytes();
        }

        this.binder_gender = parcel.readInt();
        this.head_url = parcel.readString();
    }

    public String getNickName() {
        if (this.nick_name != null && this.nick_name.length > 0) {
            String nickName = this.nick_name.toString();
            try {
                nickName = new String(nick_name, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return nickName;
        } else {
            return "";
        }
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        // TODO Auto-generated method stub
        parcel.writeInt(this.binder_type);
        parcel.writeLong(this.tinyid);
        parcel.writeString(this.getNickName());
        parcel.writeInt(this.binder_gender);
        parcel.writeString(this.head_url);
    }

    public static final Parcelable.Creator<TXBinderInfo> CREATOR = new Parcelable.Creator<TXBinderInfo>() {
        @Override
        public TXBinderInfo createFromParcel(Parcel parcel) {
            // TODO Auto-generated method stub
            return new TXBinderInfo(parcel);
        }

        @Override
        public TXBinderInfo[] newArray(int size) {
            // TODO Auto-generated method stub
            return new TXBinderInfo[size];
        }
    };
}
