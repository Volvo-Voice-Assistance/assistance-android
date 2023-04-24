package com.volvo.assistanceandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("android.intent.action.BOOT_COMPLETED" == intent.action) {
            Log.d("VolvoTest", "부팅 시작되었습니다.")
            val tempService = Intent(context, MyService::class.java)
            context.startForegroundService(tempService)
        }
    }
}