package com.booyue.binding

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.widget.Toast
import com.booyue.PRODUCT_ID
import com.booyue.SERIAL_NUMBER
import com.booyue.base.BaseActivity
import com.booyue.monitor.R
import com.booyue.utils.LoggerUtils
import com.bumptech.glide.Glide
import com.tencent.util.NetWorkUtils
import com.tencent.util.QRCodeUtils
import kotlinx.android.synthetic.main.activity_guide.*
import java.io.File

/**
 * Created by Tianluhua on 2018\10\26 0026.
 */
class BooyueGuideActivity : BaseActivity() {

    companion object {
        const val TAG = "BooyueGuideActivity"
        val QRCODE_URL = "http://iot.qq.com/add?pid=" + "$PRODUCT_ID" + "&sn=" + "$SERIAL_NUMBER"
    }

    private var filePath: String? = null

    override fun setView() {
        setContentView(R.layout.activity_guide)
    }

    override fun initView() {
        tv_back.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        generateQRCode()
    }


    private fun generateQRCode() {
        filePath = getFileRoot(this@BooyueGuideActivity) + File.separator + "qr_" + System.currentTimeMillis() + ".jpg"
        LoggerUtils.d(QRCODE_URL)
        //二维码图片较大时，生成图片、保存文件的时间可能较长，因此放在新线程中
        Thread {
            val success = QRCodeUtils.createQRImage(QRCODE_URL, 160, 160,
                    BitmapFactory.decodeResource(resources, R.drawable.logo), filePath)
            LoggerUtils.d(TAG + "success = " + success)
            showQRCode(success)
        }.start()
    }


    /**
     * 显示二维码
     *
     * @param success
     */
    //    private boolean isQRcodeSuccess = false;
    private fun showQRCode(success: Boolean) {
        runOnUiThread {
            if (success) {
                //                        isQRcodeSuccess = true;
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


    private fun showTips(tips: Int) {
        Toast.makeText(this, tips, Toast.LENGTH_SHORT).show()
    }

}