SDK 版本1.6.205
1.BooyueFriendListActivity.java 的initview中判断获取串号：
             如果本地缓存没有，从服务器获取，并保存到本地,在异步回调中执行startService();
                       I6S+通过mac地址作为唯一标志符获取对应的串号
                       T6通过sn作为唯一标志符获取对应的串号
             如果本地缓存存在，从本地获取，并执行startService();
2.朋友列表页面和音频通话页面针对T6设备头像偏下的问题做了微调，启动页和application的主题改成了透明，避免了启动黑屏问题。
3.删除视频聊天页面扬声器按钮，发布2.0.4

2018-5-15
    AudioDeviceInterface  将音频数据保存到文件
    查看日志音频数据是否正常

2018-6-11
    音频通话在电话接通的时候触发护眼模式，导致已接通，然后播放来电音乐，
    现通过标志位进行控制

2018-6-21
    关闭本地摄像头无效，手机可以
2018-6-22 V2.0.6
    1、修复关闭本地摄像头无效问题，对广播ACTION_VIDEO_QOS_NOTIFY 不作处理
    2、既然音频通话页面的音量切换按钮去除
    3、修复按home键以后，手机端还在显示通话  解决方案：在onresume注册监听，onPause中取消监听，并关闭视频
2018-6-25 V2.0.6
    1、修复自己摄像头和对方画面切换后，关闭摄像头，关闭的是对方的画面的bug
2018-6-29
    1、修复大画面显示自己，且关闭摄像头，小画面显示对方，点击小画面，大画面不显示对方画面的bug
    2、扫描页面对于二维码的生成进行了提示，

2018-9-26 v2.0.7
    1、i6s pro未在管控之内，未获取到串号，现在进行兼容
    
2018-10-25
    1.增加视屏时候关闭/开启Microphone功能.

2018-10-30 
    1.音频聊天界面增加 关闭/开启 Microphone功能
   
2018-11-02
    1.和H5同事商量好进行DataPoint通信，接受到Property_id为:100006162,Property_value为：type时，回复Property_id为:100006162，Property_value为：i9s。
    让其进行对应的逻辑操作

