// Created by 超悟空 on 2021/4/7.

package org.cwk.work

import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 实际的网络请求构建函数
 * 用于配置和生成[Call]，后续由框架负责执行请求
 * 总是在[kotlinx.coroutines.Dispatchers.IO]中执行
 *
 * @param tag 为跟踪日志标签
 * @param options 为请求所需的全部参数
 *
 * @return 构建好的[Call]
 *
 * @throws Exception 构建过程出现错误请抛出异常，异常将由框架处理，并且会回调[Work.onParamsError]生命周期
 */
typealias WorkRequest = (tag: String, options: Options) -> Call

/**
 * 默认实现的网络请求构建函数
 */
internal fun workRequestImp(tag: String, options: Options): Call {
    val config = WorkConfig.configs[options.configKey] ?: WorkConfig.defaultConfig
    return workOkHttpClientBuilder(tag, config, options).newCall(Request.Builder().apply {
        options.headers?.let { headers(it.toHeaders()) }
        url(workHttpUrlBuilder(config, options))
        method(options.method.name, workRequestBodyBuilder(config, options))
    }.build())
}

/**
 * 默认实现的生成经过参数修改后的[OkHttpClient]方法
 *
 * 此处会进行超时时间覆盖以及重试和进度监听拦截器的配置。
 *
 * @param tag 为跟踪日志标签
 * @param config 使用的全局任务配置
 * @param options 本次请求的全部参数
 *
 * @return 新的[OkHttpClient]
 */
fun workOkHttpClientBuilder(tag: String, config: WorkConfig, options: Options) =
    config.client.newBuilder().apply {
        options.connectTime?.takeIf { it >= 0 }?.let {
            connectTimeout(it, TimeUnit.MILLISECONDS)
        }
        options.readTimeout?.takeIf { it >= 0 }?.let {
            readTimeout(it, TimeUnit.MILLISECONDS)
        }
        options.writeTimeout?.takeIf { it >= 0 }?.let {
            writeTimeout(it, TimeUnit.MILLISECONDS)
        }
        if (options.retry > 0) {
            addInterceptor(WorkRetryInterceptor(tag, options.retry))
        }
        options.onSendProgress?.let {
            addNetworkInterceptor(SendProgressInterceptor(it))
        }
        options.onReceiveProgress?.let {
            addNetworkInterceptor(ReceiveProgressInterceptor(it))
        }
    }.build()

/**
 * 默认实现的网络请求地址构建器
 *
 * 当Http请求方法为[HttpMethod.GET]或[HttpMethod.HEAD]时，请求参数会在此处拼接。
 *
 * 此时[Options.params]仅支持[String]和[Map]类型参数，
 * 如果[Options.params]为[String]类型，则参数应当是已经用'&'拼接好的未使用"urlencoded"编码的字符串。
 *
 * 如果需要传递已经使用"urlencoded"编码的参数串，请在[Options.url]中自行拼接，
 * 同时请把[Options.params]留空，否则[Options.url]中的参数可能会被覆盖。
 *
 * @param config 使用的全局任务配置
 * @param options 本次请求的全部参数
 *
 * @return 构建好的[HttpUrl]实例
 */
fun workHttpUrlBuilder(config: WorkConfig, options: Options) =
    (config.baseUrl.toHttpUrlOrNull()?.resolve(options.url)
        ?: options.url.toHttpUrl()).let { httpUrl ->
        when (options.method) {
            HttpMethod.GET, HttpMethod.HEAD -> httpUrl.newBuilder().apply {
                when (val params = options.params) {
                    is String -> query(params)
                    is Map<*, *> -> params.forEach {
                        addQueryParameter("${it.key}", it.value?.toString())
                    }
                }
            }.build()
            else -> httpUrl
        }
    }

/**
 * 默认实现的[RequestBody]构建器
 *
 * @param config 使用的全局任务配置
 * @param options 本次请求的全部参数
 *
 * @return [RequestBody]，如果不需要或没有返回null
 */
fun workRequestBodyBuilder(config: WorkConfig, options: Options): RequestBody? {
    if (options.method == HttpMethod.GET || options.method == HttpMethod.HEAD) {
        return null
    }
    val contentType = options.contentType ?: config.defaultPostContentType
    return when (val params = options.params) {
        is String -> params.toRequestBody(contentType)
        is ByteString -> params.toRequestBody(contentType)
        is ByteArray -> params.toRequestBody(contentType)
        is File -> params.asRequestBody(contentType)
        is FormBody -> params
        is MultipartBody -> params
        is Map<*, *> -> params.toRequestBody(contentType, options.listFormat ?: config.listFormat)
        else -> null
    }
}