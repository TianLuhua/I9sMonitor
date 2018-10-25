package com.tencent.device;
import com.tencent.device.FriendInfo;

interface ITXDeviceService{
    long getSelfDin();
    FriendInfo getFriendInfo(String uin);
    byte[] getVideoChatSignature();
    void notifyVideoServiceStarted();
    void sendVideoCall(long peerUin, int uinType, in byte[] msg);
    void sendVideoCallM2M(long peerUin, int uinType, in byte[] msg);
    void sendVideoCMD(long peerUin, int uinType, in byte[] msg);
    void sendLANCommunicationCMD(in byte[] buffer);
    void reportCommunicationTime(long peerUin,long commTime,int type);
}