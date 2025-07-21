package com.oxjmo.sswifiwidget

import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class NetworkClient {
    private val client = OkHttpClient()

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
        return response.body?.string() ?: "Error: no data"
    }

    fun requestJsonObject  (
        url: String,
        method: String? = "GET",
        body: RequestBody? = null,
        headers: Headers? = emptyMap<String, String>().toHeaders()
    ): JSONObject {
        val response = executeRequest(url, method, body, headers)
        val data = response.body?.string() ?: "Error: no data"
        return JSONObject(data)
    }

    fun getResponseHeader(url: String, key: String): String {
        val response = executeRequest(url)
        if (response.isSuccessful) return response.header(key).toString()
        return ""
    }

    fun requestXML(
        url: String,
        method: String? = "GET",
        body: RequestBody? = null,
        headers: Headers? = emptyMap<String, String>().toHeaders()
    ) {
        val response = executeRequest(url, method, body, headers)
        val xmlData = response.body?.string() ?: "Error: no data"
        println(xmlData)
        val xml = parseXml(xmlData)
        println(xml)
    }

    private fun parseXml(xmlData: String) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xmlData.reader())
        var eventType = parser.eventType
        var text = ""
        var title = ""
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    // 开始标签
                }
                XmlPullParser.TEXT -> {
                    text = parser.text
                }
                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "title" -> {
                            title = text
                            println("Found title: $title")
                        }
                        // 可以添加更多标签解析
                    }
                }
            }
            eventType = parser.next()
        }
    }

}