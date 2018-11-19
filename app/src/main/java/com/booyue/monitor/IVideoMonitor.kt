package com.booyue.monitor

import com.booyue.CameraOperation
import com.booyue.MicPhoneOperation
import com.booyue.WheelOperation

/**
 * Created by Tianluhua on 2018\11\6 0006.
 */
interface IVideoMonitor {
    //开启监控
    fun start()

    //关闭监控
    fun stop()

    //设置MicPhone的状态
    fun setMicPhoneStatu(operation: MicPhoneOperation)

    //设置Camera的状态
    fun setCameraStatu(operation: CameraOperation)

    //操作驱动轮
    fun operationWheel(operation: WheelOperation)

    //切换到视屏聊天界面
    fun switchToVideoChat()

    fun resetEncoder(width: Int, height: Int, bitrate: Int, fps: Int)

    fun setVideoConnected(videoConnected: Boolean)
}