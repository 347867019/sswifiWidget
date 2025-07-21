package com.oxjmo.sswifiwidget

import kotlinx.coroutines.delay
import okhttp3.FormBody

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
            val res = networkClient.requestJsonObject("$hostUrl/reqproc/proc_post", "POST", FormBody.Builder()
                .add("goformId", "LOGIN")
                .add("password", "YWRtaW4=")
                .build())
            println(res)
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

    private fun getBattery(level: String): String {
        if(level == "1") return "30"
        if(level == "2") return "50"
        if(level == "3") return "70"
        if(level == "4") return "100"
        return "1"
    }

}