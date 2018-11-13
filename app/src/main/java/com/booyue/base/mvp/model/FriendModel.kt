package com.booyue.base.mvp.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.booyue.base.IBaseModel
import com.booyue.utils.ToastUtils
import com.booyue.utils.Utils
import com.tencent.device.TXBinderInfo
import com.tencent.device.TXDeviceService
import java.util.*

/**
 * Created by Tianluhua on 2018\11\13 0013.
 */
class FriendModel : IBaseModel {

    companion object {
        val TAG = "FriendModel"
    }

    private val mNotifyReceiver = NotifyReceiver()

    private val mDataChangeCallBack: DataChangeCallBack

    constructor(mDataChangeCallBack: DataChangeCallBack) {
        this.mDataChangeCallBack = mDataChangeCallBack
    }

    /**
     * 监听绑定列表
     */
    fun initChangData() {
        val filter = IntentFilter()
        filter.apply {
            addAction(TXDeviceService.BinderListChange)//绑定列表改变
            addAction(TXDeviceService.OnEraseAllBinders)//解除所有的绑定者
            addAction(TXDeviceService.OnGetSociallyNumber)//获取设备号
            addAction(TXDeviceService.OnFriendListChange)//朋友列表变化
            addAction(TXDeviceService.OnReceiveAddFriendReq)//接收添加朋友请求
            addAction(TXDeviceService.OnDelFriend)//删除朋友
            addAction(TXDeviceService.OnModifyFriendRemark)//修改朋友标志
        }
        Utils.getTopActivityWeakRef()?.registerReceiver(mNotifyReceiver, filter)
    }

    fun initData() {
        /**
         * 初始化绑定列表
         */
        val arrayBinder = TXDeviceService.getBinderList()
        if (arrayBinder != null) {
            val binderList = ArrayList<TXBinderInfo>()
            for (i in arrayBinder.indices) {
                binderList.add(arrayBinder[i])
            }
            mDataChangeCallBack.dataChange(binderList)

        }
    }

    override fun onDestroy() {
        Utils.getTopActivityWeakRef()?.unregisterReceiver(mNotifyReceiver)
    }

    /**
     * 监听绑定列表变化
     */
    inner class NotifyReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                ToastUtils.showToast("$TAG intent == null")
                return
            }
            val action = intent.action
            when (action) {
                TXDeviceService.BinderListChange -> {
                    try {
                        val listTemp = intent.extras!!.getParcelableArray("binderlist")
                        val binderList = ArrayList<TXBinderInfo>()
                        listTemp.forEach {
                            it as TXBinderInfo
                            binderList.add(it)
                        }
                        mDataChangeCallBack.dataChange(binderList)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                TXDeviceService.OnEraseAllBinders -> {
                    val resultCode = intent.extras!!.getInt(TXDeviceService.OperationResult)
                    if (0 != resultCode) {
                        ToastUtils.showToast("$TAG 解除绑定失败，错误码:$resultCode")
                    } else {
                        ToastUtils.showToast("$TAG 解除绑定成功!!!")
                    }
                }
                TXDeviceService.OnGetSociallyNumber -> {
                    val result = intent.extras!!.getInt(TXDeviceService.OperationResult)
                    if (0 == result) {
                        val sociallyNum = intent.extras!!.getLong("SociallyNumber")
                        if (0L != sociallyNum) {
                            ToastUtils.showToast("$TAG 设备号：$sociallyNum")
                        }
                    }
                }
                TXDeviceService.OnFriendListChange -> {
                    ToastUtils.showToast("$TAG OnFriendListChange")
                }
                TXDeviceService.OnReceiveAddFriendReq -> {
                    ToastUtils.showToast("$TAG OnReceiveAddFriendReq")
                }
                TXDeviceService.OnDelFriend -> {
                    val resultCode = intent.extras!!.getInt(TXDeviceService.OperationResult)
                    if (0 != resultCode) {
                        ToastUtils.showToast("$TAG 删除好友失败：错误码$resultCode")
                    } else {
                        ToastUtils.showToast("$TAG 删除好友成功")
                    }
                }
                TXDeviceService.OnModifyFriendRemark -> {
                    val resultCode = intent.extras!!.getInt(TXDeviceService.OperationResult)
                    if (0 != resultCode) {
                        ToastUtils.showToast("$TAG 修改好友备注失败：错误码$resultCode")
                    } else {
                        ToastUtils.showToast("$TAG 修改好友备注成功")
                    }
                }
            }
        }
    }

    /**
     * 跟新数据回调接口
     */
    interface DataChangeCallBack {
        fun dataChange(binderList: List<TXBinderInfo>)
    }
}