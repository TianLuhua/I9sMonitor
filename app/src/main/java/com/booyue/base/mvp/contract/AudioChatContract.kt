package com.booyue.base.mvp.contract

import com.booyue.base.IBaseView
import com.booyue.base.IPresenter

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */
interface AudioChatContract {

    interface View : IBaseView {

    }

    interface Presenter : IPresenter<View> {

    }
}