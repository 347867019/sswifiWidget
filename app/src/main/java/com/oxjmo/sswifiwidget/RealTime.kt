package com.oxjmo.sswifiwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
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

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Toast.makeText(context, "等待刷新", Toast.LENGTH_SHORT).show()
        val views = RemoteViews(context.packageName, R.layout.real_time)
        views.setTextViewText(R.id.ip, "--")
        views.setTextViewText(R.id.rssi, "--dBm")
        views.setTextViewText(R.id.electricQuantity, "--%")
        appWidgetManager.updateAppWidget(appWidgetId, views)

        val refreshIntent = Intent(context, RealTime::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

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
            views.setTextViewText(R.id.ip, ip)
            views.setTextViewText(R.id.rssi, "${rssi}dBm")
            views.setTextViewText(R.id.electricQuantity, "$electricQuantity%")
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTime = sdf.format(calendar.time)
            views.setTextViewText(R.id.infoTitle, currentTime)
            appWidgetManager.updateAppWidget(appWidgetId, views)
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
        val action = intent.action
        if (action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            } else {
                println("警告：appWidgetId 无效")
            }
        }
    }
}
