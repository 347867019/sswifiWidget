package com.oxjmo.sswifiwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp {
                TabScreen()
            }
        }
    }
}

@Composable
fun MyApp(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Composable
fun TabScreen() {
    val tabs = listOf("欧本", "老影腾", "新影腾")
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex, tabs = {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        })

        // 根据当前选中的 Tab 显示不同内容
        when (selectedTabIndex) {
            0 -> OubanConfig()
            1 -> Text(text = "老影腾 的配置内容")
            2 -> Text(text = "新影腾 的配置内容")
        }
    }
}

@Composable
fun OubanConfig() {
    var enableData by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "流量开关", modifier = Modifier.weight(1f))
            Switch(
                checked = enableData,
                onCheckedChange = { enableData = it }
            )
        }
    }
}