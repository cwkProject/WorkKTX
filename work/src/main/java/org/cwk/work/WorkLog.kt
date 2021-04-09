// Created by 超悟空 on 2021/4/7.

package org.cwk.work

import android.util.Log

/**
 * Work库日志输出方法模板
 *
 * @param priority 日志级别，参考[android.util.Log]
 * @param tag 日志标签，通常是Work实例名
 * @param message 打印的消息
 * @param data 额外携带的数据
 */
typealias WorkLog = (priority: Int, tag: String, message: String?, data: Any?) -> Unit

/**
 * 实际执行日志打印的方法，可以由客户端覆盖
 */
var workLog: WorkLog = { priority, tag, message, data ->
    "${message ?: ""} ${data ?: ""}".chunked(800).forEach {
        Log.println(priority, tag, it)
    }
}

/**
 * 用于关联实际执行的日志函数
 */
internal var printLog: WorkLog = workLog

/**
 * 一个空日志实现
 */
internal val emptyLog: WorkLog = { _, _, _, _ -> }

/**
 * 内部日志打印调用点
 */
private fun log(priority: Int, tag: String, message: String?, data: Any? = null) =
    printLog(priority, tag, message, data)

/**
 * VERBOSE级别日志
 */
internal fun logV(tag: String, message: String?, data: Any? = null) = log(2, tag, message, data)

/**
 * DEBUG级别日志
 */
internal fun logD(tag: String, message: String?, data: Any? = null) = log(3, tag, message, data)

/**
 * INFO级别日志
 */
internal fun logI(tag: String, message: String?, data: Any? = null) = log(4, tag, message, data)

/**
 * WARN级别日志
 */
internal fun logW(tag: String, message: String?, data: Any? = null) = log(5, tag, message, data)