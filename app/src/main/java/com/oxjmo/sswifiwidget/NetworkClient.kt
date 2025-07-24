package com.oxjmo.sswifiwidget

import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun executeRequest(
        url: String,
        method: String? = "GET",
        body: RequestBody? = null,
        headers: Headers? = emptyMap<String, String>().toHeaders(),
    ): Response {
        val request = Request.Builder()
            .url(url.toHttpUrl())
            .apply {if (headers != null) this.headers(headers)}

        if (method == "POST") {
            request.post(body ?: RequestBody.create(null, ByteArray(0)))
        } else {
            request.get()
        }
        return client.newCall(request.build()).execute()
    }

    fun requestString  (
        url: String,
        method: String? = "GET",
        body: RequestBody? = null,
        headers: Headers? = emptyMap<String, String>().toHeaders()
    ): String {
        val response = executeRequest(url, method, body, headers)
        val stringData = response.body?.string() ?: "Error: no data"
        response.close()
        return stringData
    }

    fun requestJsonObject  (
        url: String,
        method: String? = "GET",
        body: RequestBody? = null,
        headers: Headers? = emptyMap<String, String>().toHeaders()
    ): JSONObject {
        val response = executeRequest(url, method, body, headers)
        val objectData = response.body?.string() ?: "Error: no data"
        response.close()
        return JSONObject(objectData)
    }

    fun getResponseHeader(url: String, key: String): String {
        val response = executeRequest(url)
        if (response.isSuccessful) return response.header(key).toString()
        return ""
    }

}