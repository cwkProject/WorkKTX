package org.cwk.work.example

import kotlinx.coroutines.runBlocking
import org.cwk.work.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder

/**
 * 基础可用性测试类
 */
class BasicTest {

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
    fun getTest() = runBlocking {
        val d = TestData("超悟空", age = 32)

        val work = SimpleGetWork(d).start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun getAnyTest() = runBlocking {
        val d = "name=超悟空&age=32"

        val work = SimpleGetAnyWork(d).start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun getQueryTest() = runBlocking {
        val d = "name=${URLEncoder.encode("超悟空", "UTF-8")}&age=32"

        val work = SimpleGetQueryWork(d).start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun postJsonTest() = runBlocking {
        val d = TestData("超悟空", age = 32)

        val work = SimplePostWork(d).start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun postFormTest() = runBlocking {
        val d = mapOf(
            "name" to "超悟空",
            "age" to 32,
            "family" to listOf("father", "mother", "me"),
            "house" to ListParams(ListFormat.CSV, listOf(125, "138", "118")),
        )

        val work = SimpleFormWork(d).start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun putTest() = runBlocking {
        val d = TestData("超悟空", age = 32)

        val work = SimplePutWork(d).start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun patchTest() = runBlocking {
        val d = TestData("超悟空", age = 32)

        val work = SimplePatchWork(d).start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun headTest() = runBlocking {
        val work = SimpleHeadWork().start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }

        // 此测试服务没有提供head支持
        assertEquals(404, work.response?.statusCode)
    }

    @Test
    fun deleteTest() = runBlocking {
        val work = SimpleDeleteWork().start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun uploadTest() = runBlocking {
        val buffer = "假装这是一段内存中的文件二进制数据，比如Bitmap的二进制".toByteArray()

        val work = SimpleUploadWork(buffer).start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }

    @Test
    fun downloadTest() = runBlocking {
        val work = SimpleDownloadWork().start()

        if (work.success) {
            println("work result size:${work.result?.size}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }
}

