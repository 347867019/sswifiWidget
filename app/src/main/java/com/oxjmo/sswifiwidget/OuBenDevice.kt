package com.oxjmo.sswifiwidget

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class OuBenDevice() {
    private val networkClient = NetworkClient()
    private val hostUrl = "http://192.168.1.1"

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
            val homePageData = networkClient.requestJsonObject("$hostUrl/xml_action.cgi?method=get&module=duster&file=json_homepage_info$timestamp")
            val statusData = networkClient.requestJsonObject("$hostUrl/xml_action.cgi?method=get&module=duster&file=json_status_info$timestamp")
            val ip = homePageData.getString("lan_ip")
            val electricQuantity = statusData.getString("battery_percent")
            val rssi = statusData.getString("rssi")
            val provider = getProvider(homePageData.getString("iccid"))
            val networkMode = getNetworkMode(statusData.getString("sys_mode"))
            setViewText(R.id.ip, ip)
            setViewText(R.id.rssi, "${rssi}dBm")
            setViewText(R.id.electricQuantity, "$electricQuantity%")
            setViewText(R.id.provider, provider)
            setViewText(R.id.networkMode, networkMode)
            setViewVisibility(R.id.switchSIM, !sharedPreferences.getBoolean("oubenSwitchSimHidden", false))
            updateAppWidget()

            val oubenAccountId = sharedPreferences.getString("oubenAccountId", "")
            println(oubenAccountId)
            val flowInfo = networkClient.requestJsonObject("http://wifi2.ruijiadashop.cn/api/Card/loginCard", "POST", JSONObject().apply {
                put("dev_no", oubenAccountId)
            }.toString().toRequestBody("application/json".toMediaType()))
            val data = flowInfo.getJSONObject("data")
            val remainAmount = data.getString("remainAmount")
            val utilizableFlow = String.format("%.0fGB", remainAmount.toDouble() / 1024)
            setViewText(R.id.utilizableFlow, utilizableFlow)
            updateAppWidget()
        }catch (_: Exception) {}
    }

    data class SimInfo(
        val simId: String,
        val simImsi: String
    )

    fun onSwitchSim(
        callback: () -> Unit
    ) {
        val timestamp = System.currentTimeMillis()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val simData = networkClient.requestJsonObject("$hostUrl/xml_action.cgi?method=get&module=duster&file=json_mss_support_info$timestamp")
                val currentSimId = simData.getString("current_sim_id")
                val simListArray = simData.getJSONArray("sim_list")
                val simList = mutableListOf<SimInfo>()
                for(i in 0 until simListArray.length()) {
                    val item = simListArray.getJSONObject(i)
                    val simId = item.getString("sim_id")
                    val simImsi = item.getString("sim_imsi")
                    if(simImsi == "") continue
                    simList.add(SimInfo(simId, simImsi))
                }
                val findSimIndex = simList.indexOfFirst { it.simId == currentSimId }
                val nextSimIndex = if (findSimIndex + 1 >= simList.size){ 0 } else { findSimIndex + 1 }
                println(nextSimIndex)
                val response = networkClient.requestJsonObject("$hostUrl/xml_action.cgi?method=post&module=duster&file=json_mss_support_set$timestamp", "POST", JSONObject().apply {
                    put("sole_sim_id", nextSimIndex.toString())
                    put("switch_mode", "1")
                }.toString().toRequestBody("application/json".toMediaType()))
                if(response.getString("result") == "success") {
                    callback()
                }
            }catch (error: Exception) {
                println(error)
            }
        }
    }

    private fun getProvider(iccid: String): String {
        val chinaMobilePrefixes = listOf("898600", "898602", "898604", "898607", "898608", "898613")
        val chinaUnicomPrefixes = listOf("898601", "898606", "898609")
        val chinaTelecomPrefixes = listOf("898603", "898605", "898611")
        val chinaBroadcastPrefixes = listOf("898615")
        return when {
            chinaMobilePrefixes.any { iccid.startsWith(it) } -> "中国移动"
            chinaUnicomPrefixes.any { iccid.startsWith(it) } -> "中国联通"
            chinaTelecomPrefixes.any { iccid.startsWith(it) } -> "中国电信"
            chinaBroadcastPrefixes.any { iccid.startsWith(it) } -> "中国广电"
            else -> ""
        }
    }

    private fun getNetworkMode(mode: String): String {
        val gsmMode = listOf("3")
        val t3GMode = listOf("5", "15")
        val t4GMode = listOf("17")
        return when (mode) {
            in gsmMode -> "GSM"
            in t3GMode -> "3G"
            in t4GMode -> "4G"
            else -> ""
        }
    }

}