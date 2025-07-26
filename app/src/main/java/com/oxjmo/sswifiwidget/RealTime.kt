package com.oxjmo.sswifiwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Implementation of App Widget functionality.
 */
class RealTime : AppWidgetProvider() {
    private val ouBenDevice = OuBenDevice()
    private val newYingTengDevice = NewYingTengDevice()
    private val oldYingTengDevice = OldYingTengDevice()
    private val zhongxingDevice = ZhongxingDevice()

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
        val textViews = listOf(R.id.ip, R.id.rssi, R.id.electricQuantity, R.id.provider, R.id.networkMode, R.id.utilizableFlow)
        for (viewId in textViews) {
            views.setTextViewText(viewId, "--")
        }
        views.setOnClickPendingIntent(R.id.refresh, registerRefreshEvent(context, appWidgetId))
        views.setOnClickPendingIntent(R.id.switchSIM, registerSwitchSIMEvent(context, appWidgetId))
        appWidgetManager.updateAppWidget(appWidgetId, views)

        val setViewText: (Int, String) -> Unit = { viewId, text ->
            views.setTextViewText(viewId, if (text.isEmpty()) "--" else text)
        }
        val setViewVisibility: (Int, Boolean) -> Unit = { viewId, isVisible ->
            views.setViewVisibility(viewId, if (isVisible) View.VISIBLE else View.GONE)
        }

        GlobalScope.launch {
            ouBenDevice.onRefresh(
                context = context,
                timeMillis = timeMillis,
                setViewText = setViewText,
                setViewVisibility = setViewVisibility,
                updateAppWidget = {appWidgetManager.updateAppWidget(appWidgetId, views)}
            )
        }
        GlobalScope.launch {
            newYingTengDevice.onRefresh(
                context = context,
                timeMillis = timeMillis,
                setViewText = setViewText,
                setViewVisibility = setViewVisibility,
                updateAppWidget = {appWidgetManager.updateAppWidget(appWidgetId, views)}
            )
        }
        GlobalScope.launch {
            oldYingTengDevice.onRefresh(
                context = context,
                timeMillis = timeMillis,
                setViewText = setViewText,
                setViewVisibility = setViewVisibility,
                updateAppWidget = {appWidgetManager.updateAppWidget(appWidgetId, views)}
            )
        }
        GlobalScope.launch {
            zhongxingDevice.onRefresh(
                context = context,
                timeMillis = timeMillis,
                setViewText = setViewText,
                setViewVisibility = setViewVisibility,
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
            val callback: () -> Unit = {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "切换成功", Toast.LENGTH_SHORT).show()
                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, 200L)
                }
            }
            ouBenDevice.onSwitchSim(callback = callback)
            newYingTengDevice.onSwitchSim(callback = callback)
            oldYingTengDevice.onSwitchSim(callback = callback)
        }
    }

    private fun registerRefreshEvent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, RealTime::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val requestCode = appWidgetId + (System.currentTimeMillis() / 1000).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun registerSwitchSIMEvent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, RealTime::class.java).apply {
            action = ACTION_SWITCH_SIM
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val requestCode = appWidgetId + (System.currentTimeMillis() / 1000).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}
