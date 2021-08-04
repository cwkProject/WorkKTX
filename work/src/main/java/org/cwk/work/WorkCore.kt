// Created by 超悟空 on 2021/4/7.

package org.cwk.work

import kotlinx.coroutines.Dispatchers
import okhttp3.*
import okio.ByteString
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * [Work]核心生命周期
 *
 * 所有的生命周期方法默认在[Dispatchers.IO]中执行，可通过[execute]参数改变协程上下文
 *
 * @param D 关联的[Work]实际返回的[WorkData.result]结果数据类型
 * @param T [Work]返回的结果包装类型[WorkData]
 * @param H 对[okhttp3.OkHttpClient]响应数据的中间转换类型，通常由用户根据项目实际情况自行定制实现转换，参见[onResponseConvert]
 */
abstract class WorkCore<D, T : WorkData<D>, H> {

    /**
     * 执行任务
     * 任务的执行完全依赖协程，同样支持协程取消规范
     *
     * @param context 协程上下文，影响[Work]各生命周期方法的执行线程
     * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
     * @param onSendProgress 发送/上传进度监听器，在[httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效
     * @param onReceiveProgress 接收/下载进度监听器
     *
     * @return 包含执行结果的包装类[T]
     */
    abstract suspend fun execute(
        context: CoroutineContext = Dispatchers.IO,
        retry: Int = 0,
        onSendProgress: OnProgress? = null,
        onReceiveProgress: OnProgress? = null,
    ): T

    /**
     * 创建数据模型对象的实例
     *
     * @return [WorkData]类或子类实例，通常在用户项目中的通用[Work]基类中实现
     */
    protected abstract fun onCreateWorkData(): T

    /**
     * 参数合法性检测
     *
     * 用于检测任务启动所需的参数是否合法，需要子类重写检测规则。
     * 检测成功任务才会被正常执行，如果检测失败则[onParamsError]会被调用，
     * 且后续网络请求任务不再执行，任务仍然可以正常返回并执行生命周期[onFailed]，[onFinished]。
     *
     * @return 参数合法返回true，非法返回false。
     */
    protected open suspend fun onCheckParams() = true

    /**
     * 任务参数错误时调用
     * 通常在[onCheckParams]返回false时调用
     * 也可能在装配网络请求时失败调用，即[workRequest]返回函数执行出错时
     *
     * @return 错误消息内容，将会设置给[WorkData.message]
     */
    protected open fun onParamsError(): String? = null


    /**
     * 任务启动前调用
     *
     * 此处可以用于做操作记录、数据统计，特殊变量[WorkData.extra]创建等，
     * 也可以从缓存加载任务结果拦截后续网络请求实现可缓存任务。
     *
     * @param data 本次请求中流转的数据
     *
     * @return 如果返回有效数据实例，则框架会认为这是从本地缓存加载的数据结果，并且会跳过后续的网络请求，直接进入[onFinished]
     */
    protected open suspend fun onStarted(data: T): D? = null

    /**
     * 设置从缓存加载数据成功后的消息
     *
     * 当[onStarted]返回有效数据实例时，框架将会认为本次任务完成了缓存加载，此时触发本方法，
     * 可以给[WorkData.message]设置一个合适的内容。
     *
     * @param data 本次请求中流转的数据
     *
     * @return 缓存加载的消息，将会设置给[WorkData.message]
     */
    protected open fun onFromCacheMessage(data: T): String? = null

    /**
     * 网络请求地址
     *
     * @return 可以是完整地址，也可以是相对地址（需要设置baseUrl，查看[WorkConfig.baseUrl]）
     */
    protected abstract fun url(): String

    /**
     * http请求方法
     */
    protected open fun httpMethod() = HttpMethod.GET

    /**
     * 填充请求所需的参数
     *
     * 支持多种类型的参数装配，
     *
     * 当[httpMethod]指定的请求方法不支持请求体时（即[HttpMethod.GET]或[HttpMethod.HEAD]），此处仅支持[String]或[Map]类型，
     * 如果是[String]类型，则应当是已经使用'&'连字符拼接好的未使用"urlencoded"编码的字符串，
     * 如果需要传递已经使用"urlencoded"编码的参数串，请在[Options.url]中自行拼接，
     * 同时请把[Options.params]留空，否则[Options.url]中的参数可能会被覆盖。
     * 如果是[Map]类型，则框架会自动拼接，[Map]的`key`和`value`均支持任意类型，
     * 最终`key`会使用`"$key"`转换，`value`使用`value?.toString()`转换，请确保值是String类型或[toString]方法有意义。
     * 具体拼接方法请查看[workHttpUrlBuilder]
     *
     * 当[httpMethod]指定的请求方法支持请求体时（如[HttpMethod.POST]），此处支持[String]、[ByteString]、[ByteArray]、[File]、[RequestBody]、[Map]。
     * 其中[RequestBody]是用户自定义的OKHttp原始数据体，通常使用[FormBody]、[MultipartBody]构建，也可以自定义，此时[contentType]可忽略。
     * 如果是[String]、[ByteString]、[ByteArray]、[File]等类型，则[contentType]或[WorkConfig.defaultContentType]必须与返回的数据内容格式一致，
     * 例如：
     *
     * ```
     *
     * // 假设全局默认配置被设置为
     * WorkConfig.defaultConfig = WorkConfig(defaultContentType = "application/json".toMediaType())
     *
     * ...
     *
     * override suspend fun fillParams() = """{
     *  "key1": params1,
     *  "key2": params2
     * }"""
     *
     * ```
     *
     * 或者配合json序列化库：
     *
     * ```
     *
     * // by kotlinx.serialization
     * @Serializable
     * data class Params(val a:String,val b:String)
     *
     * class SampleWork(val params:Params) : BaseWork<Unit>(){
     *
     *      override suspend fun fillParams() = Json.encodeToString(params)
     *
     *      override fun contentType() = "application/json".toMediaType()
     *
     *      ...
     * }
     *
     * ```
     *
     * 如果是[ByteArray]或[File]等二进制内容：
     *
     *
     * ```
     *
     * override suspend fun fillParams() = File("README.md")
     *
     * override fun contentType() = "text/x-markdown; charset=utf-8".toMediaType()
     *
     * ```
     *
     * 如果是[Map]类型，则数据体会由框架负责装配，此时[contentType]仅支持"application/json"，"application/x-www-form-urlencoded"，"multipart/form-data"三种。
     * [Map]的泛型不受限，其非常类似[workHttpUrlBuilder]的模式。
     * 其中"multipart/form-data"比较特殊，此格式是支持通用文件上传的格式，通常我们使用http上传文件都会构建此种格式的数据。
     * 此时[Map]中如果要包含文件数据，可以设置value为[File]、[FileWithMimeType]或[ByteArrayWithMimeType]，框架会负责转换。
     *
     * 例如：
     *
     * ```
     *
     * override suspend fun fillParams() = mapOf(
     *      "title" : "蓝瘦香菇",
     *      "image1" : File("LanShou.jpg"),
     *      "image2" : FileWithMimeType(File("XiangGu.tmp"),"image/png","XiangGu.png"),
     *      "image3" : ByteArrayWithMimeType(cacheBuffer,"image/gif","Animation.gif"),
     * )
     *
     * override fun contentType() = "multipart/form-data".toMediaType()
     *
     * ```
     *
     * 具体拼装实现请查看[workRequestBodyBuilder]
     *
     * @return 填充的参数，没有返回null或空对象
     */
    protected abstract suspend fun fillParams(): Any?

    /**
     * 请求的Content-Type
     *
     * 如果请求[httpMethod]为支持请求体的类型，则此处可以指定本次请求体的Content-Type。
     * 默认使用[WorkConfig.defaultContentType]。
     *
     * 此处的返回值必须与[fillParams]中填充的参数格式对应，否则可能出错。
     *
     * @return 请求体的Content-Type标识，返回null表示使用[WorkConfig.defaultContentType]
     */
    protected open fun contentType(): MediaType? = null

    /**
     * 用于指定全局网络客户端配置的key
     *
     * @return 指向[WorkConfig.configs]的key，null或key不存在则表示使用默认客户端配置[WorkConfig.defaultConfig]
     */
    protected open fun configKey(): String? = null

    /**
     * 创建并填充请求头
     */
    protected open suspend fun headers(): Map<String, String>? = null

    /**
     * 后处理自定义配置http请求选择项
     *
     * @param options 请求将要使用的配置选项，修改[options]的属性以定制http行为。
     * [options]包含[Work]各项生命周期创建的请求属性，
     * 除此以外，[Options.readTimeout]和[Options.writeTimeout]等可以在此处修改
     */
    protected open suspend fun onPostOptions(options: Options) = Unit

    /**
     * 覆盖请求实现方法
     *
     * 默认实现为[workRequestImp]
     * 如果要覆盖全局实现，请覆盖[WorkConfig.workRequest]
     * 如果仅覆盖本任务请重写此方法
     *
     * @return 自定义实现的[WorkRequest]
     */
    protected open fun workRequest(options: Options) =
        (WorkConfig.configs[options.configKey] ?: WorkConfig.defaultConfig).workRequest

    /**
     * 转换OkHttp请求结果数据[ResponseBody]到用户定义的[H]类型数据
     *
     * @param data 本次请求中流转的数据
     * @param body 请求成功响应的数据体，无需执行[ResponseBody.close]，框架会负责关闭
     *
     * @return 转换到[H]数据类型，为了能良好的输出响应数据日志，此数据类型的[toString]方法应当可以输出可读的字符串表示
     *
     * @throws Exception 如果转换失败或响应数据格式不合规请抛出异常
     */
    protected abstract suspend fun onResponseConvert(data: T, body: ResponseBody): H

    /**
     * 提取服务执行结果
     *
     * http响应成功时判断本次业务请求真正的成功或失败结果
     *
     * @param data 本次请求中流转的数据
     * @param response 经由[onResponseConvert]转换后的数据结果
     *
     * @return 本次业务请求真正的成功或失败结果。
     */
    protected abstract suspend fun onRequestResult(data: T, response: H): Boolean

    /**
     * 提取服务执行成功时返回的真正有用结果数据
     *
     * 在服务请求成功后调用，即[onRequestResult]返回值为true时被调用
     *
     * @param data 本次请求中流转的数据
     * @param response 经由[onResponseConvert]转换后的数据结果
     *
     * @return 请求成功后的任务返回真正结果数据对象[D]，将会设置给[WorkData.result]
     */
    protected abstract suspend fun onRequestSuccessful(data: T, response: H): D?

    /**
     * 提取或设置服务返回的成功结果消息
     *
     * 在服务请求成功后调用，即[onRequestResult]返回值为true时被调用
     *
     * @param data 本次请求中流转的数据
     * @param response 经由[onResponseConvert]转换后的数据结果
     *
     * @return 成功消息，将会设置给[WorkData.message]
     */
    protected open fun onRequestSuccessfulMessage(data: T, response: H): String? = null

    /**
     * 提取或设置服务执行失败时的返回结果数据
     *
     * 在服务请求失败后调用，即[onRequestResult]返回值为false时被调用
     *
     * @param data 本次请求中流转的数据
     * @param response 经由[onResponseConvert]转换后的数据结果
     *
     * @return 生成请求失败后任务返回真正结果数据对象[D]，可能是一个默认值，将会设置给[WorkData.result]
     */
    protected open suspend fun onRequestFailed(data: T, response: H): D? = null

    /**
     * 提取或设置服务返回的失败结果消息
     *
     * 在服务请求失败后调用，即[onRequestResult]返回值为false时被调用
     *
     * @param data 本次请求中流转的数据
     * @param response 经由[onResponseConvert]转换后的数据结果
     *
     * @return 失败消息，将会设置给[WorkData.message]
     */
    protected open fun onRequestFailedMessage(data: T, response: H): String? = null

    /**
     * 服务器响应数据解析失败后调用
     *
     * 即在[Work.onParse]返回false时调用
     *
     * @param data 本次请求中流转的数据
     *
     * @return 响应数据解析失败时的消息，将会设置给[WorkData.message]
     */
    protected open suspend fun onParseFailed(data: T): String? = null

    /**
     * 网络连接建立成功，但是服务器响应错误时调用
     *
     * 即响应码不是2xx，如4xx，5xx等
     *
     * @param data 本次请求中流转的数据
     *
     * @return 服务器响应错误时的消息，将会设置给[WorkData.message]
     */
    protected open fun onNetworkRequestFailed(data: T): String? = null

    /**
     *  网络连接建立失败时调用，即网络不可用
     *
     *  @param data 本次请求中流转的数据
     *
     *  @return 网络无效时的消息，将会设置给[WorkData.message]
     */
    protected open fun onNetworkError(data: T): String? = null

    /**
     * 本次任务执行成功(即[onRequestResult]返回值为true)完成后执行
     *
     * 该方法在[onFinished]之前被调用，
     * 该方法与[onCanceled]和[onFailed]互斥。
     *
     * 如果任务结果从缓存加载，即[onStarted]返回值不为null，则该方法不会执行，
     * 也就是说此方法一定会在网络请求数据成功解析后被执行，这里是个写入缓存的好地方。
     *
     * @suppress 本方法禁止抛出异常，否则会破坏[Work]流程，如有危险操作请自行捕获异常
     *
     * @param data 本次请求中流转的数据
     */
    protected open suspend fun onSuccessful(data: T) = Unit

    /**
     * 本次任务执行失败后执行
     *
     * 该方法在[onFinished]之前被调用，
     * 该方法与[onCanceled]和[onSuccessful]互斥
     *
     * @suppress 本方法禁止抛出异常，否则会破坏[Work]流程，如有危险操作请自行捕获异常
     *
     * @param data 本次请求中流转的数据
     */
    protected open suspend fun onFailed(data: T) = Unit

    /**
     * 任务被取消（协程取消）时调用
     *
     * 该方法在[onFinished]之前被调用
     * 该方法与[onSuccessful]和[onFailed]互斥
     *
     * @suppress 本方法禁止抛出异常，否则会破坏[Work]流程，如有危险操作请自行捕获异常
     *
     * @param data 本次请求中流转的数据
     */
    protected open suspend fun onCanceled(data: T) = Unit

    /**
     * 最后执行的一个方法
     *
     * 此方法为最后一个生命周期方法，总是被执行
     * 如果协程取消时需要在此方法中执行多个挂起任务，请自行切换到[kotlinx.coroutines.NonCancellable]
     *
     * @suppress 本方法禁止抛出异常，否则会破坏[Work]流程，如有危险操作请自行捕获异常
     *
     * @param data 本次请求中流转的数据
     */
    protected open suspend fun onFinished(data: T) = Unit
}