package com.booyue.monitor.videomonitor

import android.app.Service
import android.content.Context
import com.booyue.monitor.BooyueVideoMonitorService
import com.tencent.av.VideoController
import com.tencent.av.camera.VcCamera
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

    lateinit var mCamera: VcCamera
    lateinit var mPeerId: String
    lateinit var mContext: Context
    var mIsReceiver = false


    override fun start(service: Service, peerId: String) {
        this.mContext = service.application
        this.mPeerId = peerId
        this.mIsReceiver = true
        this.mCamera = VideoController.getInstance().camera
        VideoController.getInstance().execute(AsyncOpenCamera(), null)
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
        VideoController.getInstance().execute({
            if (mCamera != null) {
                mCamera.closeCamera()
            }
        }, null)
        VideoController.getInstance().stopRing()
        if (mIsReceiver) {
            VideoController.getInstance().closeVideo(mPeerId)
        }
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess()
        }
    }

}