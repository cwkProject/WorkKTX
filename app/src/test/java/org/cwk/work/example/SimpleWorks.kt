// Created by 超悟空 on 2021/4/10.

package org.cwk.work.example

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.cwk.work.*
import java.io.InputStream

/**
 * 简单Get任务
 *
 * @property testData 需要发送的测试数据
 */
class SimpleGetWork(private val testData: TestData) : BaseJsonElementWork<String>() {
    override fun url() = "/get"

    override suspend fun fillParams() = mapOf(
        "name" to testData.name,
        "age" to testData.age,
    ) // 交给框架自动装配

    override suspend fun onRequestSuccessful(
        data: WorkData<String>,
        response: JsonElement
    ): String? =
        response.jsonObject["args"]?.jsonObject?.get("name")?.jsonPrimitive?.content // 返回发送的"name"
}

/**
 * 简单Get任务，已拼接参数
 *
 * @property params 已经用 key=value&key2=value2拼接好的未urlencoded转码的参数
 */
class SimpleGetAnyWork(private val params: String) : BaseJsonElementWork<String>() {
    override fun url() = "/get"

    override suspend fun fillParams() = params

    override suspend fun onRequestSuccessful(
        data: WorkData<String>,
        response: JsonElement
    ): String? =
        response.jsonObject["args"]?.toString() // 返回发送的数据
}

/**
 * 简单Get任务，将参数拼接在地址中
 *
 * @property params 已经用 key=value&key2=value2拼接好的已经过urlencoded转码的参数
 */
class SimpleGetQueryWork(private val params: String) : BaseJsonElementWork<String>() {
    override fun url() = "/get?$params"

    override suspend fun fillParams() = Unit // 地址中已经有参数，此处必须留空，否则会覆盖地址中的参数

    override suspend fun onRequestSuccessful(
        data: WorkData<String>,
        response: JsonElement
    ): String? =
        response.jsonObject["args"]?.toString() // 返回发送的数据
}

/**
 * 总是返回500的任务
 */
class SimpleErrorWork() : BaseJsonElementWork<Unit>() {
    override fun url() = "/status/500"

    override suspend fun fillParams() = Unit

    override suspend fun onRequestSuccessful(data: WorkData<Unit>, response: JsonElement) = Unit
}

/**
 * 简单Post任务
 *
 * @property testData 需要发送的测试数据
 */
class SimplePostWork(private val testData: TestData) : BaseJsonElementWork<TestData>() {
    override fun url() = "/post"

    override fun httpMethod() = HttpMethod.POST

    override fun contentType() = MediaType.JSON

    override suspend fun fillParams() =
        Json.encodeToString(testData) // 由于没有Android环境，框架无法自动转换Map -> json

    override suspend fun onRequestSuccessful(
        data: WorkData<TestData>,
        response: JsonElement
    ): TestData? =
        Json.decodeFromJsonElement(response.jsonObject["json"]!!) // 返回发送的数据
}

/**
 * 简单Post Form任务
 *
 * @property form 需要发送的测试数据
 */
class SimpleFormWork(private val form: Map<*, *>) : BaseJsonElementWork<Map<*, *>>() {
    override fun url() = "/post"

    // 全局默认的content-type为 application/x-www-form-urlencoded
    override fun httpMethod() = HttpMethod.POST

    override suspend fun fillParams() = form // 交给框架自动装配

    override suspend fun onRequestSuccessful(
        data: WorkData<Map<*, *>>,
        response: JsonElement
    ): Map<*, *>? = response.jsonObject["form"]!!.jsonObject // 返回发送的数据
}

/**
 * 简单Put Form任务
 *
 * @property testData 需要发送的测试数据
 */
class SimplePutWork(private val testData: TestData) : BaseJsonElementWork<TestData>() {
    override fun url() = "/put"

    // 全局默认的content-type为 application/x-www-form-urlencoded
    override fun httpMethod() = HttpMethod.PUT

    override suspend fun fillParams() = mapOf(
        "name" to testData.name,
        "age" to testData.age,
    ) // 交给框架自动装配

    override suspend fun onRequestSuccessful(
        data: WorkData<TestData>,
        response: JsonElement
    ): TestData? =
        Json.decodeFromJsonElement(response.jsonObject["form"]!!) // 返回发送的数据
}

/**
 * 简单Patch Form任务
 *
 * @property testData 需要发送的测试数据
 */
class SimplePatchWork(private val testData: TestData) : BaseJsonElementWork<TestData>() {
    override fun url() = "/patch"

    // 全局默认的content-type为 application/x-www-form-urlencoded
    override fun httpMethod() = HttpMethod.PATCH

    override suspend fun fillParams() = mapOf(
        "name" to testData.name,
        "age" to testData.age,
    ) // 交给框架自动装配

    override suspend fun onRequestSuccessful(
        data: WorkData<TestData>,
        response: JsonElement
    ): TestData? =
        Json.decodeFromJsonElement(response.jsonObject["form"]!!) // 返回发送的数据
}

/**
 * 简单Head任务
 *
 * 空参数任务
 */
class SimpleHeadWork : Work<Unit, WorkData<Unit>, Unit>() {
    override fun url() = "/head"

    override fun httpMethod() = HttpMethod.HEAD

    override suspend fun fillParams() = Unit

    override fun onCreateWorkData() = WorkData<Unit>()

    override suspend fun onRequestSuccessful(data: WorkData<Unit>, response: Unit) = Unit

    override suspend fun onResponseConvert(data: WorkData<Unit>, body: ResponseBody) = Unit

    override suspend fun onRequestResult(data: WorkData<Unit>, response: Unit): Boolean = true
}

/**
 * 简单Delete任务
 *
 * 空参数任务
 */
class SimpleDeleteWork : BaseJsonElementWork<Unit>() {
    override fun url() = "/delete"

    override fun httpMethod() = HttpMethod.DELETE

    override suspend fun fillParams() = Unit

    override suspend fun onRequestSuccessful(data: WorkData<Unit>, response: JsonElement) = Unit
}

/**
 * 简单上传任务
 *
 * @property buffer 需要上传的内存文件（测试环境没有准备本地文件）
 */
class SimpleUploadWork(private val buffer: ByteArray) : BaseJsonElementWork<Unit>() {
    override fun url() = "/post"

    override fun httpMethod() = HttpMethod.POST

    override fun contentType() = MediaType.MULTIPART

    override suspend fun fillParams() = mapOf(
        "name" to "测试内存文件",
        "file" to ByteArrayWithMimeType(buffer, "text/plan", "test.txt"),
    ) // 交给框架自动装配

    override suspend fun onRequestResult(data: WorkData<Unit>, response: JsonElement) =
        response.jsonObject["files"]?.jsonObject?.isNotEmpty() == true

    override suspend fun onRequestSuccessful(data: WorkData<Unit>, response: JsonElement) = Unit
}

/**
 * 简单下载任务
 */
class SimpleDownloadWork : BaseDownloadWork<ByteArray>() {
    override fun url() = "image/webp"

    override suspend fun fillParams() = Unit

    override suspend fun onRequestSuccessful(
        data: WorkData<ByteArray>,
        response: InputStream
    ): ByteArray? = response.readBytes()
}

/**
 * 简单空请求体Post任务
 */
class SimpleEmptyPostWork() : BaseJsonElementWork<Unit>() {
    override fun url() = "/post"

    override fun httpMethod() = HttpMethod.POST

    override fun contentType() = MediaType.JSON

    override suspend fun fillParams() = Unit

    override suspend fun onRequestSuccessful(
        data: WorkData<Unit>,
        response: JsonElement
    ): Unit = Unit
}

/**
 * 简单业务失败Post任务
 */
class SimpleFailedPostWork() : BaseJsonElementWork<Unit>() {
    override fun url() = "/post"

    override suspend fun onRequestResult(data: WorkData<Unit>, response: JsonElement) = false

    override fun httpMethod() = HttpMethod.POST

    override fun contentType() = MediaType.JSON

    override suspend fun fillParams() = Unit

    override suspend fun onRequestSuccessful(
        data: WorkData<Unit>,
        response: JsonElement
    ): Unit = Unit
}