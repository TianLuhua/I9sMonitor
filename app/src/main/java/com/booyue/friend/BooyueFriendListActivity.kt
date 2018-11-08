package com.booyue.friend

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.booyue.LICENSE
import com.booyue.PRODUCT_ID
import com.booyue.SERIAL_NUMBER
import com.booyue.SERVER_PUBLIC_KEY
import com.booyue.base.BaseActivity
import com.booyue.monitor.R
import com.booyue.serial.SerialNumberManager
import com.booyue.widget.CommonDialog
import com.booyue.widget.DialogManager
import com.tencent.device.TXBinderInfo
import com.tencent.device.TXDeviceService
import com.tencent.devicedemo.WifiDecodeActivity
import com.tencent.util.AppUtil
import com.tencent.util.FileUtil
import com.tencent.util.LoggerUtils
import com.tencent.util.UpgradeUtil
import kotlinx.android.synthetic.main.activity_friendlist.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

/**
 * Created by Tianluhua on 2018\10\31 0031.
 */
class BooyueFriendListActivity : BaseActivity() {


    companion object {
        val TAG = "BooyueFriendListActivity"
    }

    private var mNotifyReceiver = NotifyReceiver()
    private var checkUpgrade = false
    private var mBinderAdapter: BooyueFriendListAdapter? = null
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var dialog: AlertDialog


    override fun setView() {
        setContentView(R.layout.activity_friendlist)
    }

    override fun initView() {
        SerialNumberManager.readSerialNumber(this@BooyueFriendListActivity) {
            LoggerUtils.d(TAG + "onSerailNumberListener Code:" + it)
            runOnUiThread {
                startService()
            }
        }

        // 晨芯方案商&串号没有写入文件，
        // 不需要做任何操作，此时启动线程网络获取串号并写入文件，操作执行在接口回调中（line 60）
        /**modify by : 2018/3/5 16:17*/ //T6改用服务端获取数据
        if (!FileUtil.isSNCached() && SerialNumberManager.matchDevice()) {//启动线程通过回调获取
        } else {
            startService()
        }
        ib_erase_all_binders.setOnClickListener {

            DialogManager.createAlertDialog(it.context, 0, 0, null) {
                TXDeviceService.eraseAllBinders()
                /**modify by : 2018/3/1 18:18 如果是晨芯 需要删除串号文件*/
                FileUtil.cleanSNFile()
            }
        }
        tv_back.setOnClickListener { finish() }
        tv_pre.setOnClickListener { pre() }
        tv_next.setOnClickListener { next() }
        initRecyclerView()
        if (!checkUpgrade) {
            checkUpgrade()
        }

    }

    override fun initData() {
        val filter = IntentFilter()
        filter.addAction(TXDeviceService.BinderListChange)//绑定列表改变
        filter.addAction(TXDeviceService.OnEraseAllBinders)//解除所有的绑定者
        filter.addAction(TXDeviceService.OnGetSociallyNumber)//获取设备号
        filter.addAction(TXDeviceService.OnFriendListChange)//朋友列表变化
        filter.addAction(TXDeviceService.OnReceiveAddFriendReq)//接收添加朋友请求
        filter.addAction(TXDeviceService.OnDelFriend)//删除朋友
        filter.addAction(TXDeviceService.OnModifyFriendRemark)//修改朋友标志
        mNotifyReceiver = NotifyReceiver()
        registerReceiver(mNotifyReceiver, filter)

        val bNetworkSetted = this.getSharedPreferences("TXDeviceSDK", 0).getBoolean("NetworkSetted", false)
        if (TXDeviceService.NetworkSettingMode == true && bNetworkSetted == false) {
            LoggerUtils.d(TAG + "start WifiDecodeActivity.class")
            val intent = Intent(this@BooyueFriendListActivity, WifiDecodeActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        updateBinerList()
    }

    private fun updateBinerList() {
        /**
         * 更新绑定列表
         */
        val arrayBinder = TXDeviceService.getBinderList()
        if (arrayBinder != null) {
            val binderList = ArrayList<TXBinderInfo>()
            for (i in arrayBinder.indices) {
                binderList.add(arrayBinder[i])
            }
            if (mBinderAdapter != null) {
                mBinderAdapter!!.freshBinderList(binderList)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mNotifyReceiver)
    }

    /**
     * 检测升级
     */
    private fun checkUpgrade() {
        UpgradeUtil.checkUpgrade(this, object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response?) {
                if (response != null && response.isSuccessful) {
                    val result = response.body().string()
                    LoggerUtils.d(TAG + "checkUpgrade response = " + result)
                    processResult(result)
                }
            }
        })

    }

    private fun processResult(result: String?) {
        if (result == null || result === "") return
        try {
            val jsonObject = JSONObject(result)
            if ("1" == jsonObject.getString("ret")) {
                val contentOject = jsonObject.getJSONObject("content")
                val apk = contentOject.getString("apk")
                val newVersion = contentOject.getString("newVersion")
                val tips = contentOject.getString("video_content")
                if (!TextUtils.isEmpty(apk)) {
                    runOnUiThread {
                        val view = this@BooyueFriendListActivity.layoutInflater.inflate(R.layout.dialog_upgrade, null)
                        dialog = CommonDialog.showAppUpgradeDialog(this@BooyueFriendListActivity, view)
                        initUpgradeView(view, tips, apk)
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    /**
     * 初始化升级对话框
     *
     * @param view   升级对话框
     * @param tips   提示语
     * @param apkUrl 新版本apk地址
     */
    private fun initUpgradeView(view: View?, tips: String, apkUrl: String) {
        if (view == null) throw NullPointerException()
        LoggerUtils.d(TAG + "initUpgradeView()")
        view.findViewById<TextView>(R.id.tv_msg).text = tips
        //销毁提示框
        view.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            if (dialog != null) {
                dialog.dismiss()
            }
            checkUpgrade = false
        }
        //确定 升级下载
        view.findViewById<TextView>(R.id.btn_upgrade).setOnClickListener {
            if (dialog != null) {
                dialog.dismiss()
            }
            checkUpgrade = true
            UpgradeUtil().downLoadApk(this@BooyueFriendListActivity, apkUrl)
        }
    }


    /**
     * 启动QQ物联核心服务服务
     */
    private fun startService() {
        if (PRODUCT_ID == 0L || TextUtils.isEmpty(LICENSE) || TextUtils.isEmpty(SERIAL_NUMBER)
                || TextUtils.isEmpty(SERVER_PUBLIC_KEY)) {
            showToast(R.string.unique_identifier)
        } else {
            val startIntent = Intent(this@BooyueFriendListActivity, TXDeviceService::class.java)
            startService(startIntent)
        }
    }

    /**
     * 上一页
     */
    private fun pre() {
        if (mBinderAdapter != null && mBinderAdapter!!.itemCount > 4) {
            //获取第一个view对应的位置
            val firstPosition = recylerview_list.getChildLayoutPosition(recylerview_list.getChildAt(0))
            //获取最后一个view对应的位置
            val lastPosition = recylerview_list.getChildLayoutPosition(recylerview_list.getChildAt(recylerview_list.childCount - 1))
            if (firstPosition > 0) {
                recylerview_list.smoothScrollToPosition(firstPosition - 1)

            }
        }
    }

    /**
     * 下一页
     */
    private fun next() {

        if (mBinderAdapter != null && mBinderAdapter!!.itemCount > 4) {
            val firstPosition = recylerview_list.getChildLayoutPosition(recylerview_list.getChildAt(0))
            val lastPosition = recylerview_list.getChildLayoutPosition(recylerview_list.getChildAt(recylerview_list.childCount - 1))
            if (lastPosition < mBinderAdapter!!.itemCount - 1) {
                //                recyclerView.smoothScrollToPosition(lastPosition - 3 + 1);
                val left = recylerview_list.getChildAt(1).left
                recylerview_list.smoothScrollBy(left, 0)
            }
        }

    }

    /**
     * 初始化recyclerview
     */
    private fun initRecyclerView() {
        //创建布局管理
        linearLayoutManager = LinearLayoutManager(this, LinearLayout.HORIZONTAL, false)
        //设置布局
        recylerview_list.layoutManager = linearLayoutManager
        //创建适配器
        mBinderAdapter = BooyueFriendListAdapter(this)
        //设置适配器
        recylerview_list.adapter = mBinderAdapter

        val mBinderList = ArrayList<TXBinderInfo>()
        //刷新列表
        mBinderAdapter!!.freshBinderList(mBinderList)

    }

    /**
     * 只有app在前台才弹吐司
     *
     * @param text 弹出吐司的内容
     */
    fun showToast(text: String) {
        LoggerUtils.d(TAG + text)
        if (AppUtil.isAppOnForeground()) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun showToast(text: Int) {
        if (AppUtil.isAppOnForeground()) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }


    inner class NotifyReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
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
                        if (mBinderAdapter != null) {
                            mBinderAdapter!!.freshBinderList(binderList)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                TXDeviceService.OnEraseAllBinders -> {
                    val resultCode = intent.extras!!.getInt(TXDeviceService.OperationResult)
                    if (0 != resultCode) {
                        showToast("解除绑定失败，错误码:$resultCode")
                    } else {
                        showToast("解除绑定成功!!!")
                    }

                }
                TXDeviceService.OnGetSociallyNumber -> {
                    val result = intent.extras!!.getInt(TXDeviceService.OperationResult)
                    if (0 == result) {
                        val sociallyNum = intent.extras!!.getLong("SociallyNumber")
                        if (0L != sociallyNum) {
                            showToast("设备号：$sociallyNum")
                        }
                    }

                }
                TXDeviceService.OnFriendListChange -> {

                }
                TXDeviceService.OnReceiveAddFriendReq -> {

                }
                TXDeviceService.OnDelFriend -> {
                    val resultCode = intent.extras!!.getInt(TXDeviceService.OperationResult)
                    if (0 != resultCode) {
                        showToast("删除好友失败：错误码$resultCode")
                    } else {
                        showToast("删除好友成功")
                    }
                }
                TXDeviceService.OnModifyFriendRemark -> {
                    val resultCode = intent.extras!!.getInt(TXDeviceService.OperationResult)
                    if (0 != resultCode) {
                        showToast("修改好友备注失败：错误码$resultCode")
                    } else {
                        showToast("修改好友备注成功")
                    }
                }
            }
        }
    }
}