package com.booyue.utils

import java.util.concurrent.Executors

/**
 * Created by Tianluhua on 2018\11\12 0012.
 */


private val IO_EXECUTOR = Executors.newSingleThreadExecutor()


/**
 * 用于运行耗时任务
 */
fun runOnIOthread(runnable: () -> Unit) {
    IO_EXECUTOR.execute(runnable)
}