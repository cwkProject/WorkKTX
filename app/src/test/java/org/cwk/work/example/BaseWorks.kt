// Created by 超悟空 on 2021/4/10.

package org.cwk.work.example

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.ResponseBody
import org.cwk.work.Work
import org.cwk.work.WorkData
import java.io.InputStream

/**
 * 简单的将[JsonElement]（由于测试环境没有android支持环境，不能使用[org.json.JSONObject]，实际使用中通常使用[org.json.JSONObject]）作为结果数据的中间处理类的[Work]基类，
 * 此方式无多余依赖，兼容性强，处理便捷
 */
abstract class BaseJsonElementWork<D> : Work<D, WorkData<D>, JsonElement>() {
    override fun onCreateWorkData() = WorkData<D>()

    override suspend fun onResponseConvert(data: WorkData<D>, body: ResponseBody): JsonElement =
        Json.decodeFromString(body.string())

    // 此处通常需要检测用户业务上定义的成功标识，比如state==true,或者errCode==0等等
    override suspend fun onRequestResult(data: WorkData<D>, response: JsonElement) = true

    override fun onRequestFailedMessage(data: WorkData<D>, response: JsonElement): String? =
        response.jsonObject["message"]?.jsonPrimitive?.contentOrNull // 此测试服务中并没有这个字段

    override fun onNetworkRequestFailed(data: WorkData<D>): String? = "服务器响应失败！"

    override fun onNetworkError(data: WorkData<D>): String? = "网络错误或不可用！"
}

/**
 * 简单的文件下载[Work]基类
 *
 * 中间处理数据直接使用[InputStream]方便写入文件或在内存中处理
 */
abstract class BaseDownloadWork<D> : Work<D, WorkData<D>, InputStream>() {
    override fun onCreateWorkData() = WorkData<D>()

    override suspend fun onResponseConvert(data: WorkData<D>, body: ResponseBody): InputStream =
        body.byteStream()

    override suspend fun onRequestResult(data: WorkData<D>, response: InputStream) = true

    override fun onRequestFailedMessage(data: WorkData<D>, response: InputStream): String? =
        "不可能有这个错误"

    override fun onNetworkRequestFailed(data: WorkData<D>): String? = "服务器响应失败！"

    override fun onNetworkError(data: WorkData<D>): String? = "网络错误或不可用！"
}

/**
 * 使用标准响应数据结构作为中间转换数据，此处使用[Bin]演示
 * 实际项目中这个数据结构应当是公司的标准响应结构
 *
 * 比如以下这种
 *
 * ```
 * {
 *  "code":0, // 一般公司习惯把code为0或200作为请求成功
 *  "message":null, // 通常是失败提示信息
 *  "data":{} // 这里通常是任意类型，也就是接口真正的有效数据
 * }
 *
 * ```
 */
abstract class BaseBinWork<D> : Work<D, WorkData<D>, Bin>() {
    override fun onCreateWorkData() = WorkData<D>()

    override suspend fun onResponseConvert(data: WorkData<D>, body: ResponseBody): Bin =
        json.decodeFromString(body.string())

    // 此处通常需要检测用户业务上定义的成功标识，比如state==true,或者errCode==0等等
    override suspend fun onRequestResult(data: WorkData<D>, response: Bin) = true

    override fun onNetworkRequestFailed(data: WorkData<D>): String? = "服务器响应失败！"

    override fun onNetworkError(data: WorkData<D>): String? = "网络错误或不可用！"

    private companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}
