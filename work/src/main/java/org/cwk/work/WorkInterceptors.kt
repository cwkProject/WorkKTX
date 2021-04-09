// Created by 超悟空 on 2021/4/8.

package org.cwk.work

import okhttp3.*
import okio.*
import java.io.IOException

/**
 * 默认实现的网络请求重试拦截器
 *
 * @property tag 跟踪日志标签
 * @property retry 最大重试次数
 */
class WorkRetryInterceptor(private val tag: String, private val retry: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        var response: Response? = null
        var i = 0

        do {
            if (i > 0) {
                logW(tag, "retry $i")
                response?.close()
            }

            try {
                response = chain.proceed(request)
            } catch (e: IOException) {
                if (i >= retry || e.message == "Canceled") {
                    throw e
                }

                logW(tag, "retry $i failed", e)
            }
            i++
        } while (response?.isSuccessful != true && i <= retry)

        return response!!
    }
}

/**
 * 默认实现的发送/上传进度拦截器
 *
 * @property onProgress 发送/上传进度回调
 */
class SendProgressInterceptor(private val onProgress: OnProgress) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.body == null) {
            return chain.proceed(request)
        }

        return chain.proceed(
            request.newBuilder().method(request.method, sendProgress(request.body!!)).build()
        )
    }

    /**
     * 构建可监听发送进度的请求体
     */
    private fun sendProgress(body: RequestBody) = object : RequestBody() {
        override fun isDuplex() = body.isDuplex()

        override fun isOneShot() = body.isOneShot()

        override fun contentLength() = body.contentLength()

        override fun contentType() = body.contentType()

        override fun writeTo(sink: BufferedSink) = sink(sink).buffer().use { body.writeTo(it) }

        /**
         * 包装写入流回调进度接口
         */
        private fun sink(sink: Sink) = object : ForwardingSink(sink) {
            var bytesWritten = 0L

            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesWritten += byteCount
                onProgress(bytesWritten, contentLength(), bytesWritten == contentLength())
            }
        }
    }
}

/**
 * 默认实现的接收/下载进度拦截器
 *
 * @property onProgress 接收/下载进度回调
 */
class ReceiveProgressInterceptor(private val onProgress: OnProgress) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        return response.newBuilder().body(receiveProgress(response.body!!)).build()
    }

    /**
     * 构建可监听接收进度的响应体
     */
    private fun receiveProgress(body: ResponseBody) = object : ResponseBody() {
        private val bufferedSource by lazy {
            source(body.source()).buffer()
        }

        override fun contentLength() = body.contentLength()

        override fun contentType() = body.contentType()

        override fun source() = bufferedSource

        /**
         * 包装数据源回调进度接口
         */
        private fun source(source: Source) = object : ForwardingSource(source) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                onProgress(totalBytesRead, contentLength(), bytesRead == -1L)
                return bytesRead
            }
        }
    }
}