package com.booyue.ui.binding

import android.widget.Toast
import com.booyue.base.BaseActivity
import com.booyue.base.mvp.contract.BindingContract
import com.booyue.base.mvp.presenter.BindingPresenter
import com.booyue.monitor.R
import com.booyue.utils.LoggerUtils
import com.bumptech.glide.Glide
import com.tencent.util.NetWorkUtils
import kotlinx.android.synthetic.main.activity_guide.*

/**
 * Created by Tianluhua on 2018\10\26 0026.
 */
class BooyueGuideActivity : BaseActivity(), BindingContract.View {


    companion object {
        const val TAG = "BooyueGuideActivity"
    }

    private val mPresenter by lazy {
        BindingPresenter()
    }


    override fun showLoading() {
        LoggerUtils.d("$TAG showLoading")
    }

    override fun dismissLoading() {
        LoggerUtils.d("$TAG dismissLoading")
    }


    override fun setView() {
        setContentView(R.layout.activity_guide)
    }

    override fun initView() {
        tv_back.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        mPresenter.attachView(this)
        mPresenter.generateQRCode(applicationContext)
    }

    /**
     * 显示二维码
     *
     * @param success
     */
    //    private boolean isQRcodeSuccess = false;
    override fun showQRCode(success: Boolean, filePath: String) {
        runOnUiThread {
            if (success) {
                showTips(R.string.generate_qrcode_success)
                Glide.with(this@BooyueGuideActivity).load(filePath).into(iv_zxing)
            } else {
                if (!NetWorkUtils.isNetWorkAvailable(this@BooyueGuideActivity)) {
                    showTips(R.string.network_close)
                } else {
                    showTips(R.string.generate_qrcode_fail)
                }
            }
        }

    }

    private fun showTips(tips: Int) {
        Toast.makeText(this, tips, Toast.LENGTH_SHORT).show()
    }

}