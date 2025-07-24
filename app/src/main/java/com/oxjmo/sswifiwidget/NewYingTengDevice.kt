package com.oxjmo.sswifiwidget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NewYingTengDevice {
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
            login()
            val data = networkClient.requestJsonObject("$hostUrl/reqproc/proc_get?multi_data=1&cmd=battery_pers,lan_ipaddr,rssi,network_type&_=$timestamp")
            val ip = data.getString("lan_ipaddr")
            val rssi = data.getString("rssi")
            val networkType = data.getString("network_type")
            val electricQuantity = getBattery(data.getString("battery_pers"))
            setViewText(R.id.ip, ip)
            setViewText(R.id.rssi, "${rssi}dBm")
            setViewText(R.id.electricQuantity, "$electricQuantity%")
            setViewText(R.id.provider, "")
            setViewText(R.id.networkMode, networkType)
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
                val properties = networkClient.requestString("$hostUrl/i18n/Messages_zh-cn.properties")
                val simCount = Regex("CARD").findAll(properties).count()
                val data = networkClient.requestJsonObject("$hostUrl/reqproc/proc_get?cmd=alk_sim_select&_=$timestamp")
                val currentSim = data.getString("alk_sim_select").toInt()
                val nextSimIndex = if (currentSim + 1 >= simCount) { 0 } else { currentSim + 1 }
                val response = networkClient.requestJsonObject(
                    url = "$hostUrl/reqproc/proc_post",
                    method = "POST",
                    body = "goformId=ALK_SIM_SELECT&sim_select=$nextSimIndex".toRequestBody("application/x-www-form-urlencoded".toMediaType())
                )
                if(response.getString("result") == "success") {
                    callback()
                    networkClient.requestString(
                        url = "$hostUrl/reqproc/proc_post",
                        method = "POST",
                        body = "goformId=REBOOT_DEVICE".toRequestBody("application/x-www-form-urlencoded".toMediaType())
                    )
                }
            }catch (error: Exception) {
                println(error)
            }
        }
    }

    private fun login() {
        val response = networkClient.requestJsonObject("$hostUrl/reqproc/proc_post", "POST", FormBody.Builder()
            .add("goformId", "LOGIN")
            .add("password", "YWRtaW4=")
            .build())
        if(response.getString("result") == "0") return
        throw Exception("登录失败")
    }

    private fun getBattery(level: String): String {
        if(level == "1") return "30"
        if(level == "2") return "50"
        if(level == "3") return "70"
        if(level == "4") return "100"
        return "1"
    }

}