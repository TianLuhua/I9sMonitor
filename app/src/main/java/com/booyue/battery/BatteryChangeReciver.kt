package com.booyue.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by Tianluhua on 2018\10\25 0025.
 */
class BatteryChangeReciver : BroadcastReceiver() {

    /**
     * 监听器容器
     */
    val listeners = arrayListOf<BatteryChangeListener>()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra("level", 0)
        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                if (listeners.size > 0) {
                    listeners.forEach {
                        it.onBatteryChange(level)
                    }
                }
            }
        }
    }

    /**
     * 添加监听器
     */
    fun addBatteryChangeListener(batteryChangeListener: BatteryChangeListener) {
        if (!listeners.contains(batteryChangeListener)) {
            listeners.add(batteryChangeListener)
        }
    }

    /**
     * 移除监听器
     */
    fun removeBatteryChangeListener(batteryChangeListener: BatteryChangeListener) {
        if (listeners.contains(batteryChangeListener)) {
            listeners.remove(batteryChangeListener)
        }
    }

    /**
     * 清空监听容器
     */
    fun cleanBatteryChangeListener() {
        listeners.clear()
    }

    /**
     *电量监听
     */
    interface BatteryChangeListener {
        fun onBatteryChange(level: Int)
    }

}