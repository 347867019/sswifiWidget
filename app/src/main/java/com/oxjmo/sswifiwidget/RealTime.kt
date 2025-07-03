package com.oxjmo.sswifiwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Intent
import android.content.ComponentName
import androidx.core.net.toUri
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
        val views = RemoteViews(context.packageName, R.layout.real_time)

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(calendar.time)
        views.setTextViewText(R.id.infoTitle, currentTime)
        views.setTextViewText(R.id.electricQuantity, "30%")
        appWidgetManager.updateAppWidget(appWidgetId, views)


        val refreshIntent = Intent(context, RealTime::class.java)
        refreshIntent.action = ACTION_REFRESH
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

        GlobalScope.launch {
            val url = "http://cloud.oxjmo.top/test/test.json".toHttpUrl()
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response: Response = client.newCall(request).execute()
            val data = response.body?.string() ?: "Error: no date"
            val jsonObject = JSONObject(data)
            println("数据： $data")
            println("电量： ${jsonObject.getString("battery")}")
            views.setTextViewText(R.id.electricQuantity, jsonObject.getString("battery"))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val active = intent.action
        if(active === ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                println("检测到按钮点击，开始刷新小控件")
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
        }
    }
}
