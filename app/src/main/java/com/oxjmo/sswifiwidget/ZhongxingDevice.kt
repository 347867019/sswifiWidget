package com.oxjmo.sswifiwidget

import android.content.SharedPreferences
import kotlinx.coroutines.delay
import okhttp3.FormBody
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ZhongxingDevice {
    private val networkClient = NetworkClient()
    private val hostUrl = "http://192.168.0.1"
    private var properties = ""

    suspend fun onRefresh(
        timeMillis: Long,
        sharedPreferences: SharedPreferences,
        setViewText: (viewId: Int, charSequence: String) -> Unit,
        setViewVisibility: (viewId: Int, isVisible: Boolean) -> Unit,
        updateAppWidget: () -> Unit
    ) {
        val timestamp = System.currentTimeMillis()
        delay(timeMillis)
        try {
            login()
            getProperties()
            val data = networkClient.requestJsonObject("$hostUrl/goform/goform_get_cmd_process?multi_data=1&isTest=false&cmd=battery_pers,lan_ipaddr,rssi,network_type,network_provider&_=$timestamp")
            val ip = data.getString("lan_ipaddr")
            val rssi = data.getString("rssi")
            val networkType = data.getString("network_type")
            val electricQuantity = getBattery(data.getString("battery_pers"))
            val provider = getProvider(data.getString("network_provider"))
            setViewText(R.id.ip, ip)
            setViewText(R.id.rssi, "${rssi}dBm")
            setViewText(R.id.electricQuantity, "$electricQuantity%")
            setViewText(R.id.provider, provider)
            setViewText(R.id.networkMode, networkType)
            setViewVisibility(R.id.switchSIM, !sharedPreferences.getBoolean("zhongXingSwitchSimHidden", false))
            updateAppWidget()

        }catch (_: Exception) {}
    }

    fun onSwitchSim(
        callback: () -> Unit
    ) {
        val timestamp = System.currentTimeMillis()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                login()
                getProperties()
                val htmlData = networkClient.requestString("$hostUrl/tmpl/esim/esim.html")
                val simCount = Regex("EsimSimGrp").findAll(htmlData).count()
                val statusData = networkClient.requestJsonObject("$hostUrl/goform/goform_get_cmd_process?isTest=false&cmd=sim_esim_mode&multi_data=1&_=$timestamp")
                val currentSim = statusData.getString("sim_esim_mode").toInt()
                val nextSimIndex = if (currentSim + 1 >= simCount) { 0 } else { currentSim + 1 }
                val switchResponse = networkClient.requestJsonObject(
                    url = "$hostUrl/goform/goform_set_cmd_process",
                    method = "POST",
                    body = "goformId=set_esim&isTest=false&esimmode=$nextSimIndex".toRequestBody("application/x-www-form-urlencoded".toMediaType())
                )
                if(switchResponse.getString("result") == "success") {
                    callback()
                }
            }catch (error: Exception) {
                println(error)
            }
        }
    }

    private fun login() {
        val encodedBytes = Base64.encode("admin".toByteArray(), Base64.DEFAULT)
        val response = networkClient.requestJsonObject("$hostUrl/goform/goform_set_cmd_process ", "POST", FormBody.Builder()
            .add("isTest", "false")
            .add("goformId", "LOGIN")
            .add("username", String(encodedBytes))
            .add("password", String(encodedBytes))
            .build())
        if(response.getString("result") == "0") return
        throw Exception("登录失败")
    }

    private fun getProperties() {
        if(properties == "") properties = networkClient.requestString("$hostUrl/i18n/Messages_zh-cn.properties")
    }

    private fun getBattery(level: String): String {
        if(level == "1") return "30"
        if(level == "2") return "50"
        if(level == "3") return "70"
        if(level == "4") return "100"
        if(level == "5" || level == "0") return "0"
        return "1"
    }

    private fun getProvider(provider: String): String {
        val data = properties.trimIndent()
        if(provider == "中国广电") {
            val matchResult = Regex("China_radio_and_television = (.+?)\n").find(data)
            return matchResult?.groupValues?.get(1)?.toString() ?: provider
        }
        if(provider == "China Mobile") {
            val matchResult = Regex("china_mobile = (.+?)\n").find(data)
            return matchResult?.groupValues?.get(1)?.toString() ?: provider
        }
        if(provider == "China Unicom") {
            val matchResult = Regex("china_unicom = (.+?)\n").find(data)
            return matchResult?.groupValues?.get(1)?.toString() ?: provider
        }
        if(provider == "China Telecom") {
            val matchResult = Regex("china_telecom = (.+?)\n").find(data)
            return matchResult?.groupValues?.get(1)?.toString() ?: provider
        }
        return provider
    }
}