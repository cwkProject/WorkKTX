package org.cwk.work.example

import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.coroutines.suspendCoroutine

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() = runBlocking {
        val data = B("B test")
        val string = Json.encodeToString(data)
        println(string)

        val gson = Gson().toJson(data)
        println(gson)

        data.testSuspend()

        data.start(0, { current, total, done ->
            println("start $current $total $done")
        }) { current, total, done ->
            println("start $current $total $done")
        }
    }
}

typealias OnProgress = (current: Int, total: Int, done: Boolean) -> Unit

abstract class A {

    private val _log = "${javaClass.simpleName}@${this.hashCode().toString(16)}"

    @Transient
    open val httpM = "A httpM"

    open suspend fun testSuspend() = coroutineScope {
        println("testSuspend A")
    }

    fun start(retry: Int = 0, send: OnProgress? = null, receive: OnProgress? = null) {
        send?.invoke(1, 1, true)
        receive?.invoke(2, 2, false)
    }
}

@Serializable
class B(val url: String = "B url") : A() {

    override val httpM = "B httpM"

    override suspend fun testSuspend() {
        println("testSuspend B")
    }
}