// Created by 超悟空 on 2021/4/10.

package org.cwk.work.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 测试请求参数
 */
@Serializable
data class TestData(val name: String? = null, val age: Int? = null)

/**
 * http://httpbin.org 测试站点的响应数据根结构，仅提取部分参数
 *
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
 *
 * @property json 假设为真正的有效业务数据，比如实例中的"data"，使用[JsonElement]作为中间格式，便于二次转换
 */
@Serializable
data class Bin(val json: JsonElement? = null, val url: String)

/**
 * 一个测试数据结构，将要与[Bin]配合使用
 */
@Serializable
data class User(val accountId: String, val nickname: String)