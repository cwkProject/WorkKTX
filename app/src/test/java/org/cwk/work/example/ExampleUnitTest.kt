package org.cwk.work.example

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() = runBlocking {

        val channel = Channel<Int>()

        val job = launch {
            println("1 launch")

            async {
                println("async start")
                delay(2000)
                println("async end")
            }.start()
            yield()
            subScope()
            channel.close()
            println("subScope finish")
        }

        job.invokeOnCompletion {
            println("job finally 1 $it")
            channel.close()
        }

        job.invokeOnCompletion {
            println("job finally 2 $it")
            channel.close()
        }

        delay(2000)
        println("1 join")
        job.cancelAndJoin()
    }

    suspend fun subScope() = coroutineScope {
        launch {
            println("2 launch")
            delay(1000)
            println("2 end")
        }

        launch {
            println("3 launch")
            delay(1000)
            println("3 end")
        }
    }
}

