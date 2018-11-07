package com.booyue.monitor

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.booyue.*
import com.booyue.monitor.videomonitor.VideoMonitorSF
import com.tencent.av.VideoController
import com.tencent.av.core.VideoConstants
import com.tencent.device.TXBinderInfo
import com.tencent.device.TXDataPoint
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
            //接受到H5手Q端的信息
            filter.addAction(TXDeviceService.OnReceiveDataPoint)
            //添加电量变化监听
            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(mBroadcasterHandler, filter)
            mVideoMonitor = VideoMonitorSF(applicationContext, mPeerId)
            mVideoMonitor.start()
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
                    if (mPeerId == null && "".equals(mPeerId)) {
                        LoggerUtils.e("ACTION_BATTERY_CHANGED mPeerId IS NULL!")
                        return
                    }
                    val dataPoint = TXDataPoint()
                    dataPoint.property_id = TXDATAPOINT_BATTERY_CHANG_ID
                    dataPoint.property_val = "$level"
                    val arrayDataPoint = arrayOf(dataPoint)
                    TXDeviceService.ackDataPoint(mPeerId.toLong(), arrayDataPoint)
                    LoggerUtils.e("ACTION_BATTERY_CHANGED  LEVEL：$level")
                }
                //接受到手Q端的信息
                TXDeviceService.OnReceiveDataPoint -> {
                    if (mVideoMonitor == null) {
                        LoggerUtils.e("BooyueVideoMonitorService IVideoMonitor is Null")
                        return
                    }
                    val from = intent.extras!!.getLong("from", 0)
                    if (from == mPeerId.toLong()) {
                        val arrayDataPoint = intent.extras!!.getParcelableArray("datapoint")
                        arrayDataPoint.forEach {
                            it as TXDataPoint
                            val strText = "BooyueVideoMonitorService DataPoint Property ID：" + it.property_id + "   Property Value：" + it.property_val
                            LoggerUtils.e(strText)
                            when (it.property_id) {
                                TXDATAPOINT_CAMERA_ID -> {
                                    //控制Camera
                                    val value = it.property_val
                                    if (value !in arrayListOf(TXDATAPOINT_CAMERA_ON, TXDATAPOINT_CAMERA_OFF)) {
                                        LoggerUtils.e("BooyueVideoMonitorService IVideoMonitor CAMERA DataPoint value {$value}")
                                        return
                                    }
                                    var operation: CameraOperation? = null
                                    when (value) {
                                        TXDATAPOINT_CAMERA_ON -> {
                                            operation = CameraOperation.ON(value)
                                        }
                                        TXDATAPOINT_CAMERA_OFF -> {
                                            operation = CameraOperation.OFF(value)
                                        }
                                    }
                                    if (operation == null) {
                                        LoggerUtils.e("BooyueVideoMonitorService IVideoMonitor CAMERA Operation Is Null")
                                        return
                                    }
                                    mVideoMonitor.setCameraStatu(operation)
                                }

                                TXDATAPOINT_MICPHONE_ID -> {
                                    //控制MicPhone
                                    val value = it.property_val
                                    if (value !in arrayListOf(TXDATAPOINT_MICPHONE_ON, TXDATAPOINT_MICPHONE_OFF)) {
                                        LoggerUtils.e("BooyueVideoMonitorService IVideoMonitor MICPHONE DataPoint value {$value}")
                                        return
                                    }
                                    var operation: MicPhoneOperation? = null
                                    when (value) {
                                        TXDATAPOINT_MICPHONE_ON -> {
                                            operation = MicPhoneOperation.ON(value)
                                        }
                                        TXDATAPOINT_MICPHONE_OFF -> {
                                            operation = MicPhoneOperation.OFF(value)
                                        }
                                    }
                                    if (operation == null) {
                                        LoggerUtils.e("BooyueVideoMonitorService IVideoMonitor MICPHONE Operation Is Null")
                                        return
                                    }

                                    mVideoMonitor.setMicPhoneStatu(operation)

                                }

                                TXDATAPOINT_WHEEL_ID -> {
                                    //控制驱动轮
                                    val value = it.property_val
                                    if (it.property_val !in arrayListOf(TXDATAPOINT_WHEEL_FORWARD, TXDATAPOINT_WHEEL_BACKWARD, TXDATAPOINT_WHEEL_LEFT, TXDATAPOINT_WHEEL_RIGHT)) {
                                        LoggerUtils.e("BooyueVideoMonitorService IVideoMonitor WHEEL DataPoint value {$value}")
                                        return
                                    }
                                    var operation: WheelOperation? = null
                                    when (value) {
                                        TXDATAPOINT_WHEEL_FORWARD -> {
                                            operation = WheelOperation.Forward(value)
                                        }
                                        TXDATAPOINT_WHEEL_BACKWARD -> {
                                            operation = WheelOperation.Backward(value)
                                        }
                                        TXDATAPOINT_WHEEL_LEFT -> {
                                            operation = WheelOperation.Left(value)
                                        }
                                        TXDATAPOINT_WHEEL_RIGHT -> {
                                            operation = WheelOperation.Right(value)
                                        }
                                    }
                                    if (operation == null) {
                                        LoggerUtils.e("BooyueVideoMonitorService IVideoMonitor WHEEL Operation Is Null")
                                        return
                                    }
                                    mVideoMonitor.operationWheel(operation)

                                }
                            }

                        }
                    }
                }
                else -> {
                    LoggerUtils.e("没有定义的Action")
                }

            }

        }

    }
}