// Created by 超悟空 on 2021/4/9.

package org.cwk.work

import android.webkit.MimeTypeMap
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

/**
 * 可将[Map]转换为[RequestBody]
 */
internal fun Map<*, *>.toRequestBody(contentType: MediaType, listFormat: ListFormat) = when {
    "${contentType.type}/${contentType.subtype}" == "application/json" -> JSONObject(this).toString()
        .toRequestBody(contentType)
    contentType == MultipartBody.FORM -> MultipartBody.Builder().apply {
        setType(MultipartBody.FORM)
        handleSequence(listFormat) { key, value ->
            when (value) {
                is FileWithMimeType -> addFormDataPart(
                    key,
                    value.name ?: value.file.name,
                    value.asRequestBody()
                )
                is ByteArrayWithMimeType -> addFormDataPart(key, value.name, value.asRequestBody())
                is File -> addFormDataPart(
                    key,
                    value.name,
                    value.asRequestBody(value.extension.let {
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
                    }?.toMediaType())
                )
                else -> addFormDataPart(key, "$value")
            }
        }
    }.build()
    contentType == MediaType.FORM_DATA -> FormBody.Builder().apply {
        handleSequence(listFormat) { key, value -> add(key, "$value") }
    }.build()
    else -> throw IllegalArgumentException("unsupported auto convert $contentType")
}

/**
 * 根据框架规则将[Map]转换为可用于[MultipartBody]和[FormBody]的处理
 */
private fun Map<*, *>.handleSequence(listFormat: ListFormat, handler: (String, Any) -> Unit) {
    fun readNext(key: String, data: Any?) {
        fun handleList(data: List<*>, listFormat: ListFormat) = when (listFormat) {
            ListFormat.MULTI, ListFormat.MULTI_COMPATIBLE -> data.forEachIndexed { index, value ->
                val isCollection = value is Map<*, *> || value is List<*> || value is ListParams
                if (listFormat == ListFormat.MULTI) {
                    readNext("$key${if (isCollection) "[$index]" else ""}", value)
                } else {
                    readNext("$key[${if (isCollection) "$index" else ""}]", value)
                }
            }
            else -> readNext(key, data.joinToString(listFormat.separator()))
        }

        when (data) {
            is ListParams -> handleList(data.values, data.format)
            is List<*> -> handleList(data, listFormat)
            is Map<*, *> -> data.forEach {
                if (key.isEmpty()) {
                    readNext("${it.key}", it.value)
                } else {
                    readNext("$key[${it.key}]", it.value)
                }
            }
            is Any -> handler(key, data)
        }
    }

    readNext("", this)
}

/**
 * 将[FileWithMimeType]转换为[RequestBody]
 */
private fun FileWithMimeType.asRequestBody() = file.asRequestBody(mimeType.toMediaType())

/**
 * 将[ByteArrayWithMimeType]转换为[RequestBody]
 */
private fun ByteArrayWithMimeType.asRequestBody() = byteArray.toRequestBody(mimeType.toMediaType())