package com.booyue.base.mvp.contract

import android.content.Context
import com.booyue.base.IBaseView
import com.booyue.base.IPresenter

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */
interface BindingContract {

    interface View : IBaseView {
        /**
         * 显示二维码
         */
        fun showQRCode(success: Boolean, filePAth: String)
    }

    interface Presenter : IPresenter<View> {
        /**
         * 生成二维码
         */
        fun generateQRCode(mContext: Context): Unit

    }
}