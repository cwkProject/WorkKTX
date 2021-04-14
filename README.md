# work

[![maven central](https://maven-badges.herokuapp.com/maven-central/org.cwk.kotlin/work/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.cwk.kotlin/work)

* 封装http业务接口协议，提供标准使用流程，基于kotlin协程和OkHttp实现，与公司http规范紧密结合，规范团队成员接口编写和使用方式。
* 核心设计理念为封装http接口的请求数据和响应数据的序列化和反序列化，接口调用处不能出现任何解析http数据的代码。
装配和解析代码应该全部由`Work`类完成，接口调用处使用一致的方式，无需关心http的实现方式和接口处理细节。
* 优点是规范团队接口编写方式，统一项目http接口代码风格。
* 本库依赖kotlin协程机制，与协程深度结合，仅启用协程的项目可用。

## Usage
on gradle

```gradle

repositories {
  google()
  mavenCentral()
}

dependencies {
  implementation 'io.github.cwkproject:work:1.0.0'
}

```

## 第一步实现公司http规范基类

通常项目中都有一个标准的基础http响应数据的包装结构，通常包含业务处理成功失败标志，消息，具体数据等字段，
`Work`库所说的"封装http业务接口协议"正是要处理此场景。

假设公司使用以下数据格式

```通用协议
{
  "code":0, // 响应码，为0表示本次请求成功，其它表示错误码
  "message":null, // 业务消息字符串，可以是成功时用于显示的信息，也可以是失败时的提示信息
  "result": {}  // 真正响应的有效业务数据，任意类型
}
```

实现一个包含`code`的通用任务结果数据结构，即`WorkData`的子类

```AppWorkData

class AppWorkData<D> : WorkData<D>(){
    var code : Int = 0

    // 解构，前三个由父类实现
    operator fun component4() = code
}

```

为了保持库的架构简单数据处理灵活易扩展，因此`Work`在处理响应数据时需要一个中间类型，参考`onResponseConvert`生命周期。
因此在通常处理json响应数据时我们需要一个中转数据结构，这个结构通常是与公司协议一致的数据结构。

比如对于上述协议，假设我们的项目基于[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) 库实现json序列化
（Gson的工作方式在kotlin中会突破空安全和默认参数导致错误）

```AppResponseJson

// 此处将[data]定义为[kotlinx.serialization.json.JsonElement]作为真实接口响应数据的中间类型，以便二次转换，此方式省去了声明Json类解析器的麻烦
@Serializable
data class AppResponseJson(val code : Int = 0, val message : String ?= null, val data : JsonElement? = null)

```

下一步实现一个`Work`基类

```BaseWork

abstract class BaseWork<D> : Work<D, AppWorkData<D>, AppResponseJson>() {
    override fun onCreateWorkData() = AppWorkData<D>()

    override suspend fun onResponseConvert(data: AppWorkData<D>, body: ResponseBody) =
        json.decodeFromString(body.string())

    override suspend fun onRequestResult(data: AppWorkData<D>, response: AppResponseJson) = 
        response.code == 0

    override fun onRequestFailedMessage(data: AppWorkData<D>, response: JsonElement) = response.message

    override fun onNetworkRequestFailed(data: AppWorkData<D>): String? = "服务器响应失败！"

    override fun onNetworkError(data: AppWorkData<D>): String? = "网络错误或不可用！"

    private companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}

```

以上就完成了使用`Work`库的前置工作，下面就可以实现具体的接口了。

## 增加接口

继承`BaseWork<D>`，`<D>`为真正需要返回的数据模型类。

示例

```

@Serializable
data class User(val accountId: String, val nickname: String)

class LoginWork(private val username: String, private val password: String) : BaseWork<User>() {
    override fun url() = "/login" // 接口相对路径，在[WorkConfig]中可配置全局baseUrl

    override fun httpMethod() = HttpMethod.POST // 设置为post请求

    override fun contentType() = MediaType.JSON // 此处使用"application/json"请求格式

    override suspend fun fillParams() = mapOf(
        "username" to username,
        "password" to password,
    )   // 框架会自行装配，具体规则请查看方法文档

    override suspend fun onRequestSuccess(data: AppWorkData<User>, response: AppResponseJson): User? =
        response.data?.let { Json.decodeFromJsonElement(it) }
        // 对还是中间格式的真实数据进行转换，[JsonElement]拥有多种方法可以简单的直接读取数据。
}

```

## 调用接口

`Work`库执行流程基于协程设计，所以任务启动必须在协程作用域内。
库实现了多种实用的任务启动函数方便用户使用。

```start

fun main() = runBlocking{

    // 创建一个任务实例
    val work = LoginWork("cwk","123456")

    // 最简单的启动方式，在协程作用域内启动，以同步的方式书写异步请求和响应，充分利用协程优势
    // 默认的Work会在[Dispatchers.IO]中执行，如果要控制Work工作上下文，请传入[CoroutineContext]，
    // 尽管可以控制Work的基础生命周期的工作[CoroutineContext]但是实际执行网络请求的部分生命周期依然会在[Dispatchers.IO]中执行。
    var data = work.start() // data为AppWorkData<User>类型

    if (data.success){
        println("登录成功 ${data.result?.nickname}")
    } else {
        println("${data.message} : ${data.code}")
    }

    // 仅监听接受/下载进度的快捷启动方式
    data = work.download { current, total, done ->
         println("work progress $current/$total done:$done")
    }

    // 仅监听发送/上传进度的快捷启动方式
    data = work.upload { current, total, done ->
         println("work progress $current/$total done:$done")
    }

    // 原始启动方式，其它启动方式基于此方法实现
    data = work.execute()

    // 使用自创建协程作用域的模式启动任务
    work.launch {
        // 默认在[MainScope]中启动，协程退出时会自动关闭作用域
        // 也可以在参数中传入用户指定的作用域，比如在Android中传入viewModelScope快速启动ViewModel作用域的协程

        // 此lambda方法执行时Work请求已经完成了

        // 此时it为任务结果的数据类
        if (it.success){
            println("登录成功 ${it.result?.nickname}")
        } else {
            println("${it.message} : ${it.code}")
        }

        // ... 执行后续的协程方法处理逻辑 
    }

    // 与work.launch类似，使用自创建协程作用域的异步启动任务
    work.async{
        // it同样是任务结果数据
    }.await()

}

```

## 文件上传

上传文件通常就是构建一个`multipart/form-data`请求体的post请求。

```UploadWork

class SimpleUploadWork(private val file: File) : BaseWork<Unit>() {
    override fun url() = "/upload"

    override fun httpMethod() = HttpMethod.POST

    override fun contentType() = MediaType.MULTIPART // 指定为多重表单类型

    override suspend fun fillParams() = mapOf(
        "type" to "image", // 同时携带的其它数据参数等
        "file" to file, // 此处也可以使用[FileWithMimeType]的包装类型明确指出上传给服务器的文件名和类型
    ) // 交给框架自动装配

    override suspend fun onRequestSuccess(data: AppWorkData<Unit>, response: AppResponseJson) = Unit
}

```

## 下载文件

由于下载文件时整个响应数据都是文件流，所以不能再继承`BaseWork`了，
此时需要一个简单的下载实现，当然也可以实现一个下载任务基类，便于扩展。

```DownloadWork

abstract class BaseDownloadWork<D> : Work<D, AppWorkData<D>, InputStream>() {
    override fun onCreateWorkData() = AppWorkData<D>()

    override suspend fun onResponseConvert(data: AppWorkData<D>, body: ResponseBody): InputStream =
        body.byteStream()

    override suspend fun onRequestResult(data: AppWorkData<D>, response: InputStream) = true

    override fun onNetworkRequestFailed(data: AppWorkData<D>): String? = "服务器响应失败！"

    override fun onNetworkError(data: AppWorkData<D>): String? = "网络错误或不可用！"
}

class DownloadWork(private val fileId: String,private val path: String) : BaseDownloadWork<File>() {
    override fun url() = "/download/$fileId"

    override suspend fun fillParams() = Unit

    override suspend fun onRequestSuccess(
        data: AppWorkData<File>,
        response: InputStream
    ): File? = File(path).apply{ outputStream().use{ response.copyTo(it) } }
}

```

## 取消任务

任务可以被被取消，但是`Work`本身并不提供取消接口，任务完全遵循协程取消机制。
当任务执行所在的协程作用域关闭时，已经启动的任务也会因为协程的取消而取消，正在访问的网络请求也会立刻中断。
如果要精确控制一个特定的请求，请将其放在`Job`或`Deferred`中以便在其它协程中随时取消。当然，也可以使用`timeout`。

```cancel

    fun main() = runBlocking {
        
        // [Work]库没有提供直接的取消接口
        // 但是[Work]与协程是协作的，支持完全遵循协程的取消行为
        // 所以我们可以通过取消协程来取消一个任务
        val job = launch {
            val work = DownloadWork("image234", "/files/img/tmp.jpg").start()

            if (work.success) {
                println("work result ${work.result?.exists()}")
            } else {
                println("work error ${work.errorType} message ${work.message}")
            }
        }

        // 延迟500毫秒取消任务
        delay(500)

        job.cancelAndJoin()

        // 或者使用[Work.launch]方式

        val job2 = DownloadWork("image234", "/files/img/tmp.jpg").launch {
            if (it.success) {
                println("work result ${it.result?.exists()}")
            } else {
                println("work error ${it.errorType} message ${it.message}")
            }
        }

        delay(500)

        job2.cancelAndJoin()
    }

```

## 全局设置和日志

`WorkConfig`包括所有支持的全局设置，在这里可以设置`baseUrl`，默认发送的请求体格式`defaultContentType`，使用的`OkHttpClient`等，
用户可以实现自定义的`OkHttpClient`，也可以实现自定义的请求装配函数`WorkRequest`。

如需修改请创建并覆盖默认配置，比如

```config

WorkConfig.defaultConfig = WorkConfig(baseUrl = "http://httpbin.org/")

```

`WorkConfig.configs`提供了多组全局配置支持，以便定制多个服务后台，多种网络配置等复杂场景。

`WorkConfig.debugWork = true` 时可以启动`Work`库调试模式，此时会输出日志，默认开启调试。
用户也可以重定向日志输出方法，需要覆盖一个全局函数变量`workLog`。

## 其他Work生命周期函数

`Work`中还有很多其它生命周期方法，用于处理接口的各种任务，原则是接口数据处理由接口自己(即`Work`)处理。
其它更多实用方法可以参考项目[测试用例](https://github.com/cwkProject/WorkKTX/tree/master/app/src/test/java/org/cwk/work/example).

## License

```License

Copyright 2021 超悟空, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

```
