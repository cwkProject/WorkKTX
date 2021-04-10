// Created by 超悟空 on 2021/4/9.

package org.cwk.work

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 执行任务
 * 任务的执行完全依赖协程，同样支持协程取消规范
 *
 * 推荐使用
 *
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param context 协程上下文，影响[Work]各生命周期方法的执行线程，其中部分网络请求关联方法总是在[Dispatchers.IO]中执行
 * @param sendProgressChannel 发送/上传进度的通道，参数为进度百分比，任务结束后会自动关闭通道，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效，
 * @param receiveProgressChannel 接收/下载进度监听器，参数为进度百分比，任务结束后会自动关闭通道
 *
 * @return 包含执行结果的包装类[T]
 */
suspend fun <D, T : WorkData<D>, H> Work<D, T, H>.start(
    retry: Int = 0,
    context: CoroutineContext = Dispatchers.IO,
    sendProgressChannel: SendChannel<Float>? = null,
    receiveProgressChannel: SendChannel<Float>? = null,
) = try {
    execute(context, retry, sendProgressChannel?.let {
        { current, total, _ ->
            try {
                it.offer(current * 100f / total)
            } catch (e: Exception) {
            }
        }
    }, receiveProgressChannel?.let {
        { current, total, _ ->
            try {
                it.offer(current * 100f / total)
            } catch (e: Exception) {
            }
        }
    })
} finally {
    sendProgressChannel?.close()
    receiveProgressChannel?.close()
}

/**
 * 执行任务，监听发送/上传进度
 * 任务的执行完全依赖协程，同样支持协程取消规范
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
 * 执行任务，监听发送/上传进度
 * 任务的执行完全依赖协程，同样支持协程取消规范
 *
 * @param channel 发送/上传进度的通道，参数为进度百分比，任务结束后会自动关闭通道，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效，
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param context 协程上下文，影响[Work]各生命周期方法的执行线程，其中部分网络请求关联方法总是在[Dispatchers.IO]中执行
 *
 * @return 包含执行结果的包装类[T]
 */
suspend fun <D, T : WorkData<D>, H> Work<D, T, H>.upload(
    channel: SendChannel<Float>,
    retry: Int = 0,
    context: CoroutineContext = Dispatchers.IO,
) = try {
    upload(retry, context) { current, total, _ ->
        try {
            channel.offer(current * 100f / total)
        } catch (e: Exception) {
        }
    }
} finally {
    channel.close()
}

/**
 * 执行任务，监听接收/下载进度
 * 任务的执行完全依赖协程，同样支持协程取消规范
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
 * 执行任务，监听接收/下载进度
 * 任务的执行完全依赖协程，同样支持协程取消规范
 *
 * @param channel 接收/下载进度监听器，参数为进度百分比，任务结束后会自动关闭通道
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param context 协程上下文，影响[Work]各生命周期方法的执行线程，其中部分网络请求关联方法总是在[Dispatchers.IO]中执行
 *
 * @return 包含执行结果的包装类[T]
 */
suspend fun <D, T : WorkData<D>, H> Work<D, T, H>.download(
    channel: SendChannel<Float>,
    retry: Int = 0,
    context: CoroutineContext = Dispatchers.IO,
) = try {
    download(retry, context) { current, total, _ ->
        try {
            channel.offer(current * 100f / total)
        } catch (e: Exception) {
        }
    }
} finally {
    channel.close()
}

/**
 * 使用[CoroutineScope.launch]协程构建器执行任务并在[block]中返回任务结果[T]
 *
 * 此模式为启动协程+执行任务的组合快捷方式
 *
 * @param coroutineScope 指定协程作用域，如果为null将使用[MainScope]作用域并在任务结束时取消
 * @param context 协程上下文
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param start 协程启动选项，参考[CoroutineScope.launch]
 * @param onSendProgress 发送/上传进度监听器，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效
 * @param onReceiveProgress 接收/下载进度监听器
 * @param block 协程作用域执行函数，参数为[Work]执行完成后返回的[T]
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
): Job = (coroutineScope ?: MainScope()).let { scope ->
    scope.launch(context, start) {
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
 * 使用[CoroutineScope.launch]协程构建器执行任务并在[block]中返回任务结果[T]
 * 使用[SendChannel]监听进度的特殊版本
 *
 * 此模式为启动协程+执行任务的组合快捷方式
 *
 * @param coroutineScope 指定协程作用域，如果为null将使用[MainScope]作用域并在任务结束时取消
 * @param context 协程上下文
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param start 协程启动选项，参考[CoroutineScope.launch]
 * @param sendProgressChannel 发送/上传进度的通道，参数为进度百分比，任务结束后会自动关闭通道，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效，
 * @param receiveProgressChannel 接收/下载进度监听器，参数为进度百分比，任务结束后会自动关闭通道
 * @param block 协程作用域执行函数，参数为[Work]执行完成后返回的[T]
 *
 * @return 包含执行结果的包装类[T]
 */
fun <D, T : WorkData<D>, H> Work<D, T, H>.launchWithChannel(
    coroutineScope: CoroutineScope? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    retry: Int = 0,
    sendProgressChannel: SendChannel<Float>? = null,
    receiveProgressChannel: SendChannel<Float>? = null,
    block: suspend CoroutineScope.(T) -> Unit
): Job = launch(coroutineScope, context, start, retry, sendProgressChannel?.let {
    fun(current: Long, total: Long, _: Boolean) {
        try {
            it.offer(current * 100f / total)
        } catch (e: Exception) {
        }
    }
}, receiveProgressChannel?.let {
    fun(current: Long, total: Long, _: Boolean) {
        try {
            it.offer(current * 100f / total)
        } catch (e: Exception) {
        }
    }
}) {
    sendProgressChannel?.close()
    receiveProgressChannel?.close()
    block(it)
}.apply {
    invokeOnCompletion {
        if (it is CancellationException) {
            sendProgressChannel?.close()
            receiveProgressChannel?.close()
        }
    }
}

/**
 * 使用[CoroutineScope.async]协程构建器执行任务并在[block]中返回任务结果[T]
 *
 * 此模式为启动协程+执行任务的组合快捷方式
 *
 * @param coroutineScope 指定协程作用域，如果为null将使用[MainScope]作用域并在任务结束时取消
 * @param context 协程上下文
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param start 协程启动选项，参考[CoroutineScope.async]
 * @param onSendProgress 发送/上传进度监听器，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效
 * @param onReceiveProgress 接收/下载进度监听器
 * @param block 协程作用域执行函数，参数为[Work]执行完成后返回的[T]
 *
 * @return 包含执行结果的包装类[T]
 */
fun <D, T : WorkData<D>, H, R> Work<D, T, H>.async(
    coroutineScope: CoroutineScope? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    retry: Int = 0,
    onSendProgress: OnProgress? = null,
    onReceiveProgress: OnProgress? = null,
    block: suspend CoroutineScope.(T) -> R
): Deferred<R> = (coroutineScope ?: MainScope()).let { scope ->
    scope.async(context, start) {
        block(this@async.execute(context, retry, onSendProgress, onReceiveProgress))
    }.apply {
        invokeOnCompletion {
            if (coroutineScope == null) {
                scope.cancel()
            }
        }
    }
}

/**
 * 使用[CoroutineScope.async]协程构建器执行任务并在[block]中返回任务结果[T]
 * 使用[SendChannel]监听进度的特殊版本
 *
 * 此模式为启动协程+执行任务的组合快捷方式
 *
 * @param coroutineScope 指定协程作用域，如果为null将使用[MainScope]作用域并在任务结束时取消
 * @param context 协程上下文
 * @param retry 请求失败重试次数，0表示不重试，实际请求1次，1表示重试1次，实际最多请求两次，以此类推
 * @param start 协程启动选项，参考[CoroutineScope.async]
 * @param sendProgressChannel 发送/上传进度的通道，参数为进度百分比，任务结束后会自动关闭通道，在[Work.httpMethod]为[HttpMethod.GET]和[HttpMethod.HEAD]时无效，
 * @param receiveProgressChannel 接收/下载进度监听器，参数为进度百分比，任务结束后会自动关闭通道
 * @param block 协程作用域执行函数，参数为[Work]执行完成后返回的[async]
 *
 * @return 包含执行结果的包装类[T]
 */
fun <D, T : WorkData<D>, H, R> Work<D, T, H>.asyncWithChannel(
    coroutineScope: CoroutineScope? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    retry: Int = 0,
    sendProgressChannel: SendChannel<Float>? = null,
    receiveProgressChannel: SendChannel<Float>? = null,
    block: suspend CoroutineScope.(T) -> R
): Deferred<R> = async(coroutineScope, context, start, retry, sendProgressChannel?.let {
    fun(current: Long, total: Long, _: Boolean) {
        try {
            it.offer(current * 100f / total)
        } catch (e: Exception) {
        }
    }
}, receiveProgressChannel?.let {
    fun(current: Long, total: Long, _: Boolean) {
        try {
            it.offer(current * 100f / total)
        } catch (e: Exception) {
        }
    }
}) {
    sendProgressChannel?.close()
    receiveProgressChannel?.close()
    block(it)
}.apply {
    invokeOnCompletion {
        if (it is CancellationException) {
            sendProgressChannel?.close()
            receiveProgressChannel?.close()
        }
    }
}