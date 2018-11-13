package com.booyue.base.mvp.presenter

import com.booyue.base.BasePresenter
import com.booyue.base.mvp.contract.FriendContract
import com.booyue.base.mvp.model.FriendModel
import com.tencent.device.TXBinderInfo

/**
 * Created by Tianluhua on 2018\11\13 0013.
 */
class FriendPresenter : BasePresenter<FriendContract.View>(), FriendContract.Presenter, FriendModel.DataChangeCallBack {

    private val mFriendModel by lazy {
        FriendModel(this)
    }

    override fun initData() {
        mFriendModel.initData()
    }

    override fun initChangData() {
        mFriendModel.initChangData()
    }


    override fun dataChange(binderList: List<TXBinderInfo>) {
        mRootView?.freshBinderList(binderList)
    }

    override fun detachView() {
        super.detachView()
        mFriendModel.onDestroy()
    }

}