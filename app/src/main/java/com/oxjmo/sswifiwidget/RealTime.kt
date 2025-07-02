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

/**
 * Implementation of App Widget functionality.
 */
class RealTime : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH = "com.example.widgetdemo.REFRESH"
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

        GlobalScope.launch {
            val url = "http://cloud.oxjmo.top/test/test.json".toHttpUrl()
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response: Response = client.newCall(request).execute()
            val data = response.body?.string() ?: "Error: no date"
            println("数据： $data")
        }




        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(calendar.time)
        views.setTextViewText(R.id.infoTitle, currentTime)
        views.setTextViewText(R.id.electricQuantity, "30%")
        appWidgetManager.updateAppWidget(appWidgetId, views)


        val refreshIntent = Intent(context, RealTime::class.java)
        refreshIntent.action = ACTION_REFRESH
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, pendingIntent)

        Log.d("MainActivity", "success")
    }

    // 静态方法：用于外部调用更新所有小部件
    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, RealTime::class.java)
        val ids = manager.getAppWidgetIds(componentName)
        for (id in ids) {
            updateAppWidget(context, manager, id)
        }
    }
}
