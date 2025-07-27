package com.oxjmo.sswifiwidget

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val tabs = listOf("欧本", "新影腾", "老影腾", "中兴")
    var selectedTabIndex by remember { mutableStateOf(0) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
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
                                0 -> OubanConfig(navController)
                                1 -> NewYingTengConfig(navController)
                                2 -> OldYingTengConfig(navController)
                                3 -> ZhongxingConfig(navController)
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
fun OubanConfig(navController: NavController) {
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
fun NewYingTengConfig(navController: NavController) {
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
                                .type_items a {display: table-cell!important;}
                                .nav_right ul li {display: block!important;}
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
fun OldYingTengConfig(navController: NavController) {
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
fun ZhongxingConfig(navController: NavController) {
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