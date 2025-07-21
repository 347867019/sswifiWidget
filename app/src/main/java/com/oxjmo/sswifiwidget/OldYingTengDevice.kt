package com.oxjmo.sswifiwidget

import kotlinx.coroutines.delay
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import java.security.MessageDigest
import kotlin.random.Random

class OldYingTengDevice {
    private val networkClient = NetworkClient()
    private val hostUrl = "http://192.168.100.1"

    suspend fun onRefresh(
        timeMillis: Long,
        setViewText: (viewId: Int, charSequence: String) -> Unit,
        updateAppWidget: () -> Unit
    ) {
        val timestamp = System.currentTimeMillis()
        delay(timeMillis)
        try {
            val loginParamString = networkClient.getResponseHeader("$hostUrl/login.cgi?_=$timestamp", "WWW-Authenticate")
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
            val HA2 = md5("GET:/cgi/protected.cgi")
            val digestRes = md5("$HA1:$nonce:00000001:$authCnonce:$authQop:$HA2")
            val headers: Headers = mapOf(
                "Authorization" to "Digest username=\"$username\", realm=\"$authRealm\", nonce=\"$nonce\", uri=\"/cgi/xml_action.cgi\", response=\"$digestRes\", qop=$authQop, nc=00000002, cnonce=\"$authCnonce\""
            ).toHeaders()
            networkClient.requestString(
                url = "$hostUrl/login.cgi?Action=Digest&username=$username&realm=$authRealm&nonce=$nonce&response=$digestRes&qop=$authQop&cnonce=$authCnonce&temp=asr&_=$timestamp",
                headers = headers
            )
            println("登录成功")
            networkClient.requestXML(
                url = "$hostUrl/xml_action.cgi?method=get&module=duster&file=status1",
                headers = headers
            )


        }catch (error: Exception) {
            println(error)
        }

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

}