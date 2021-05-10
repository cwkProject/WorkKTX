// Created by 超悟空 on 2021/4/10.

package org.cwk.work.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType
import org.cwk.work.HttpMethod
import org.cwk.work.JSON
import org.cwk.work.WorkData

/**
 * 指定延迟响应任务
 *
 * @property testData 需要发送的测试数据
 * @property delay 延迟时间，单位秒，最大10
 */
class DelayWork(private val testData: TestData, private val delay: Int) :
    BaseJsonElementWork<String>() {
    override fun url() = "/delay/$delay"

    override suspend fun fillParams() = mapOf(
        "name" to testData.name,
        "age" to testData.age,
    ) // 交给框架自动装配

    override suspend fun onRequestSuccessful(
        data: WorkData<String>,
        response: JsonElement
    ): String? =
        response.jsonObject["args"]?.toString()
}

/**
 * 模拟用户登录任务
 *
 * @property user 模拟用户数据发给服务器，同时接受此用户数据响应作为收到服务器登录用户的模拟
 */
class UserLoginWork(private val user: User) : BaseBinWork<User>() {
    override fun url() = "/post"

    override fun httpMethod() = HttpMethod.POST

    override fun contentType() = MediaType.JSON

    override suspend fun fillParams() = Json.encodeToString(user)

    override suspend fun onRequestSuccessful(data: WorkData<User>, response: Bin): User? =
        response.json?.let { Json.decodeFromJsonElement(it) }
}

/**
 * 模拟用户注册任务
 *
 * 此处使用json序列化库与Work配合使用
 *
 * @property accountId 模拟用户id
 * @property nickname 模拟用户昵称
 */
@Serializable
class UserRegisterWork(private val accountId: String, private val nickname: String) :
    BaseBinWork<User>() {
    override fun url() = "/post"

    override fun httpMethod() = HttpMethod.POST

    override fun contentType() = MediaType.JSON

    override suspend fun fillParams() = Json.encodeToString(this)

    override suspend fun onRequestSuccessful(data: WorkData<User>, response: Bin): User? =
        response.json?.let { Json.decodeFromJsonElement(it) }
}

/**
 * 可缓存任务
 *
 * @property testData 模拟数据
 */
class CacheableWork(private val id: Int, private val testData: TestData) : BaseBinWork<TestData>() {

    companion object {
        /**
         * 假设这是本地缓存数据库
         */
        private val caches = mutableMapOf<Int, TestData?>()
    }

    override fun url() = "/post"

    override fun httpMethod() = HttpMethod.POST

    override fun contentType() = MediaType.JSON

    override suspend fun fillParams() = Json.encodeToString(testData)

    override suspend fun onRequestSuccessful(data: WorkData<TestData>, response: Bin): TestData? =
        response.json?.let { Json.decodeFromJsonElement(it) }

    // 假设这里是根据参数读取缓存，如果缓存存在则直接返回缓存数据
    override suspend fun onStarted(data: WorkData<TestData>): TestData? = caches[id]

    override suspend fun onSuccessful(data: WorkData<TestData>) {
        // 网络请求成功会到达这里，结果数据一定来自于服务器
        caches[id] = data.result
    }

    override fun onFromCacheMessage(data: WorkData<TestData>): String? = "id:$id 命中缓存"
}