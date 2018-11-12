package com.booyue.audiochat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.booyue.base.BaseActivity
import com.booyue.monitor.R
import com.booyue.serial.SerialNumberManager
import com.booyue.utils.LoggerUtils
import com.tencent.av.VideoController
import com.tencent.av.core.VideoConstants
import com.tencent.device.FriendInfo
import com.tencent.device.TXBinderInfo
import com.tencent.device.TXDeviceService
import com.tencent.devicedemo.ListItemInfo
import com.tencent.util.ImageUtils
import kotlinx.android.synthetic.main.activity_audio_chat_booyue.*
import java.util.*

/**
 * Created by Tianluhua on 2018\10\30 0030.
 */
class BooyueAudioChatActivity : BaseActivity() {

    companion object {
        val TAG = "BooyueAudioChatActivity"
    }

    private var mIsReceiver = false
    private lateinit var mPeerId: String
    private var mDinType = 0
    private lateinit var mSelfDin: String
    private var startTime = 0.toLong()
    private lateinit var mBroadcastHandler: BroadcastHandler
    private lateinit var mainHandler: Handler
    private lateinit var imageUtils: ImageUtils
    private var mSetFetching = HashSet<Long>()
    private var isChannelReady: Boolean = false


    override fun setView() {
        setContentView(R.layout.activity_audio_chat_booyue)
        val intent = getIntent()
        if (intent == null) {
            LoggerUtils.d("$TAG intent is null")
            return
        }
        mPeerId = intent.getStringExtra("peerid")
        mDinType = intent.getIntExtra("dinType", VideoController.UINTYPE_QQ)
        mSelfDin = VideoController.getInstance().GetSelfDin()
        //判断是发起者还是接受者
        mIsReceiver = intent.getBooleanExtra("receive", false)
        LoggerUtils.d("$TAG peerid:$mPeerId mDinType:$mDinType mIsReceiver:$mIsReceiver")
        if (mPeerId.toLong() == 0.toLong() || mSelfDin.toLong() == 0.toLong()) {
            finish()
        }
    }

    override fun initView() {
        tv_duration.visibility = View.GONE
        val friendInfo = VideoController.getInstance().getFriendInfo(mPeerId)
        tv_name.text = friendInfo.name
        val devName = friendInfo.devName
        val devType = friendInfo.devType
        LoggerUtils.d(TAG + "deviceName = " + devName + ",deviceType = " + devType)
        ib_speaker_switcher.visibility = View.GONE

        ib_speaker_switcher.setOnClickListener {
            if (VideoController.getInstance().isSelfMute) {
                VideoController.getInstance().setSelfMute2(false)
                ib_speaker_switcher.setImageResource(R.drawable.button_speaker_hi)
            } else {
                VideoController.getInstance().setSelfMute2(true)
                ib_speaker_switcher.setImageResource(R.drawable.button_speaker_nr)
            }
        }

        if (TextUtils.equals(SerialNumberManager.T6_ID, Build.ID)) {
            val layoutParams = iv_avatar.layoutParams as FrameLayout.LayoutParams
            layoutParams.topMargin = resources.getDimension(R.dimen.dimen_34) as Int
            iv_avatar.layoutParams = layoutParams
        }
        mainHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                setBitmapToImageView(friendInfo)
            }
        }
        imageUtils = ImageUtils(mainHandler, mSetFetching)
        setBitmapToImageView(friendInfo)
        if (!mIsReceiver) {
            ll_recieve_before.visibility = View.GONE
        } else {
            tv_cancel.visibility = View.GONE
        }
        tv_cancel.setOnClickListener {
            if (mIsReceiver) {
                VideoController.getInstance().rejectRequestAudio(mPeerId)
            }
            finish()
        }
        tv_hangup.setOnClickListener {
            if (mIsReceiver) {
                VideoController.getInstance().rejectRequestAudio(mPeerId)
            }
            finish()
        }
        tv_receive.setOnClickListener {
            tv_receive.visibility = View.GONE
            VideoController.getInstance().acceptRequestAudio(mPeerId)
        }
    }


    fun setBitmapToImageView(friendInfo: FriendInfo) {
        val bitmap = imageUtils.getBinderHeadPic(java.lang.Long.parseLong(friendInfo.uin), ListItemInfo.LISTITEM_TYPE_BINDER)
        if (bitmap == null) {
            imageUtils.fetchBinderHeadPic(java.lang.Long.parseLong(friendInfo.uin), friendInfo.headUrl)
        } else {
            iv_avatar.setImageBitmap(bitmap)
        }
    }

    override fun initData() {
        mBroadcastHandler = BroadcastHandler()
        val intentFilter = IntentFilter()
        intentFilter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT)
        intentFilter.addAction(VideoController.ACTION_CHANNEL_READY)
        intentFilter.addAction(TXDeviceService.BinderListChange)
        registerReceiver(mBroadcastHandler, intentFilter)
        if (!mIsReceiver) {
            VideoController.getInstance().requestAudio(mPeerId, mDinType)
            tv_state_desc.setText(R.string.wait_friend_receive_audio)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isChannelReady) {
            return
        }
        if (mIsReceiver) {
            VideoController.getInstance().startRing(R.raw.qav_video_incoming, -1, null)
        } else {
            VideoController.getInstance().startRing(R.raw.qav_video_request, -1, null)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        VideoController.getInstance().closeAudio(mPeerId)
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess()
        }
        unregisterReceiver(mBroadcastHandler)
        mainHandler.removeCallbacksAndMessages(null)
    }

    inner class BroadcastHandler : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                LoggerUtils.d(TAG + "BroadcastHandler intent is null")
                return
            }
            val action = intent.action
            when (action) {
                VideoConstants.ACTION_STOP_VIDEO_CHAT -> {
                    val reason = intent.getIntExtra("reason", VideoConstants.VOIP_REASON_OTHERS)
                    val reasonString = when (reason) {
                        //发起请求之后，对方拒绝
                        VideoConstants.VOIP_REASON_REJECT_BY_FRIEND -> context!!.getString(R.string.voip_reason_reject_by_friend)
                        //发起请求之后，对方一直不接听，最后超时
                        VideoConstants.VOIP_REASON_SELF_WAIT_RELAYINFO_TIMEOUT -> context!!.getString(R.string.voip_reason_self_wait_relayinfo_timeout)
                        //连通之后，对方主动关闭
                        VideoConstants.VOIP_REASON_CLOSED_BY_FRIEND -> context!!.getString(R.string.voip_reason_closed_by_friend)
                        //其它原因
                        else -> context!!.getString(R.string.voip_reason_other)
                    }
                    Toast.makeText(context, reasonString, Toast.LENGTH_LONG).show()
                    finish()
                }

                VideoController.ACTION_CHANNEL_READY -> {
                    VideoController.getInstance().stopRing()
                    VideoController.getInstance().stopShake()
                    VideoController.getInstance().startShake()
                    isChannelReady = true
                    startTime = System.currentTimeMillis()
                    mainHandler.post(updateTime)
                    if (tv_state_desc.visibility == View.VISIBLE) {
                        tv_state_desc.visibility = View.GONE
                    }
                    if (tv_duration.visibility == View.GONE) {
                        tv_duration.visibility = View.VISIBLE
                    }
                    if (ib_speaker_switcher.visibility == View.GONE) {
                        ib_speaker_switcher.visibility = View.VISIBLE
                    }
                }

                TXDeviceService.BinderListChange -> {
                    var bFind = false
                    val listBinder = intent.extras.getParcelableArray("binderlist")
                    listBinder.forEach {
                        it as TXBinderInfo
                        if (it.tinyid == mPeerId.toLong()) {
                            bFind = true
                            return
                        }
                    }
                    if (!bFind) {
                        finish()
                    }
                }

            }
        }

    }

    /**
     * 更新时间的线程
     */
    internal var updateTime = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val time = (currentTime - startTime) / 1000
            val min = (time / 60).toInt()
            val sec = (time % 60).toInt()
            val sb = StringBuilder()
            if (min < 10) {
                sb.append("0")
            }
            sb.append(min)
            sb.append(":")
            if (sec < 10) {
                sb.append("0")
            }
            sb.append(sec)
            tv_duration.text = sb.toString()
            //不断的更新时间
            mainHandler.postDelayed(this, 1000)
        }
    }
}