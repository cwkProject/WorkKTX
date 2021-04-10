// Created by 超悟空 on 2021/4/10.

package org.cwk.work.example

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.cwk.work.*
import org.junit.Before
import org.junit.Test

/**
 * 上传/下载进度监听测试类
 */
class ProgressTest {

    @Before
    fun setup() {
        WorkConfig.debugWork = true // 开启调试模式可以输出日志（默认开启）
        // 重定向[Work]库日志，避免测试环境执行[android.util.Log]
        workLog = { priority, tag, message, data ->
            println("$tag - v$priority: ${message ?: ""} ${data ?: ""}")
        }

        WorkConfig.defaultConfig = WorkConfig(baseUrl = "http://httpbin.org/")
    }

    @Test
    fun uploadTest() = runBlocking {
        val buffer = SimpleDownloadWork().start().result!! //先下载一个文件作为上传内容

        val work = SimpleUploadWork(buffer)

        var data = work.upload { current, total, done ->
            println("first work progress $current/$total done:$done")
        }

        println("first work finish ${data.success}")

        // 使用[Channel]在其它协程内处理进度
        val channel = Channel<Float>()

        launch {
            for (progress in channel) {
                println("second work progress $progress")
            }
        }

        yield() // 让子协程跑起来

        data = work.upload(channel)

        println("second work finish ${data.success}")
    }

    @Test
    fun downloadTest() = runBlocking {
        val work = SimpleDownloadWork()

        var data = work.download { current, total, done ->
            println("first work progress $current/$total done:$done")
        }

        println("first work finish ${data.success}")

        // 使用[Channel]在其它协程内处理进度
        val channel = Channel<Float>()

        launch {
            for (progress in channel) {
                println("second work progress $progress")
            }
        }

        yield() // 让子协程跑起来

        data = work.download(channel)

        println("second work finish ${data.success}")
    }

    @Test
    fun uploadAndDownloadTest() = runBlocking {
        // 同时监听上传和下载进度

        val buffer = SimpleDownloadWork().start().result!! //先下载一个文件作为上传内容

        val work = SimpleUploadWork(buffer)

        var data = work.execute(onSendProgress = { current, total, done ->
            println("first work upload progress $current/$total done:$done")
        }) { current, total, done ->
            println("first work download progress $current/$total done:$done")
        }

        println("first work finish ${data.success}")

        // 使用[Channel]在其它协程内处理进度
        val uploadChannel = Channel<Float>()
        val downloadChannel = Channel<Float>()

        launch {
            for (progress in uploadChannel) {
                println("second work upload progress $progress")
            }
        }

        launch {
            for (progress in downloadChannel) {
                println("second work download progress $progress")
            }
        }

        yield() // 让子协程跑起来

        data = work.start(
            sendProgressChannel = uploadChannel,
            receiveProgressChannel = downloadChannel
        )

        println("second work finish ${data.success}")
    }
}