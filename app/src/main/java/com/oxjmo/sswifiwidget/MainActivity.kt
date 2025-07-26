package com.oxjmo.sswifiwidget

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScreen() {
    val context = LocalContext.current
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val tabs = listOf("欧本", "新影腾", "老影腾", "中兴")
    var selectedTabIndex by remember { mutableStateOf(0) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // 使用 ModalDrawerSheet 来应用默认背景色和样式
            ModalDrawerSheet(
                modifier = Modifier.width(240.dp) // 控制宽度
            ) {
                Column {
                    NavigationDrawerItem(
                        label = { Text("帮助") },
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "我也帮不了你", Toast.LENGTH_SHORT).show()
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("源码") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } }
                    )
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("WiFi 小控件") },
                        actions = {
                            IconButton(onClick = {
                                Toast.makeText(context, "请返回桌面添加安卓小控件", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Info, contentDescription = "更多")
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                        }
                    )
                },
                content = { padding ->
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)) {
                        TabRow(selectedTabIndex = selectedTabIndex, tabs = {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    text = { Text(title) },
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index }
                                )
                            }
                        })

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (selectedTabIndex) {
                                0 -> OubanConfig()
                                1 -> NewYingTengConfig()
                                2 -> OldYingTengConfig()
                                3 -> ZhongxingConfig()
                            }
                        }

                        Text(
                            text = "version：${pInfo.versionName}",
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                    }
                }
            )
        }
    )
}

@Composable
fun OubanConfig() {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("com.oxjmo.sswifiwidget.storage", Context.MODE_PRIVATE)
    }
    var accountId by remember {
        mutableStateOf(sharedPreferences.getString("oubenAccountId", "") ?: "")
    }
    var switchSimHidden = remember {
        mutableStateOf(
            sharedPreferences.getBoolean("oubenSwitchSimHidden", false)
        )
    }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "账号ID", modifier = Modifier.weight(1f))
            TextField(
                value = accountId,
                onValueChange = {
                    accountId = it
                    sharedPreferences.edit {
                        putString("oubenAccountId", it)
                    }
                },
                modifier = Modifier.weight(2f)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "隐藏SIM开关", modifier = Modifier.weight(1f))
            Switch(
                checked = switchSimHidden.value,
                onCheckedChange = {
                    switchSimHidden.value = it
                    sharedPreferences.edit {
                        putBoolean("oubenSwitchSimHidden", it)
                    }
                }
            )
        }
    }
}

@Composable
fun NewYingTengConfig() {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("com.oxjmo.sswifiwidget.storage", Context.MODE_PRIVATE)
    }
    var switchSimHidden = remember {
        mutableStateOf(sharedPreferences.getBoolean("newYingTengSwitchSimHidden", false))
    }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "隐藏SIM开关", modifier = Modifier.weight(1f))
            Switch(
                checked = switchSimHidden.value,
                onCheckedChange = {
                    switchSimHidden.value = it
                    sharedPreferences.edit {
                        putBoolean("newYingTengSwitchSimHidden", it)
                    }
                }
            )
        }
    }
}

@Composable
fun OldYingTengConfig() {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("com.oxjmo.sswifiwidget.storage", Context.MODE_PRIVATE)
    }
    var switchSimHidden = remember {
        mutableStateOf(sharedPreferences.getBoolean("oldYingTengSwitchSimHidden", false))
    }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "隐藏SIM开关", modifier = Modifier.weight(1f))
            Switch(
                checked = switchSimHidden.value,
                onCheckedChange = {
                    switchSimHidden.value = it
                    sharedPreferences.edit {
                        putBoolean("oldYingTengSwitchSimHidden", it)
                    }
                }
            )
        }
    }
}

@Composable
fun ZhongxingConfig() {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("com.oxjmo.sswifiwidget.storage", Context.MODE_PRIVATE)
    }
    var switchSimHidden = remember {
        mutableStateOf(sharedPreferences.getBoolean("zhongXingSwitchSimHidden", false))
    }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "隐藏SIM开关", modifier = Modifier.weight(1f))
            Switch(
                checked = switchSimHidden.value,
                onCheckedChange = {
                    switchSimHidden.value = it
                    sharedPreferences.edit {
                        putBoolean("zhongXingSwitchSimHidden", it)
                    }
                }
            )
        }
    }
}
