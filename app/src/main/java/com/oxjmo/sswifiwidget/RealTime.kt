package com.oxjmo.sswifiwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Implementation of App Widget functionality.
 */
class RealTime : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH = "com.oxjmo.sswifiwidget.REFRESH"
        const val ACTION_SWITCH_SIM = "com.oxjmo.sswifiwidget.SWITCHSIM"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, 200L)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        timeMillis: Long
    ) {
        Toast.makeText(context, "等待刷新", Toast.LENGTH_SHORT).show()
        val views = RemoteViews(context.packageName, R.layout.real_time)
        val textViews = listOf(R.id.ip, R.id.rssi, R.id.electricQuantity, R.id.provider, R.id.networkMode)
        for (viewId in textViews) {
            views.setTextViewText(viewId, "--")
        }
        views.setOnClickPendingIntent(R.id.refresh, registerRefreshEvent(context, appWidgetId))
        views.setOnClickPendingIntent(R.id.switchSIM, registerSwitchSIMEvent(context, appWidgetId))
        appWidgetManager.updateAppWidget(appWidgetId, views)

        fun setViewText(viewId: Int, charSequence: String) {
            views.setTextViewText(viewId, if(charSequence.isEmpty()) "--" else charSequence)
        }
        val timestamp = System.currentTimeMillis()
        GlobalScope.launch {
            try {
                delay(timeMillis)
                val homePageData = request("http://192.168.1.1/xml_action.cgi?method=get&module=duster&file=json_homepage_info$timestamp".toHttpUrl())
                val statusData = request("http://192.168.1.1/xml_action.cgi?method=get&module=duster&file=json_status_info$timestamp".toHttpUrl())
                val ip = homePageData.getString("lan_ip")
                val electricQuantity = statusData.getString("battery_percent")
                val rssi = statusData.getString("rssi")
                val provider = getProviderByOuben(homePageData.getString("iccid"))
                val networkMode = getNetworkModeByOuben(statusData.getString("sys_mode"))
                setViewText(R.id.ip, ip)
                setViewText(R.id.rssi, "${rssi}dBm")
                setViewText(R.id.electricQuantity, "$electricQuantity%")
                setViewText(R.id.provider, provider)
                setViewText(R.id.networkMode, networkMode)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }catch (_: Exception) {}
        }
        GlobalScope.launch {
            try {
                delay(timeMillis)
                val res = request("http://192.168.100.1/reqproc/proc_post".toHttpUrl(), "POST", FormBody.Builder()
                    .add("goformId", "LOGIN")
                    .add("password", "YWRtaW4=")
                    .build())
                println(res)
                val data = request("http://192.168.100.1/reqproc/proc_get?multi_data=1&cmd=battery_pers,lan_ipaddr,rssi,network_type&_=$timestamp".toHttpUrl())
                val ip = data.getString("lan_ipaddr")
                val rssi = data.getString("rssi")
                val networkType = data.getString("network_type")
                val electricQuantity = getBatteryByYingteng(data.getString("battery_pers"))
                setViewText(R.id.ip, ip)
                setViewText(R.id.rssi, "${rssi}dBm")
                setViewText(R.id.electricQuantity, "$electricQuantity%")
                setViewText(R.id.provider, "")
                setViewText(R.id.networkMode, networkType)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }catch (_: Exception) {}
        }

    }

    private fun getProviderByOuben(iccid: String): String {
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

    private fun getNetworkModeByOuben(mode: String): String {
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

    private fun getBatteryByYingteng(level: String): String {
        if(level == "1") return "30"
        if(level == "2") return "50"
        if(level == "3") return "70"
        if(level == "4") return "100"
        return "1"
    }

    data class SimInfo(
        val simId: String,
        val simImsi: String
    )

    private fun switchSim(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val timestamp = System.currentTimeMillis()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val simData = request("http://192.168.1.1/xml_action.cgi?method=get&module=duster&file=json_mss_support_info$timestamp".toHttpUrl())
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
                val result = request("http://192.168.1.1/xml_action.cgi?method=post&module=duster&file=json_mss_support_set$timestamp".toHttpUrl(), "POST", JSONObject().apply {
                    put("sole_sim_id", nextSimIndex.toString())
                    put("switch_mode", "1")
                }.toString().toRequestBody("application/json".toMediaType()))
                println("切换成功")
                println(result)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "切换成功", Toast.LENGTH_SHORT).show()
                    updateAppWidget(context, appWidgetManager, appWidgetId, 1000L)
                }
            }catch (error: Exception) {
                println(error)
            }
        }
    }

    private val client = OkHttpClient()

    private fun request(url: HttpUrl, method: String? = "GET", body: RequestBody? = null): JSONObject {
        val request = Request.Builder()
            .url(url)

        // 根据方法设置请求类型
        if (method == "POST") {
            request.post(body ?: RequestBody.create(null, ByteArray(0)))
        } else {
            request.get()
        }

        val response = client.newCall(request.build()).execute()
        val data = response.body?.string() ?: "Error: no data"
        return JSONObject(data)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (intent.action == ACTION_REFRESH && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, 200L)
        }
        if (intent.action == ACTION_SWITCH_SIM && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            switchSim(context, AppWidgetManager.getInstance(context), appWidgetId)
        }
    }

    private fun registerRefreshEvent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, RealTime::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun registerSwitchSIMEvent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, RealTime::class.java).apply {
            action = ACTION_SWITCH_SIM
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
