package com.oxjmo.sswifiwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetClass = RealTime::class.java
    val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, appWidgetClass))

    val sharedPreferences = remember {
        context.getSharedPreferences("com.oxjmo.sswifiwidget.storage", Context.MODE_PRIVATE)
    }
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val tabs = listOf("欧本", "新影腾", "老影腾", "中兴")
    var selectedTabIndex by remember {
        mutableStateOf(sharedPreferences.getInt("selectedTabIndex", 0))
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var widgetFontSize by remember {
        mutableStateOf(sharedPreferences.getString("widgetFontSize", "小") ?: "")
    }
    var widgetOrientation by remember {
        mutableStateOf(sharedPreferences.getString("widgetOrientation", "纵向") ?: "")
    }
    var translucent by remember {
        mutableStateOf(sharedPreferences.getString("translucent", "否") ?: "")
    }
    var visibleDeviceStatus by remember {
        mutableStateOf(sharedPreferences.getString("visibleDeviceStatus", "隐藏") ?: "隐藏")
    }
    var showEditFontSizeDialog by remember { mutableStateOf(false) }
    var showEditOrientationDialog by remember { mutableStateOf(false) }
    var showEditTranslucentDialog by remember { mutableStateOf(false) }
    var showEditVisibleDeviceStatus by remember { mutableStateOf(false) }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(240.dp) // 控制宽度
            ) {
                Column {
                    NavigationDrawerItem(
                        label = {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "控件字体大小")
                                Text(
                                    text = widgetFontSize,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            showEditFontSizeDialog = true
                        }
                    )
                    NavigationDrawerItem(
                        label = {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "控件方向")
                                Text(
                                    text = widgetOrientation,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            showEditOrientationDialog = true
                        }
                    )
                    NavigationDrawerItem(
                        label = {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "透明度")
                                Text(
                                    text = translucent,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            showEditTranslucentDialog = true
                        }
                    )
                    NavigationDrawerItem(
                        label = {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "首页设备状态")
                                Text(
                                    text = visibleDeviceStatus,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            showEditVisibleDeviceStatus = true
                        }
                    )
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
                        onClick = {
                            val encodedUrl = Uri.encode("https://github.com/347867019/sswifiWidget")
                            navController.navigate("webview?url=$encodedUrl")
                            scope.launch { drawerState.close() }
                        }
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
                                Toast.makeText(context, "请长按桌面 → 小部件 → 找到【随时wifi控件】→ 添加小部件", Toast.LENGTH_SHORT).show()
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
                        .background(MaterialTheme.colorScheme.background)
                        .padding(padding)) {
                        TabRow(selectedTabIndex = selectedTabIndex, tabs = {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    text = { Text(title) },
                                    selected = selectedTabIndex == index,
                                    onClick = {
                                        selectedTabIndex = index
                                        sharedPreferences.edit {
                                            putInt("selectedTabIndex", index)
                                        }
                                    }
                                )
                            }
                        })

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (selectedTabIndex) {
                                0 -> OubanConfig(navController, sharedPreferences)
                                1 -> NewYingTengConfig(navController, sharedPreferences)
                                2 -> OldYingTengConfig(navController, sharedPreferences)
                                3 -> ZhongxingConfig(navController, sharedPreferences)
                            }
                        }

                        if(visibleDeviceStatus == "显示") {DeviceStatus(sharedPreferences)}
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
    if (showEditFontSizeDialog) {
        AlertDialog(
            onDismissRequest = { showEditFontSizeDialog = false },
            title = { Text("选择字体大小") },
            text = {
                Column {
                    listOf("小", "中", "大").forEach { value ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    widgetFontSize = value
                                    showEditFontSizeDialog = false
                                    sharedPreferences.edit {
                                        putString("widgetFontSize", value)
                                    }
                                    RealTime().onUpdate(context, appWidgetManager, appWidgetIds)
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = value)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    if (showEditOrientationDialog) {
        AlertDialog(
            onDismissRequest = { showEditOrientationDialog = false },
            title = { Text("选择控件方向") },
            text = {
                Column {
                    listOf("纵向", "横向").forEach { value ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    widgetOrientation = value
                                    showEditOrientationDialog = false
                                    sharedPreferences.edit {
                                        putString("widgetOrientation", value)
                                    }
                                    RealTime().onUpdate(context, appWidgetManager, appWidgetIds)
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = value)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    if (showEditTranslucentDialog) {
        AlertDialog(
            onDismissRequest = { showEditTranslucentDialog = false },
            title = { Text("选择透明度") },
            text = {
                Column {
                    listOf("否", "10%", "30%", "50%", "75%", "90%").forEach { value ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    translucent = value
                                    showEditTranslucentDialog = false
                                    sharedPreferences.edit {
                                        putString("translucent", value)
                                    }
                                    RealTime().onUpdate(context, appWidgetManager, appWidgetIds)
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = value)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    if (showEditVisibleDeviceStatus) {
        AlertDialog(
            onDismissRequest = { showEditVisibleDeviceStatus = false },
            title = { Text("选择是否显示拖拽设备状态") },
            text = {
                Column {
                    listOf("显示", "隐藏").forEach { value ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    visibleDeviceStatus = value
                                    showEditVisibleDeviceStatus = false
                                    sharedPreferences.edit {
                                        putString("visibleDeviceStatus", value)
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = value)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun OubanConfig(navController: NavController, sharedPreferences: SharedPreferences) {
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
            Text(text = "充值号", modifier = Modifier.weight(1f))
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
fun NewYingTengConfig(navController: NavController, sharedPreferences: SharedPreferences) {
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "全功能后台", modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val encodedUrl = Uri.encode("http://192.168.100.1/index.html")
                    val enCodeScript = Uri.encode("""
                        {
                            const style = document.createElement("style")
                            style.innerHTML = `
                                .type_items a {display: table-cell!important}
                                .nav_right ul li {display: block!important}
                                .traffic_control_container {display: inline!important}
                            `
                            document.body.appendChild(style)
                            setTimeout(_ => {
                                if(document.getElementById("frmLogin")) {
                                    const inputElement = document.getElementById('txtAdmin');
                                    inputElement.value = 'admin';
                                    const keypressevent = new Event('keypress', {bubbles: true,cancelable: true});
                                    inputElement.dispatchEvent(keypressevent);
                                    document.getElementById('btnLogin')?.click()
                                }
                            }, 500)
                        }
                    """.trimIndent())
                    navController.navigate("webview?url=$encodedUrl&script=$enCodeScript")
                },
                modifier = Modifier
            ) {
                Text(text = "查看")
            }
        }
    }
}

@Composable
fun OldYingTengConfig(navController: NavController, sharedPreferences: SharedPreferences) {
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
fun ZhongxingConfig(navController: NavController, sharedPreferences: SharedPreferences) {
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "全功能后台", modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val encodedUrl = Uri.encode("http://192.168.0.1/index.html")
                    val enCodeScript = Uri.encode("""
                        {
                            const style = document.createElement("style")
                            style.innerHTML = `
                                .type_items ul li {display: inline !important}
                                .nav_right ul li {display: inline!important}
                                .traffic_control_container {display: inline!important}
                            `
                            document.body.appendChild(style)
                            setTimeout(_ => {
                                if(document.getElementById("frmLogin")) {
                                    const inputElement = document.getElementById('txtPwd');
                                    inputElement.value = 'admin';
                                    const keypressevent = new Event('keypress', {bubbles: true,cancelable: true});
                                    inputElement.dispatchEvent(keypressevent);
                                    document.getElementById('btnLogin')?.click()
                                }
                            }, 500)
                        }
                    """.trimIndent())
                    navController.navigate("webview?url=$encodedUrl&script=$enCodeScript")
                },
                modifier = Modifier
            ) {
                Text(text = "查看")
            }
        }
    }
}