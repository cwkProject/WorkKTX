// Created by 超悟空 on 2021/4/9.

package org.cwk.work

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 执行任务
 * 任务的执行完全依赖协程，同样支持协程取消规范
 *
 * 启动无需关心上传或下载进度的任务的快捷方式，推荐使用
 *
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param context 协程上下文，影响[Work]各生命周期方法的执行线程，其中部分网络请求关联方法总是在[Dispatchers.IO]中执行
 *
 * @return 包含执行结果的包装类[T]
 */
suspend fun <D, T : WorkData<D>, H> Work<D, T, H>.start(
    retry: Int = 0,
    context: CoroutineContext = Dispatchers.IO
) = execute(context, retry)

/**
 * 执行任务
 * 任务的执行完全依赖协程，同样支持协程取消规范
 *
 * 启动需要关心上传进度的任务的快捷方式
 *
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param context 协程上下文，影响[Work]各生命周期方法的执行线程，其中部分网络请求关联方法总是在[Dispatchers.IO]中执行
 * @param onSendProgress 发送/上传进度监听器，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效
 *
 * @return 包含执行结果的包装类[T]
 */
suspend fun <D, T : WorkData<D>, H> Work<D, T, H>.upload(
    retry: Int = 0,
    context: CoroutineContext = Dispatchers.IO,
    onSendProgress: OnProgress
) = execute(context, retry, onSendProgress)

/**
 * 执行任务
 * 任务的执行完全依赖协程，同样支持协程取消规范
 *
 * 启动需要关心上传进度的任务的快捷方式
 *
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param context 协程上下文，影响[Work]各生命周期方法的执行线程，其中部分网络请求关联方法总是在[Dispatchers.IO]中执行
 * @param sendProgressChannel 发送/上传进度的通道，参数为进度百分比，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效
 *
 * @return 包含执行结果的包装类[T]
 */
suspend fun <D, T : WorkData<D>, H> Work<D, T, H>.upload(
    sendProgressChannel: Channel<Float>,
    retry: Int = 0,
    context: CoroutineContext = Dispatchers.IO,
) = upload( retry,context) {current, total, done ->
    sendProgressChannel.offer(current.toFloat()/total)
}

/**
 * 执行任务
 * 任务的执行完全依赖协程，同样支持协程取消规范
 *
 * 启动需要关心下载进度的任务的快捷方式
 *
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param context 协程上下文，影响[Work]各生命周期方法的执行线程，其中部分网络请求关联方法总是在[Dispatchers.IO]中执行
 * @param onReceiveProgress 接收/下载进度监听器
 *
 * @return 包含执行结果的包装类[T]
 */
suspend fun <D, T : WorkData<D>, H> Work<D, T, H>.download(
    retry: Int = 0,
    context: CoroutineContext = Dispatchers.IO,
    onReceiveProgress: OnProgress
) = execute(context, retry, onReceiveProgress = onReceiveProgress)

/**
 * 执行任务
 *
 * 此模式自身启动[CoroutineScope.launch]类型协程任务
 *
 * @param coroutineScope 指定协程作用域，如果为null将使用[MainScope]作用域并在任务结束时取消
 * @param context 协程上下文
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param start 协程启动选项，参考[CoroutineScope.launch]
 * @param onSendProgress 发送/上传进度监听器，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效
 * @param onReceiveProgress 接收/下载进度监听器
 * @param block 协程作用域执行函数，参数为[Work]执行完成后返回的[WorkData]
 *
 * @return 包含执行结果的包装类[T]
 */
fun <D, T : WorkData<D>, H> Work<D, T, H>.launch(
    coroutineScope: CoroutineScope? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    retry: Int = 0,
    onSendProgress: OnProgress? = null,
    onReceiveProgress: OnProgress? = null,
    block: suspend CoroutineScope.(T) -> Unit
): Job {
    val scope = coroutineScope ?: MainScope()
    return scope.launch(context, start) {
        block(this@launch.execute(context, retry, onSendProgress, onReceiveProgress))
    }.apply {
        invokeOnCompletion {
            if (coroutineScope == null) {
                scope.cancel()
            }
        }
    }
}

/**
 * 执行任务
 *
 * 此模式自身启动[CoroutineScope.launch]类型协程任务
 *
 * @param coroutineScope 指定协程作用域，如果为null将使用[MainScope]作用域并在任务结束时取消
 * @param context 协程上下文
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param start 协程启动选项，参考[CoroutineScope.launch]
 * @param onSendProgress 发送/上传进度监听器，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效
 * @param onReceiveProgress 接收/下载进度监听器
 * @param block 协程作用域执行函数，参数为[Work]执行完成后返回的[WorkData]
 *
 * @return 包含执行结果的包装类[T]
 */
fun <D, T : WorkData<D>, H> Work<D, T, H>.launch(
    coroutineScope: CoroutineScope? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    retry: Int = 0,
    sendProgressChannel: Channel<Float>? = null,
    receiveProgressChannel: Channel<Float>? = null,
    block: suspend CoroutineScope.(T) -> Unit
): Job {
    val scope = coroutineScope ?: MainScope()
    return scope.launch(context, start) {
        val onSendProgress:OnProgress? = if (sendProgressChannel==null) null else {current, total, done ->
            sendProgressChannel.send(current.toFloat()/total)
        }

        block(this@launch.execute(context, retry, onSendProgress, onReceiveProgress))
    }.apply {
        invokeOnCompletion {
            if (coroutineScope == null) {
                scope.cancel()
            }
        }
    }
}