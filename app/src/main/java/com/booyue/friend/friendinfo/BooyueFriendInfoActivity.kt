package com.booyue.friend.friendinfo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Message
import android.widget.Toast
import com.booyue.base.BaseActivity
import com.booyue.monitor.R
import com.tencent.av.VideoController
import com.tencent.av.VideoController.ACTION_VIDEO_QOS_NOTIFY
import com.tencent.av.core.VideoConstants
import com.tencent.device.FriendInfo
import com.tencent.device.QLog
import com.tencent.device.TXDeviceService
import com.tencent.devicedemo.ListItemInfo
import com.tencent.util.ImageUtils
import kotlinx.android.synthetic.main.activity_friendinfo_booyue.*
import java.util.*

/**
 * Created by Tianluhua on 2018\11\5 0005.
 */
class BooyueFriendInfoActivity : BaseActivity() {

    companion object {
        val TAG = "BooyueFriendInfoActivity"
    }

    private var mIsReceiver = false
    private var mPeerId = ""
    private var mDinType = 0
    private var mSelfDin = ""
    private var mBroadcastHandler: BroadcastHandler? = null

    private var mHandler: Handler? = null
    private val mSetFetching = HashSet<Long>()
    private var imageUtils: ImageUtils? = null


    override fun setView() {
        setContentView(R.layout.activity_friendinfo_booyue)
        val intent = super.getIntent()
        if (intent == null) {
            return
        }
        mPeerId = intent.getStringExtra("peerid")
        mDinType = intent.getIntExtra("dinType", VideoController.UINTYPE_QQ)
        mSelfDin = VideoController.getInstance().GetSelfDin()
        mIsReceiver = intent.getBooleanExtra("receive", false)
        if (java.lang.Long.parseLong(mPeerId) == 0L || java.lang.Long.parseLong(mSelfDin) == 0L) {
            QLog.e(TAG, QLog.CLR, "invalid peerId: $mPeerId invalid selfDin: $mSelfDin")
            finish()
        }
        mBroadcastHandler = BroadcastHandler()
        val filter = IntentFilter()
        filter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT)
        filter.addAction(VideoController.ACTION_NETMONIOTR_INFO)
        filter.addAction(ACTION_VIDEO_QOS_NOTIFY)
        filter.addAction(VideoController.ACTION_CHANNEL_READY)
        filter.addAction(VideoController.ACTION_NET_LEVEL)
        filter.addAction(VideoController.ACTION_NET_BAD)
        filter.addAction(TXDeviceService.BinderListChange)
        filter.addAction(TXDeviceService.OnEraseAllBinders)
        registerReceiver(mBroadcastHandler, filter)
    }

    override fun initView() {
        tv_back.setOnClickListener {
            finish()
        }
        val friendInfo = VideoController.getInstance().getFriendInfo(mPeerId)
        tv_name.setText(friendInfo.name)
        imageUtils = ImageUtils(mHandler, mSetFetching)
        mHandler = object : Handler(this.mainLooper) {
            override fun handleMessage(msg: Message) {
                setBitmapToImageView(friendInfo)
            }
        }
        imageUtils = ImageUtils(mHandler, mSetFetching)
        setBitmapToImageView(friendInfo)
    }

    override fun initData() {
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastHandler)
    }

    fun setBitmapToImageView(friendInfo: FriendInfo) {
        val bitmap = imageUtils!!.getBinderHeadPic(java.lang.Long.parseLong(friendInfo.uin), ListItemInfo.LISTITEM_TYPE_BINDER)
        if (bitmap == null) {
            imageUtils!!.fetchBinderHeadPic(java.lang.Long.parseLong(friendInfo.uin), friendInfo.headUrl)
        } else {
            iv_avatar.setImageBitmap(bitmap)
        }
    }


    inner class BroadcastHandler : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                return
            }
            val action = intent.action
            when (action) {
                VideoConstants.ACTION_STOP_VIDEO_CHAT -> {

                }

                VideoController.ACTION_NETMONIOTR_INFO -> {

                }

                VideoController.ACTION_CHANNEL_READY -> {

                }
                TXDeviceService.BinderListChange -> {

                }

                TXDeviceService.OnEraseAllBinders -> {

                }

                VideoController.ACTION_VIDEO_QOS_NOTIFY -> {

                }

                VideoController.ACTION_NET_LEVEL -> {
                    // 双方网络分级selfNetLevel和peerNetLevel表示自己和对方的网络状况，1-3依次为好，一般，差
                    val tips = intent.getStringExtra("displayNetState")
                    if (tips != null) {
                        Toast.makeText(context, tips, Toast.LENGTH_SHORT).show()
                    }
                }

                /**
                 * 网络差的情况
                 */
                VideoController.ACTION_NET_BAD -> {

                }
            }
        }

    }
}