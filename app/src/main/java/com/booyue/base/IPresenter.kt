package com.booyue.base

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */
interface IPresenter<in V : IBaseView> {

    /**
     * 加载View
     */
    fun attachView(mRootView: V)

    /**
     * 卸载View
     */
    fun detachView()

    /**
     * 检查View是否加载
     */
    val isViewAttached: Boolean
}