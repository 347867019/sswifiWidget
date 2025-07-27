package com.oxjmo.sswifiwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp {
                AppNavHost()
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
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "webview?url={url}&script={script}",
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("script") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val script = backStackEntry.arguments?.getString("script") ?: ""
            WebViewScreen(url = url, script = script, navController = navController)
        }
    }
}