package com.tencent.device;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class TXFriendInfo implements Parcelable {

    public long friend_din = 0;    //设备好友din
    public String head_url = "";    //设备头像url
    public int device_type = 0;    //设备类型
    public String str_device_type = "";   //设备类型（字符串描述，如电视）
    public byte[] admin_remark = new byte[0];   //设备主人的智能备注，请通过getAdminRemark()方法获取该字段
    public byte[] device_name = new byte[0];    //设备名，请通过getDeviceName()方法获取该字段

    public TXFriendInfo() {

    }

    public TXFriendInfo(Parcel parcel) {
//		this.friend_din			= parcel.readLong();
//		this.head_url		    = parcel.readString();
//		this.device_type			= parcel.readInt();
//		this.str_device_type	    = parcel.readString();
//		if (this.admin_remark != null) {
//			parcel.readByteArray(this.admin_remark);
//		}
//		if (this.device_name != null) {
//			parcel.readByteArray(this.device_name);
//		}
        Bundle bundle = parcel.readBundle();
        if (bundle != null) {
            this.friend_din = bundle.getLong("din", 0);
            this.head_url = bundle.getString("url");
            this.device_type = bundle.getInt("devtype", 0);
            this.str_device_type = bundle.getString("strdevtype");
            this.admin_remark = bundle.getByteArray("adminremark");
            this.device_name = bundle.getByteArray("devname");
        }

    }

    public String getAdminRemark() {
        String strAdminRemark = this.admin_remark.toString();
        try {
            strAdminRemark = new String(this.admin_remark, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return strAdminRemark;
    }

    public String getDeviceName() {
        String strDevName = this.device_name.toString();
        try {
            strDevName = new String(this.device_name, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return strDevName;
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        // TODO Auto-generated method stub
//		parcel.writeLong(this.friend_din);
//		parcel.writeString(this.head_url);
//		parcel.writeInt(this.device_type);
//		parcel.writeString(this.str_device_type);
//		parcel.writeByteArray(this.admin_remark);
//		parcel.writeByteArray(this.device_name);
        Bundle bundle = new Bundle();
        bundle.putLong("din", this.friend_din);
        bundle.putString("url", this.head_url);
        bundle.putInt("devtype", this.device_type);
        bundle.putString("strdevtype", this.str_device_type);
        bundle.putByteArray("adminremark", this.admin_remark);
        bundle.putByteArray("devname", this.device_name);
        parcel.writeBundle(bundle);
    }

    public static final Parcelable.Creator<TXFriendInfo> CREATOR = new Parcelable.Creator<TXFriendInfo>() {

        @Override
        public TXFriendInfo createFromParcel(Parcel parcel) {
            // TODO Auto-generated method stub
            return new TXFriendInfo(parcel);
        }

        @Override
        public TXFriendInfo[] newArray(int size) {
            // TODO Auto-generated method stub
            return new TXFriendInfo[size];
        }
    };

}
