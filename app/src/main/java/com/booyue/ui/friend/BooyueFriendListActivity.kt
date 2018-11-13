package com.booyue.ui.friend

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
import com.booyue.base.mvp.contract.FriendContract
import com.booyue.base.mvp.presenter.FriendPresenter
import com.booyue.monitor.R
import com.booyue.serial.SerialNumberManager
import com.booyue.ui.friend.adapter.BooyueFriendListAdapter
import com.booyue.utils.LoggerUtils
import com.booyue.utils.ToastUtils
import com.booyue.widget.CommonDialog
import com.booyue.widget.DialogManager
import com.tencent.device.TXBinderInfo
import com.tencent.device.TXDeviceService
import com.tencent.devicedemo.WifiDecodeActivity
import com.tencent.util.AppUtil
import com.tencent.util.FileUtil
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
class BooyueFriendListActivity : BaseActivity(), FriendContract.View {


    companion object {
        val TAG = "BooyueFriendListActivity"
    }

    private var checkUpgrade = false
    private var mBinderAdapter: BooyueFriendListAdapter? = null
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var dialog: AlertDialog
    private val mFriendPresenter by lazy {
        FriendPresenter()
    }


    override fun freshBinderList(binderList: List<TXBinderInfo>) {
        LoggerUtils.d("$TAG initUpgradeView()")
        mBinderAdapter?.freshBinderList(binderList)
    }

    override fun showLoading() {
        LoggerUtils.d("$TAG showLoading()")
    }

    override fun dismissLoading() {
        LoggerUtils.d("$TAG dismissLoading()")
    }


    override fun setView() {
        setContentView(R.layout.activity_friendlist)
    }

    override fun initView() {
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
        mFriendPresenter.attachView(this)
        mFriendPresenter.initChangData()
        val bNetworkSetted = this.getSharedPreferences("TXDeviceSDK", 0).getBoolean("NetworkSetted", false)
        if (TXDeviceService.NetworkSettingMode && !bNetworkSetted) {
            LoggerUtils.d(TAG + "start WifiDecodeActivity.class")
            val intent = Intent(this@BooyueFriendListActivity, WifiDecodeActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        mFriendPresenter.initData()
    }


    override fun onDestroy() {
        super.onDestroy()
        mFriendPresenter.detachView()
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
}