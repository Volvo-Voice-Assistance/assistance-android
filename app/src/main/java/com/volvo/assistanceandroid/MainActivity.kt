package com.volvo.assistanceandroid


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.volvo.assistanceandroid.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkPermission()
            } else {
                Toast.makeText(this, "권한을 받아오지 못했습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.btStart.setOnClickListener { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        binding.btStop.setOnClickListener {
            stopService(
                Intent(
                    this@MainActivity,
                    MyService::class.java
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
    }

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this@MainActivity, MyService::class.java))
            } else {
                Toast.makeText(this, "권한이 없어 앱을 종료합니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /** 현재 앱이 다른 앱 위에 그릴 수 있는 권한을 확인하고 요청하는 함수입니다. **/
    private fun checkPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            activityResultLauncher.launch(intent)
        } else {
            startService(Intent(this@MainActivity, MyService::class.java))
        }
    }

}