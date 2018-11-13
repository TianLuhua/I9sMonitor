package com.booyue.base.mvp.contract

import com.booyue.base.IBaseView
import com.booyue.base.IPresenter
import com.tencent.device.TXBinderInfo

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */
interface FriendContract {

    interface View : IBaseView {
        /**
         * 数据变化刷新界面UI
         */
        fun freshBinderList(binderList: List<TXBinderInfo>)

    }

    interface Presenter : IPresenter<View> {
        /**
         * 初始数据接口
         */
        fun initData()

        /**
         * 数据变化接口
         */
        fun initChangData()

    }
}