package com.oxjmo.sswifiwidget

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import kotlin.random.Random

class OldYingTengDevice {
    private val networkClient = NetworkClient()
    private val hostUrl = "http://192.168.100.1"

    private var loginParamString = ""
    suspend fun onRefresh(
        timeMillis: Long,
        sharedPreferences: SharedPreferences,
        setViewText: (viewId: Int, charSequence: String) -> Unit,
        setViewVisibility: (viewId: Int, charSequence: Boolean) -> Unit,
        updateAppWidget: () -> Unit
    ) {
        delay(timeMillis)
        try {
            login()
            val xmlData = networkClient.requestString(
                url = "$hostUrl/xml_action.cgi?method=get&module=duster&file=status1",
                headers = getAuthHeaders("GET").headers
            )
            val ip = extractTagContent("ip", extractTagContent("lan", xmlData).toString()).toString()
            val rssi = extractTagContent("rssi", xmlData).toString()
            val electricQuantity = extractTagContent("Battery_voltage", xmlData).toString()
            val provider = extractTagContent("network_name", xmlData).toString()
            val sysMode = extractTagContent("sys_mode", xmlData).toString()
            val dbm = getDbm(rssi, sysMode)
            val networkMode = getNetworkMode(sysMode)
            setViewText(R.id.ip, ip)
            setViewText(R.id.rssi, dbm)
            setViewText(R.id.electricQuantity, "$electricQuantity%")
            setViewText(R.id.provider, provider)
            setViewText(R.id.networkMode, networkMode)
            setViewVisibility(R.id.switchSIM, !sharedPreferences.getBoolean("oldYingTengSwitchSimHidden", false))
            updateAppWidget()

        }catch (error: Exception) {
            println(error)
        }
    }

    fun onSwitchSim(
        callback: () -> Unit
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                login()
                val htmlData = networkClient.requestString("$hostUrl/html/internet/internet_connection.html")
                val simCount = countValidOptions(htmlData, "VersionSel").toInt()
                val statusXMLData = networkClient.requestString(
                    url = "$hostUrl/xml_action.cgi?method=get&module=duster&file=wan",
                    headers = getAuthHeaders("GET").headers
                )
                val currentSim = extractTagContent("version_flag", extractTagContent("wan", statusXMLData).toString())?.toInt() ?: 0
                val nextSimIndex = if (currentSim + 1 >= simCount) { 0 } else { currentSim + 1 }
                println(nextSimIndex)
                val response = networkClient.requestString(
                    url = "$hostUrl/xml_action.cgi?method=set&module=duster&file=wan",
                    method = "POST",
                    headers = getAuthHeaders("POST").headers,
                    body = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?> <RGW><wan><version_flag>$nextSimIndex</version_flag><version_flag_action>1</version_flag_action></wan></RGW>".toRequestBody("application/xml".toMediaType())
                )
                println(response)
                if(response.startsWith("<RGW><wan>")) {
                    callback()
                }
            }catch (error: Exception) {
                println(error.toString())
            }
        }

    }

    private fun login() {
        val timestamp = System.currentTimeMillis()
        loginParamString = networkClient.getResponseHeader("$hostUrl/login.cgi?_=$timestamp", "WWW-Authenticate")
        val authHeaderResult = getAuthHeaders("GET")
        val response = networkClient.requestString(
            url = "$hostUrl/login.cgi?Action=Digest&username=${authHeaderResult.username}&realm=${authHeaderResult.authRealm}&nonce=${authHeaderResult.nonce}&response=${authHeaderResult.digestResByUrl}&qop=${authHeaderResult.authQop}&cnonce=${authHeaderResult.authCnonce}&temp=asr&_=$timestamp",
            headers = authHeaderResult.headers
        )
        if(response.contains("HTTP/1.1 200 OK")) return
        throw Exception("登录失败")
    }

    data class AuthHeaderResult(
        val username: String,
        val authRealm: String,
        val nonce: String,
        val digestResByUrl: String,
        val authQop: String,
        val authCnonce: String,
        val headers: Headers
    )

    fun getAuthHeaders(requestType: String): AuthHeaderResult {
        val loginParamMap = loginParamString.replace("\"", "").replace(", ", " ").split(" ")
        val loginParam = loginParamMap.map { it.split("=").getOrElse(1) { "" } }
        val authRealm = loginParam.getOrNull(1) ?: ""
        val nonce = loginParam.getOrNull(2) ?: ""
        val authQop = loginParam.getOrNull(3) ?: ""
        val username = "admin"
        val password = "admin"
        val rand = Random.nextInt(100001)
        val date = System.currentTimeMillis()
        val salt = "$rand$date"
        val tmp = md5(salt)
        val authCnonce = tmp.take(16)
        val HA1 = md5("$username:$authRealm:$password")
        val HA2ByUrl = md5("GET:/cgi/protected.cgi")
        val HA2ByHeader = md5("$requestType:/cgi/xml_action.cgi")
        val digestResByUrl = md5("$HA1:$nonce:00000001:$authCnonce:$authQop:$HA2ByUrl")
        val digestResByHeader = md5("$HA1:$nonce:00000002:$authCnonce:$authQop:$HA2ByHeader")
        return AuthHeaderResult(
            username = username,
            authRealm = authRealm,
            nonce = nonce,
            digestResByUrl = digestResByUrl,
            authQop = authQop,
            authCnonce = authCnonce,
            headers = mapOf(
                "Authorization" to "Digest username=\"$username\", realm=\"$authRealm\", nonce=\"$nonce\", uri=\"/cgi/xml_action.cgi\", response=\"$digestResByHeader\", qop=$authQop, nc=00000002, cnonce=\"$authCnonce\""
            ).toHeaders()
        )
    }

    fun getDbm(rssi: String, sysMode: String): String {
        if(sysMode == "17") {
            return "${rssi.toInt() - 140}dBm"
        } else {
            return "${(rssi.toInt() * 2) - 113}dBm"
        }
    }

    fun getNetworkMode(sysMode: String): String {
        if(sysMode == "3") {
            return "GSM"
        } else if (sysMode == "5") {
            return "WCDMA"
        } else if (sysMode == "15") {
            return "TD-SCDMA"
        } else if (sysMode == "17") {
            return "LTE"
        } else {
            return "No Service"
        }
    }

    fun extractTagContent(tag: String, data: String): String? {
        val safeTag = Regex.escape(tag)
        val pattern = "<$safeTag>(.*?)</$safeTag>".toRegex()
        return pattern.find(data)?.groupValues?.get(1)
    }

    fun countValidOptions(html: String, selectId: String): Int {
        val selectRegex = Regex("<select\\s+id=[\"']$selectId[\"']>(.*?)</select>", RegexOption.DOT_MATCHES_ALL)
        val selectContent = selectRegex.find(html)?.groupValues?.get(1) ?: return 0
        val commentRegex = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        val cleanedContent = commentRegex.replace(selectContent, "")
        val optionRegex = Regex("<option\\b", RegexOption.IGNORE_CASE)
        return optionRegex.findAll(cleanedContent).count()
    }

    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        val sb = StringBuilder()
        for (byte in digest) {
            sb.append(String.format("%02x", byte.toInt() and 0xff))
        }
        return sb.toString()
    }

    fun hex(d: Int): String {
        val hexDigits = "0123456789ABCDEF"
        var num = d
        var h = hexDigits[num and 15].toString()
        while (num > 15) {
            num = num shr 4
            h = hexDigits[num and 15] + h
        }
        return h
    }

}