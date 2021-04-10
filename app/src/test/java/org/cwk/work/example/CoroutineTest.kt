// Created by 超悟空 on 2021/4/10.

package org.cwk.work.example

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.cwk.work.*
import org.junit.Before
import org.junit.Test

/**
 * 与协程的协作测试
 */
class CoroutineTest {
    @Before
    fun setup() {
        WorkConfig.debugWork = true // 开启调试模式可以输出日志（默认开启）
        // 重定向[Work]库日志，避免测试环境执行[android.util.Log]
        workLog = { priority, tag, message, data ->
            println("$tag - v$priority: ${message ?: ""} ${data ?: ""}")
        }

        WorkConfig.defaultConfig = WorkConfig(baseUrl = "http://httpbin.org/")
    }

    /**
     * 取消测试
     */
    @Test
    fun cancelTest() = runBlocking {
        val startTime = System.currentTimeMillis()

        // [Work]库没有提供直接的取消接口
        // 但是[Work]与协程是协作的，支持完全遵循协程的取消行为
        // 所以我们可以通过取消协程来取消一个任务
        val job = launch {
            val d = TestData("超悟空", age = 32)

            try {

                val work = DelayWork(d, 5).start()

                if (work.success) {
                    println("work result ${work.result}")
                } else {
                    println("work error ${work.errorType} message ${work.message}")
                }

            } finally {
                println("work is cancelled ${System.currentTimeMillis() - startTime}")
            }
        }

        delay(1000)

        println("job cancel ${System.currentTimeMillis() - startTime}")
        job.cancelAndJoin()

        println("job cancel ${System.currentTimeMillis() - startTime}")
    }

    /**
     * 使用协程并发请求
     */
    @Test
    fun concurrentlyTest() = runBlocking {

        // 充分利用协程组合，比如使用流和异步来控制[Work]并发等

        val startTime = System.currentTimeMillis()

        val works = flow {
            val d = TestData("超悟空", age = 32)
            val work = DelayWork(d, 1)
            // 启动10次请求
            (1..10).map {
                async {
                    work.start()
                    // 每个任务完成后返回一个任务号
                    "work $it"
                }
            }.forEach {
                emit(it.await())
            }
        }

        works.collect {
            println("$it finish at ${System.currentTimeMillis() - startTime}")
        }
    }

    /**
     * 任务自启动协程快捷方式测试
     */
    @Test
    fun shortcutTest() = runBlocking {
        val d = TestData("超悟空", age = 32)
        val work = DelayWork(d, 1)

        // 自身启动一个协程执行任务，并在回调中返回任务结果
        // 此处使用本协程作用域，默认使用的[MainScope]需要依赖android环境
        val job = work.launch(this) {

            yield()

            // 此处it就是任务结果数据[WorkData]
            println("work launch result ${it.result}")
        }

        job.join()

        // 自身启动一个异步，并在回调中返回任务结果
        // 此处使用本协程作用域，默认使用的[MainScope]需要依赖android环境
        val def = work.async(this) {
            yield()

            // 此处it就是任务结果数据[WorkData]
            "work async result ${it.result}"
        }

        println(def.await())
    }
}