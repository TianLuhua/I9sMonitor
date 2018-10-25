package com.tencent.annotation;

/**
 * Created by Administrator on 2018/4/26.09:29
 */

import android.support.annotation.StringDef;

import com.tencent.SerialNumberManager;

@StringDef({SerialNumberManager.MAC,SerialNumberManager.SN})
public @interface Unique {
}
