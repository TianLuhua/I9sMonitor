package com.booyue.utils

import android.widget.Toast
import com.tencent.util.AppUtil

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */
class ToastUtils {
    companion object {
        fun showToast(msg: String) {
            val context = Utils.getTopActivityWeakRef()
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        fun showToast(msgID: Int) {
            val context = Utils.getTopActivityWeakRef()
            Toast.makeText(context, context?.resources?.getText(msgID), Toast.LENGTH_SHORT).show()
        }

        fun showLongToast(msg: String) {
            val context = Utils.getTopActivityWeakRef()
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }

        fun showLongToast(msgID: Int) {
            val context = Utils.getTopActivityWeakRef()
            Toast.makeText(context, context?.resources?.getText(msgID), Toast.LENGTH_LONG).show()
        }
    }
}