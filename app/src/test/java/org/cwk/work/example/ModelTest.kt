// Created by 超悟空 on 2021/4/10.

package org.cwk.work.example

import kotlinx.coroutines.runBlocking
import org.cwk.work.WorkConfig
import org.cwk.work.start
import org.cwk.work.workLog
import org.junit.Before
import org.junit.Test

/**
 * 结合公司业务数据模型，模拟真实使用场景
 */
class ModelTest {

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
    fun loginTest() = runBlocking {

        val u = User("1234455", "超悟空")

        val login = UserLoginWork(u).start()

        if (login.success) {
            println("work result ${login.result}")
        } else {
            println("work error ${login.errorType} message ${login.message}")
        }
    }

    @Test
    fun registerTest() = runBlocking {

        val work = UserRegisterWork("sadasd", "超悟空").start()

        if (work.success) {
            println("work result ${work.result}")
        } else {
            println("work error ${work.errorType} message ${work.message}")
        }
    }
}