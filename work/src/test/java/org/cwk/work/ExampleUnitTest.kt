package org.cwk.work

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
       val url= "http://httpbin.org/".toHttpUrl().resolve("/tools/testing")?.toString()

        println(url)
    }
}

private val executor = Executors.newScheduledThreadPool(1)
private val client = OkHttpClient()

fun run() {
    val request = Request.Builder()
        .url("http://httpbin.org/delay/2") // This URL is served with a 2 second delay.
        .build()

    val startNanos = System.nanoTime()
    val call = client.newCall(request)

    // Schedule a job to cancel the call in 1 second.
    executor.schedule({
        System.out.printf("%.2f Canceling call.%n", (System.nanoTime() - startNanos) / 1e9f)
        call.cancel()
        System.out.printf("%.2f Canceled call.%n", (System.nanoTime() - startNanos) / 1e9f)
    }, 1, TimeUnit.SECONDS)

    System.out.printf("%.2f Executing call.%n", (System.nanoTime() - startNanos) / 1e9f)
    try {
        call.execute().use { response ->
            System.out.printf("%.2f Call was expected to fail, but completed: %s%n",
                (System.nanoTime() - startNanos) / 1e9f, response)
        }
    } catch (e: IOException) {
        System.out.printf("%.2f Call failed as expected: %s%n",
            (System.nanoTime() - startNanos) / 1e9f, e)
    }
}