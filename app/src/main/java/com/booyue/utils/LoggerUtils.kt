package com.booyue.utils

import android.util.Log

/**
 * Created by Tianluhua on 2018\11\8 0008.
 */
class LoggerUtils {
    companion object {
        private var isOpen = true
        private val TAG = "LoggerUtils"

        fun i(msg: String) {
            if (isOpen) {
                Log.i(TAG, msg)
            }
        }

        fun d(msg: String) {
            if (isOpen) {
                Log.d(TAG, msg)
            }
        }

        fun d(msg: String, tag: String) {
            if (isOpen) {
                Log.d(TAG, "$tag ----->" + msg)
            }
        }

        fun e(msg: String) {
            if (isOpen) {
                Log.e(TAG, msg)
            }
        }

        fun e(msg: String, tag: String) {
            if (isOpen) {
                Log.e(TAG, "$tag ----->" + msg)
            }
        }

        fun setDebug(debug: Boolean) {
            this.isOpen = debug
        }

        fun format_debug(format: String, vararg args: Any) {
            if (isOpen) {
                Log.e(TAG, String.format(format, args))
            }
        }
    }
}