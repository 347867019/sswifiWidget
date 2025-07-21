package com.oxjmo.sswifiwidget

import kotlinx.coroutines.delay
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
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
            val HA2ByUrl = md5("GET:/cgi/protected.cgi")
            val HA2ByHeader = md5("GET:/cgi/xml_action.cgi")
            val digestResByUrl = md5("$HA1:$nonce:00000001:$authCnonce:$authQop:$HA2ByUrl")
            val digestResByHeader = md5("$HA1:$nonce:00000002:$authCnonce:$authQop:$HA2ByHeader")
            val headers: Headers = mapOf(
                "Authorization" to "Digest username=\"$username\", realm=\"$authRealm\", nonce=\"$nonce\", uri=\"/cgi/xml_action.cgi\", response=\"$digestResByHeader\", qop=$authQop, nc=00000002, cnonce=\"$authCnonce\""
            ).toHeaders()
            val response = networkClient.requestString(
                url = "$hostUrl/login.cgi?Action=Digest&username=$username&realm=$authRealm&nonce=$nonce&response=$digestResByUrl&qop=$authQop&cnonce=$authCnonce&temp=asr&_=$timestamp",
                headers = headers
            )
            if(!response.contains("HTTP/1.1 200 OK")) return
            val xmlData = networkClient.requestString(
                url = "$hostUrl/xml_action.cgi?method=get&module=duster&file=status1",
                headers = headers
            )
            val extractTagContent: (String) -> String? = { tag ->
                val pattern = "<$tag>(.*?)</$tag>".toRegex()
                pattern.find(xmlData)?.groupValues?.get(1)
            }
            val ip = extractTagContent("ip").toString()
            val rssi = extractTagContent("rssi").toString()
            val electricQuantity = extractTagContent("Battery_voltage").toString()
            val provider = extractTagContent("network_name").toString()
            val sysMode = extractTagContent("sys_mode").toString()
            val dbm = getDbm(rssi, sysMode)
            val networkMode = getNetworkMode(sysMode)
            setViewText(R.id.ip, ip)
            setViewText(R.id.rssi, dbm)
            setViewText(R.id.electricQuantity, "$electricQuantity%")
            setViewText(R.id.provider, provider)
            setViewText(R.id.networkMode, networkMode)
            updateAppWidget()

        }catch (error: Exception) {
            println(error)
        }

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