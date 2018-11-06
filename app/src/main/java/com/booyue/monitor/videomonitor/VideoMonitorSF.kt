package com.booyue.monitor.videomonitor

import android.app.Service
import android.content.Context
import android.widget.Toast
import com.booyue.monitor.BooyueVideoMonitorService
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
class VideoMonitorSF : BooyueVideoMonitorService.IVideoMonitor {

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

    override fun start() {
        this.mIsReceiver = true
        VideoController.getInstance().execute(AsyncOpenCamera(), object : FutureListener<Boolean> {
            override fun onFutureDone(p0: Future<Boolean>?) {
                //打开Camera完成！
            }
        })
    }

    override fun stop() {
        terminateVideo()
    }

    override fun resetEncoder(width: Int, height: Int, bitrate: Int, fps: Int) {

    }

    override fun setVideoConnected(videoConnected: Boolean) {
    }

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

    private fun terminateVideo() {
        VideoController.getInstance().execute {
            if (mCamera != null) {
                mCamera.closeCamera()
            }
        }
        VideoController.getInstance().stopRing()
        if (mIsReceiver) {
            VideoController.getInstance().closeVideo(mPeerId)
        }
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess()
        }
    }

}