package com.booyue.annotation

import android.support.annotation.StringDef
import com.booyue.serial.SerialNumberManager

/**
 * Created by Tianluhua on 2018\10\26 0026.
 */

@StringDef(SerialNumberManager.MAC, SerialNumberManager.SN)
annotation class Unique {
}