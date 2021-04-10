// Created by 超悟空 on 2021/4/6.

package org.cwk.work

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * Work库全局网络客户端配置
 *
 * @property client 默认的[OkHttpClient]客户端
 * @property defaultContentType 默认的请求体的Content-Type类型
 * @property baseUrl 根路径
 *                   如果为空则每个请求必须指定完整的请求地址，
 *                   如果不为空则每个请求可以简单指定相对路径或完整地址（优先使用完整地址）
 * @property listFormat 默认的自动序列化请求参数时的数组装配格式
 * @property workRequest 默认的全局实际网络请求执行函数
 **/
data class WorkConfig(
    val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    val defaultContentType: MediaType = MediaType.FORM_DATA,
    val baseUrl: String = "",
    val listFormat: ListFormat = ListFormat.MULTI,
    val workRequest: WorkRequest = ::workRequestImp,
) {
    companion object {
        /**
         * 是否开启debug模式，开启后会输出日志
         */
        var debugWork by Delegates.observable(true) { _, _, new ->
            printLog = if (new) {
                workLog
            } else {
                emptyLog
            }
        }

        /**
         * 全局默认的配置
         */
        var defaultConfig = WorkConfig()

        /**
         * 存放自定义的[WorkConfig]
         *
         * 如果项目中有多种配置的基础请求参数，可以在这里缓存自定义的[WorkConfig]
         * 比如项目中有多个域名的根host，可以在这里配置每个域名的[WorkConfig]
         * 使用时需要在[Work.configKey]中指定对应的key
         *
         * 当[Work]未指定key或key没有对应的[WorkConfig]时将使用[defaultConfig]
         */
        val configs = mutableMapOf<String, WorkConfig>()
    }
}

private val mJson = "application/json; charset=utf-8".toMediaType()

private val mFormData = "application/x-www-form-urlencoded".toMediaType()

private val mMultipart = "multipart/form-data".toMediaType()

/**
 * "application/json" Content-type
 */
val MediaType.Companion.JSON get() = mJson

/**
 * "application/x-www-form-urlencoded" Content-type
 */
val MediaType.Companion.FORM_DATA get() = mFormData

/**
 * "multipart/form-data" Content-type
 *
 * 通常用于上传文件
 */
val MediaType.Companion.MULTIPART get() = mMultipart