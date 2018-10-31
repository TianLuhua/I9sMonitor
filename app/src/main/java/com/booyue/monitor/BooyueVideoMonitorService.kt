package com.booyue.monitor

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.booyue.monitor.videomonitor.VideoMonitorSF
import com.tencent.av.VideoController
import com.tencent.av.core.VideoConstants
import com.tencent.device.TXBinderInfo
import com.tencent.device.TXDeviceService
import com.tencent.util.LoggerUtils

/**
 * Created by Tianluhua on 2018\10\31 0031.
 */
class BooyueVideoMonitorService : Service() {


    companion object {
        val TAG = "BooyueVideoMonitorService"
    }

    lateinit var mVideoMonitor: IVideoMonitor
    lateinit var mPeerId: String
    val mBroadcasterHandler = BroadcastHandler()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            mPeerId = intent.getStringExtra("peerid")
            val filter = IntentFilter()
            filter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT)
            filter.addAction(VideoController.ACTION_NETMONIOTR_INFO)
            filter.addAction(VideoController.ACTION_CHANNEL_READY)
            filter.addAction(VideoController.ACTION_VIDEO_QOS_NOTIFY)
            filter.addAction(TXDeviceService.BinderListChange)
            filter.addAction(TXDeviceService.OnEraseAllBinders)
            //添加电量变化监听
            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(mBroadcasterHandler, filter)
            mVideoMonitor = VideoMonitorSF()
            mVideoMonitor.start(this, mPeerId)

            VideoController.getInstance().acceptRequest(mPeerId)
        } else {
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcasterHandler)
        mVideoMonitor.setVideoConnected(false)
        mVideoMonitor.stop()
    }

    inner class BroadcastHandler : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                return
            }
            val action = intent.action
            when (action) {
                VideoConstants.ACTION_STOP_VIDEO_CHAT -> {
                    mVideoMonitor.setVideoConnected(false)
                    this@BooyueVideoMonitorService.stopSelf()
                }
                VideoController.ACTION_CHANNEL_READY -> {
                    mVideoMonitor.setVideoConnected(true)
                }
                VideoController.ACTION_NETMONIOTR_INFO -> {
                    val msg = intent.getStringExtra("msg")
                    LoggerUtils.e(TAG + " msg:" + msg)
                }
                VideoController.ACTION_VIDEO_QOS_NOTIFY -> {
                    val width = intent.getIntExtra("width", 0)
                    val height = intent.getIntExtra("height", 0)
                    val bitrate = intent.getIntExtra("bitrate", 0) * 1000
                    val fps = intent.getIntExtra("fps", 0)
                    if (width != 0 && height != 0 && bitrate != 0 && fps != 0) {
                        mVideoMonitor.resetEncoder(width, height, bitrate, fps)
                    }
                }
                TXDeviceService.BinderListChange -> {
                    var bFind = false
                    val listBinder = intent.extras!!.getParcelableArray("binderlist")
                    for (i in listBinder!!.indices) {
                        val binder = listBinder[i] as TXBinderInfo
                        if (binder.tinyid == java.lang.Long.parseLong(mPeerId)) {
                            bFind = true
                            break
                        }
                    }
                    if (bFind == false) {
                        mVideoMonitor.setVideoConnected(false)
                        this@BooyueVideoMonitorService.stopSelf()
                    }
                }
                TXDeviceService.OnEraseAllBinders -> {
                    mVideoMonitor.setVideoConnected(false)
                    this@BooyueVideoMonitorService.stopSelf()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    //电量发生变化，通知手Q端
                    val level = intent.getIntExtra("level", 0)
                    LoggerUtils.e("ACTION_BATTERY_CHANGED  level：$level")
                }
                else -> {
                    LoggerUtils.e("没有定义的Action")
                }

            }

        }

    }


    interface IVideoMonitor {
        fun start(service: Service, peerId: String)

        fun stop()

        fun resetEncoder(width: Int, height: Int, bitrate: Int, fps: Int)

        fun setVideoConnected(videoConnected: Boolean)
    }
}