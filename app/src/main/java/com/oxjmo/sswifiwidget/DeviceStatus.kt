package com.oxjmo.sswifiwidget

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatus(sharedPreferences: SharedPreferences) {
    var refreshTrigger by remember { mutableStateOf(0) }
    var switchTrigger by remember { mutableStateOf(0) }
    val ouBenDevice = OuBenDevice()
    val newYingTengDevice = NewYingTengDevice()
    val oldYingTengDevice = OldYingTengDevice()
    val zhongxingDevice = ZhongxingDevice()
    var ip by remember { mutableStateOf("") }
    var rssi by remember { mutableStateOf("") }
    var electricQuantity by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var networkMode by remember { mutableStateOf("") }
    var utilizableFlow by remember { mutableStateOf("") }
    val setViewText: (Int, String) -> Unit = { viewId, text ->
        when (viewId) {
            R.id.ip -> ip = text
            R.id.rssi -> rssi = text
            R.id.electricQuantity -> electricQuantity = text
            R.id.provider -> provider = text
            R.id.networkMode -> networkMode = text
            R.id.utilizableFlow -> utilizableFlow = text
        }
    }
    val setViewVisibility: (Int, Boolean) -> Unit = { viewId, isVisible -> }
    val updateAppWidget: () -> Unit = {}

    LaunchedEffect(refreshTrigger) {
        ip = "--"
        rssi = "--"
        electricQuantity = "--"
        provider = "--"
        networkMode = "--"
        utilizableFlow = "--"
        try {
            GlobalScope.launch {
                ouBenDevice.onRefresh(
                    timeMillis = 200L,
                    sharedPreferences = sharedPreferences,
                    setViewText = setViewText,
                    setViewVisibility = setViewVisibility,
                    updateAppWidget = updateAppWidget
                )
            }
            GlobalScope.launch {
                newYingTengDevice.onRefresh(
                    timeMillis = 200L,
                    sharedPreferences = sharedPreferences,
                    setViewText = setViewText,
                    setViewVisibility = setViewVisibility,
                    updateAppWidget = updateAppWidget
                )
            }
            GlobalScope.launch  {
                oldYingTengDevice.onRefresh(
                    timeMillis = 200L,
                    sharedPreferences = sharedPreferences,
                    setViewText = setViewText,
                    setViewVisibility = setViewVisibility,
                    updateAppWidget = updateAppWidget
                )
            }
            GlobalScope.launch  {
                zhongxingDevice.onRefresh(
                    timeMillis = 200L,
                    sharedPreferences = sharedPreferences,
                    setViewText = setViewText,
                    setViewVisibility = setViewVisibility,
                    updateAppWidget = updateAppWidget
                )
            }
        } catch (_: Exception) {}
    }
    LaunchedEffect(switchTrigger) {
        try {
            withContext(Dispatchers.IO) {
                newYingTengDevice.onSwitchSim({ refreshTrigger++ })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    Box(
        modifier = Modifier.padding(16.dp)
    ) {
        Divider()
        Row(
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("ip: $ip", style = MaterialTheme.typography.titleMedium)
                Text("信号: $rssi", style = MaterialTheme.typography.titleMedium)
                Text("电量: $electricQuantity", style = MaterialTheme.typography.titleMedium)
                Text("运营商: $provider", style = MaterialTheme.typography.titleMedium)
                Text("模式: $networkMode", style = MaterialTheme.typography.titleMedium)
                Text("剩余流量: $utilizableFlow", style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                Button(onClick = { refreshTrigger++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("刷新")
                }
                Button(onClick = { switchTrigger++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("切换sim卡")
                }
            }
        }
    }
}