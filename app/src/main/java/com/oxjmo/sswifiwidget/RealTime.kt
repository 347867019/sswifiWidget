package com.oxjmo.sswifiwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
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

        val timestamp = System.currentTimeMillis()
        GlobalScope.launch {
            delay(200L)
            val homePageData = request("http://192.168.1.1/xml_action.cgi?method=get&module=duster&file=json_homepage_info$timestamp".toHttpUrl())
            val statusData = request("http://192.168.1.1/xml_action.cgi?method=get&module=duster&file=json_status_info$timestamp".toHttpUrl())
//            val simData = request("http://192.168.1.1/xml_action.cgi?method=get&module=duster&file=json_mss_support_info$timestamp".toHttpUrl())
//            println(simData)
            val ip = homePageData.getString("lan_ip")
            val electricQuantity = statusData.getString("battery_percent")
            val rssi = statusData.getString("rssi")
            val provider = getProvider(homePageData.getString("iccid"))
            val networkMode = getNetworkMode(statusData.getString("sys_mode"))
            views.setTextViewText(R.id.ip, ip)
            views.setTextViewText(R.id.rssi, "${rssi}dBm")
            views.setTextViewText(R.id.electricQuantity, "$electricQuantity%")
            views.setTextViewText(R.id.provider, provider)
            views.setTextViewText(R.id.networkMode, networkMode)
            appWidgetManager.updateAppWidget(appWidgetId, views)
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

    private fun request(url: HttpUrl): JSONObject {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response: Response = client.newCall(request).execute()
        val data = response.body?.string() ?: "Error: no date"
        return JSONObject(data)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (intent.action == ACTION_REFRESH && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
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
