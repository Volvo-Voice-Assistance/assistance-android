package com.volvo.assistanceandroid

//ui용 import

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts


//ui용 import

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setContentView(R.layout.activity_main)

        val bt_start = findViewById<View>(R.id.bt_start) as Button
        bt_start.setOnClickListener { checkPermission() }

        val bt_stop = findViewById<View>(R.id.bt_stop) as Button
        bt_stop.setOnClickListener { stopService(Intent(this@MainActivity, MyService::class.java)) }

    }


    private val someActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                // TODO: 결과 처리
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this@MainActivity, MyService::class.java))
                } else {
                    // TODO: 동의를 얻지 못했을 경우의 처리
                }
            }
        }

    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 체크
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                someActivityResultLauncher.launch(intent)
            } else {
                startService(Intent(this@MainActivity, MyService::class.java))
            }
        } else {
            startService(Intent(this@MainActivity, MyService::class.java))
        }
    }

}