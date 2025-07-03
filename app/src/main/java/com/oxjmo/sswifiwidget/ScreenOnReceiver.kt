package com.oxjmo.sswifiwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenOnReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 检查是否是用户回到桌面的动作
        if (intent.action == Intent.ACTION_USER_PRESENT || intent.action == Intent.ACTION_SCREEN_ON) {
            println("回到首页")
        }
    }
}