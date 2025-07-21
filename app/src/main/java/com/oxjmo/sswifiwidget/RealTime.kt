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
import kotlinx.coroutines.launch

/**
 * Implementation of App Widget functionality.
 */
class RealTime : AppWidgetProvider() {
    private val ouBenDevice = OuBenDevice()
    private val newYingTengDevice = NewYingTengDevice()
    private val oldYingTengDevice = OldYingTengDevice()

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

        val setViewText: (Int, String) -> Unit = { viewId, text ->
            views.setTextViewText(viewId, if (text.isEmpty()) "--" else text)
        }
        val timestamp = System.currentTimeMillis()
        GlobalScope.launch {
            ouBenDevice.onRefresh(
                timeMillis = timeMillis,
                setViewText = setViewText,
                updateAppWidget = {appWidgetManager.updateAppWidget(appWidgetId, views)}
            )
        }
        GlobalScope.launch {
            newYingTengDevice.onRefresh(
                timeMillis = timeMillis,
                setViewText = setViewText,
                updateAppWidget = {appWidgetManager.updateAppWidget(appWidgetId, views)}
            )
        }
        GlobalScope.launch {
            oldYingTengDevice.onRefresh(
                timeMillis = timeMillis,
                setViewText = setViewText,
                updateAppWidget = {appWidgetManager.updateAppWidget(appWidgetId, views)}
            )
        }

    }

    data class SimInfo(
        val simId: String,
        val simImsi: String
    )

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (intent.action == ACTION_REFRESH && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, 200L)
        }
        if (intent.action == ACTION_SWITCH_SIM && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            GlobalScope.launch (Dispatchers.Main){
                ouBenDevice.onSwitchSim {
                    Toast.makeText(context, "切换成功", Toast.LENGTH_SHORT).show()
                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, 200L)
                }
            }
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
