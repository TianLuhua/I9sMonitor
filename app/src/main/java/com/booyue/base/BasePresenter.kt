package com.booyue.base

import java.lang.RuntimeException

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */
open class BasePresenter<T : IBaseView> : IPresenter<T> {

    var mRootView: T? = null
        private set


    override fun attachView(mRootView: T) {
        this.mRootView = mRootView
    }

    override fun detachView() {
        this.mRootView = null

    }

    override val isViewAttached: Boolean
        get() = mRootView != null

    fun checkViewAttached() {
        if (!isViewAttached) throw MVPViewNotAttachedException()
    }

    private class MVPViewNotAttachedException internal constructor() : RuntimeException("Please call IPresenter.attachView(IBaseView) before" + " requesting data to the IPresenter")
}