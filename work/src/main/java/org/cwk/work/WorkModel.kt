// Created by 超悟空 on 2021/4/6.

package org.cwk.work

import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MultipartBody
import java.io.File

/**
 * 进度监听器
 *
 * 可用于监听上传或下载进度
 * 默认回调执行在[kotlinx.coroutines.Dispatchers.IO]
 *
 * @suppress 本回调禁止抛出异常，否则会中断网络请求，如有危险操作请自行捕获异常
 *
 * @param current 当前上传/下载字节数
 * @param total 总字节数
 * @param done 是否完成
 */
typealias OnProgress = (current: Long, total: Long, done: Boolean) -> Unit

/**
 * http请求类型
 */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD,
    PATCH,
}

/**
 * 请求参数子序列的自动化装配时的格式化类型
 *
 * 仅当Content-Type为"application/x-www-form-urlencoded"或"multipart/form-data"，且[Work.fillParams]返回[Map]类型时有效，
 * 在框架将[Map]转换为[FormBody]或[MultipartBody]时遇到value为[List]时的序列化格式，
 * 为了不发生顺序错乱，格式错误等意外问题，子序列仅支持[List]类型数据，请用户自行将集合转换为[List]。
 *
 * 如需对子序列指定单独的[ListFormat]，请在[Map]中将值设置为[ListParams]类型。
 *
 * 框架默认值[ListFormat.MULTI]
 */
enum class ListFormat {
    /**
     * 逗号分隔
     *
     * e.g. (foo,bar,baz)
     */
    CSV,

    /**
     * 空格分隔
     *
     * e.g. (foo bar baz)
     */
    SSV,

    /**
     * 制表符分隔
     *
     * e.g. (foo\tbar\tbaz)
     */
    TSV,

    /**
     * 管道分隔
     *
     * e.g. (foo|bar|baz)
     */
    PIPES,

    /**
     * 多参数实例
     *
     * e.g. (foo=value&foo=another_value)
     */
    MULTI,

    /**
     * 前向兼容
     *
     * e.g. (foo[]=value&foo[]=another_value)
     */
    MULTI_COMPATIBLE;

    /**
     * 获取格式对应的分隔符
     */
    fun separator() = when (this) {
        CSV -> ","
        SSV -> " "
        TSV -> """\t"""
        PIPES -> "|"
        else -> ""
    }
}

/**
 * 单次请求的配置信息
 *
 * @property url 相对地址（需设置[WorkConfig.baseUrl]），或完整的请求地址（包含http(s)://）
 * @property params 最终用于发送的请求参数，由[Work.fillParams]提供
 * @property method Http请求方法
 * @property retry 请求重试次数，默认0表示不重试，实际执行1次请求，如果设置为1则至少执行一次请求，最多执行两次请求，以此类推
 * @property configKey 用于指定[WorkConfig]的key，关联性请查看[WorkConfig.configs]
 * @property connectTime 连接超时，单位毫秒，默认null表示使用全局配置，0表示不会超时
 * @property readTimeout 读取超时，单位毫秒，默认null表示使用全局配置，0表示不会超时
 * @property writeTimeout 写入超时，单位毫秒，默认null表示使用全局配置，0表示不会超时
 * @property headers 自定义/追加的Http请求头，如果不为null则会覆盖默认的OkHttp框架设置的header
 * @property contentType 请求的Content-Type，为null使用默认值[WorkConfig.defaultContentType]，如果要上传文件请将此类型设为"multipart/form-data"
 * @property onSendProgress 发送/上传进度监听器，在[method]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效
 * @property onReceiveProgress 接收/下载进度监听器
 * @property listFormat 自动序列化时的数组装配格式，默认使用[WorkConfig.listFormat]
 */
data class Options(
    var url: String,
    var params: Any?,
    var method: HttpMethod = HttpMethod.GET,
    var retry: Int = 0,
    var configKey: String? = null,
    var connectTime: Long? = null,
    var readTimeout: Long? = null,
    var writeTimeout: Long? = null,
    var headers: Map<String, String>? = null,
    var contentType: MediaType? = null,
    var onSendProgress: OnProgress? = null,
    var onReceiveProgress: OnProgress? = null,
    var listFormat: ListFormat? = null,
) {
    override fun toString(): String = """
    method: $method
    url: $url
    headers: $headers
    params: $params"""
}

/**
 * Http响应数据
 *
 * @property success http成功失败标志
 * @property statusCode 响应状态码
 * @property headers 响应头信息
 */
data class HttpResponse(
    val success: Boolean,
    val statusCode: Int,
    val headers: Headers,
) {
    override fun toString(): String = """
    success: $success; code: $statusCode;
    headers: $headers;"""
}

/**
 * [Work]的异常类型
 */
enum class WorkErrorType {

    /**
     * 任务传入参数错误
     */
    PARAMS,

    /**
     * 网络错误，比如网络不可用，或者网络中断等
     */
    NETWORK,

    /**
     * 请求超时，包含连接超时，写入超时，读取超时
     */
    TIMEOUT,

    /**
     * 服务器返回错误，4xx,5xx
     */
    RESPONSE,

    /**
     * 业务任务执行错误（应用业务逻辑失败）
     */
    TASK,

    /**
     * 响应数据解析错误
     */
    PARSE,

    /**
     * 任务被取消
     */
    CANCEL,

    /**
     * 一些其他异常，可能是网络库或其他数据处理异常
     */
    OTHER
}

/**
 * [Work]执行返回的数据包装类，包含单次任务执行周期的全部请求和响应数据
 *
 * @param T 类型的业务数据实例
 */
open class WorkData<T> {

    /**
     * 本次服务成功失败标志
     */
    var success = false
        internal set

    /**
     * 服务响应消息
     */
    var message: String? = null
        internal set

    /**
     * 任务结果数据
     */
    var result: T? = null
        internal set

    /**
     * 用于网络请求使用的参数
     */
    var options: Options? = null
        internal set

    /**
     * http响应数据
     *
     * 在[Work.onResponseConvert]生命周期阶段开始出现
     */
    var response: HttpResponse? = null
        internal set

    /**
     * 执行失败的原因类型，为空表示没有异常
     */
    var errorType: WorkErrorType? = null
        internal set

    /**
     * 在一次请求生命周期中可携带的数据
     */
    var extra: Any? = null

    operator fun component1() = success

    operator fun component2() = result

    operator fun component3() = message
}

/**
 * 拥有独立序列化格式的子序列包装类
 *
 * 在框架自动序列化数据时覆盖默认[ListFormat]格式，
 * 指明使用[format]拼接格式，此配置仅影响[values]的序列化。
 *
 * @property format 此子序列的序列化格式
 * @property values 实际的子序列数据
 *
 * @see ListFormat
 */
data class ListParams(val format: ListFormat, val values: List<*>)

/**
 * 带有指定的mimeType的文件包装类
 *
 * 通常在上传文件时[File]本身的文件名无意义或错误需要手动指明文件的类型和名称时使用。
 * 仅当Content-Type为"multipart/form-data"，且[Work.fillParams]返回[Map]类型时由框架负责装配。
 * 如果用户需要自己装配文件提交表单，请在[Work.fillParams]直接返回自己构建的[MultipartBody]
 *
 * @property file 需要上传的文件
 * @property mimeType 指定的文件类型
 * @property name 指定的文件名，如果null则框架会使用[file]的名称
 */
data class FileWithMimeType(val file: File, val mimeType: String, val name: String? = null)

/**
 * 带有指定的mimeType的内存数据包装类
 *
 * 在上传已经加载到内存的文件数据时使用，
 * 仅当Content-Type为"multipart/form-data"，且[Work.fillParams]返回[Map]类型时由框架负责装配。
 * 如果用户需要自己装配文件提交表单，请在[Work.fillParams]直接返回自己构建的[MultipartBody]
 *
 * @property byteArray 需要上传的数据
 * @property mimeType 指定的数据的文件类型
 * @property name 指定数据的文件名
 */
data class ByteArrayWithMimeType(
    val byteArray: ByteArray,
    val mimeType: String,
    val name: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayWithMimeType

        if (!byteArray.contentEquals(other.byteArray)) return false
        if (mimeType != other.mimeType) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = byteArray.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }
}