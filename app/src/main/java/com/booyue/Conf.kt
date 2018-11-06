package com.booyue


/**
 * Created by Tianluhua on 2018\11\2 0002.
 */

//Bugly app_id
val APP_ID = "9824f36e23"

//QQ物联注册相关参数
var PRODUCT_ID: Long = 0
var SERIAL_NUMBER: String? = null
var LICENSE: String? = null
var SERVER_PUBLIC_KEY: String? = null


//H5通讯相关的DataPoint ID
/**
 * Camera相关
 */
val TXDATAPOINT_CAMERA_ID = 100006163L
val TXDATAPOINT_CAMERA_ON = 1.toString()
val TXDATAPOINT_CAMERA_OFF = 0.toString()

sealed class CameraOperation {
    class ON(val value: String) : CameraOperation()
    class OFF(val value: String) : CameraOperation()
}

/**
 * MicPhone相关
 */
val TXDATAPOINT_MICPHONE_ID = 100006164L
val TXDATAPOINT_MICPHONE_ON = 1.toString()
val TXDATAPOINT_MICPHONE_OFF = 0.toString()

sealed class MicPhoneOperation {
    class ON(val value: String) : MicPhoneOperation()
    class OFF(val value: String) : MicPhoneOperation()
}

/**
 * 驱动轮相关的操作封装
 */
val TXDATAPOINT_WHEEL_ID = 100006164L
val TXDATAPOINT_WHEEL_FORWARD = 1.toString()
val TXDATAPOINT_WHEEL_BACKWARD = 2.toString()
val TXDATAPOINT_WHEEL_LEFT = 3.toString()
val TXDATAPOINT_WHEEL_RIGHT = 4.toString()

sealed class WheelOperation {
    class Forward(val value: String) : WheelOperation()
    class Backward(val value: String) : WheelOperation()
    class Left(val value: String) : WheelOperation()
    class Right(val value: String) : WheelOperation()
}


val TXDATAPOINT_BATTERY_CHANG_ID = 10101L
val TXDATAPOINT_SWITCH_MOBITOR_ID = 100006165L
val TXDATAPOINT_SHUTDOWN_ID = 100006166L
val TXDATAPOINT_TURN_MOBITOR_ID = 100006167L
val TXDATAPOINT_HAS_OPERATION_ID = 100006168L




