package com.booyue.base.mvp.presenter

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import com.booyue.PRODUCT_ID
import com.booyue.SERIAL_NUMBER
import com.booyue.base.BasePresenter
import com.booyue.base.mvp.contract.BindingContract
import com.booyue.monitor.R
import com.booyue.ui.binding.BooyueGuideActivity
import com.booyue.utils.LoggerUtils
import com.booyue.utils.runOnIOthread
import com.tencent.util.QRCodeUtils
import java.io.File

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */
class BindingPresenter : BasePresenter<BindingContract.View>(), BindingContract.Presenter {


    companion object {
        val QRCODE_URL = "http://iot.qq.com/add?pid=" + "$PRODUCT_ID" + "&sn=" + "$SERIAL_NUMBER"
    }

    override fun generateQRCode(mContext: Context) {
        checkViewAttached()
        mRootView?.showLoading()
        val filePath = getFileRoot(mContext) + File.separator + "qr_" + System.currentTimeMillis() + ".jpg"
        LoggerUtils.d(QRCODE_URL)

        //二维码图片较大时，生成图片、保存文件的时间可能较长，因此放在新线程中
        runOnIOthread {
            val success = QRCodeUtils.createQRImage(QRCODE_URL, 160, 160,
                    BitmapFactory.decodeResource(mContext.resources, R.drawable.logo), filePath)
            LoggerUtils.d(BooyueGuideActivity.TAG + "success = " + success)
            mRootView?.showQRCode(success, filePath!!)
        }
        mRootView?.dismissLoading()
    }

    /**
     * 二维码缓存目录
     */
    private fun getFileRoot(context: Context): String {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val external = context.getExternalFilesDir(null)
            if (external != null) {
                return external.absolutePath
            }
        }
        return context.filesDir.absolutePath
    }
}