package com.booyue.monitor.videomonitor

import android.app.Service
import android.content.Context
import android.widget.Toast
import com.booyue.CameraOperation
import com.booyue.MicPhoneOperation
import com.booyue.WheelOperation
import com.booyue.monitor.IVideoMonitor
import com.tencent.av.VideoController
import com.tencent.av.camera.VcCamera
import com.tencent.av.thread.Future
import com.tencent.av.thread.FutureListener
import com.tencent.device.QLog
import com.tencent.device.TXDeviceService
import com.tencent.util.LoggerUtils

/**
 * Created by Tianluhua on 2018\10\31 0031.
 */
class VideoMonitorSF : IVideoMonitor {

    companion object {
        val TAG = "VideoMonitorSF"
    }

    var mCamera: VcCamera
    var mPeerId: String
    var mContext: Context
    var mIsReceiver = false

    constructor(service: Service, peerId: String) {
        this.mContext = service.application
        this.mPeerId = peerId
        this.mCamera = VideoController.getInstance().camera
    }

    /**
     * 开启监控
     */
    override fun start() {
        this.mIsReceiver = true
        VideoController.getInstance().execute(AsyncOpenCamera(), object : FutureListener<Boolean> {
            override fun onFutureDone(p0: Future<Boolean>?) {
                //打开Camera完成！
            }
        })
    }

    /**
     * 停止监控
     */
    override fun stop() {
        terminateVideo()
    }

    override fun resetEncoder(width: Int, height: Int, bitrate: Int, fps: Int) {

    }

    override fun setVideoConnected(videoConnected: Boolean) {
    }

    /**
     * MicPhone操作
     * operation:
     *   MicPhoneOperation.ON 打开MicPhone
     *   MicPhoneOperation.OFF 关闭MicPhone
     */
    override fun setMicPhoneStatu(operation: MicPhoneOperation) {
        if (mIsReceiver) {
            var mute = false
            when (operation) {
                is MicPhoneOperation.ON -> {
                    mute = true
                }
                is MicPhoneOperation.OFF -> {
                    mute = false
                }
            }
            VideoController.getInstance().setSelfMute2(mute)
        }
    }

    /**
     *Camera操作
     * operation：
     *   CameraOperation.ON 打开Camera
     *   CameraOperation.OFF 关闭Camera
     */
    override fun setCameraStatu(operation: CameraOperation) {
        if (mIsReceiver) {
            var runnable: Runnable
            when (operation) {
                is CameraOperation.ON -> {
                    runnable = AsyncOpenCamera()
                }
                is CameraOperation.OFF -> {
                    runnable = AsyncCloseCamera()
                }
            }
            VideoController.getInstance().execute(runnable)
        }
    }

    /**
     * 驱动轮操作
     * operation：
     *   WheelOperation.Forward 前
     *   WheelOperation.Backward 后
     *   WheelOperation.Left 左
     *   WheelOperation.Right 右
     */
    override fun operationWheel(operation: WheelOperation) {
        when (operation) {
            is WheelOperation.Forward -> {
                Toast.makeText(mContext, "向前", Toast.LENGTH_SHORT).show()
            }
            is WheelOperation.Backward -> {
                Toast.makeText(mContext, "向后", Toast.LENGTH_SHORT).show()
            }
            is WheelOperation.Left -> {
                Toast.makeText(mContext, "向左", Toast.LENGTH_SHORT).show()
            }
            is WheelOperation.Right -> {
                Toast.makeText(mContext, "向右", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 断开与手Q连接
     */
    private fun terminateVideo() {
        mIsReceiver = false
        VideoController.getInstance().execute(AsyncCloseCamera(), object : FutureListener<Boolean> {
            override fun onFutureDone(p0: Future<Boolean>?) {
                //关闭Camera完成！
            }
        })
        VideoController.getInstance().stopRing()
        if (mIsReceiver) {
            VideoController.getInstance().closeVideo(mPeerId)
        }
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess()
        }
    }

    /**
     * 打开Camera
     */
    inner class AsyncOpenCamera : Runnable {

        override fun run() {
            try {
                if (mCamera == null || !mCamera.openCameraWithSilent()) {
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera failed to start camera.")
                    }
                    return
                } else {
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera success.")
                    }
                }
            } catch (e: Exception) {
                LoggerUtils.e("AsyncOpenCamera  Fail，" + e.message)
            }

        }
    }

    /**
     * 关闭Camera
     */
    inner class AsyncCloseCamera : Runnable {
        override fun run() {
            if (mCamera != null) {
                mCamera.closeCamera()
            }
        }
    }

}