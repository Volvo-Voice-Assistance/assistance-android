package com.volvo.assistanceandroid


import android.Manifest
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
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.volvo.assistanceandroid.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { permission ->
                when {
                    permission.value -> {
                        checkPermission()
                    }
                    shouldShowRequestPermissionRationale(permission.key) -> {
                        Toast.makeText(this, "음성 인식을 위해 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    private lateinit var binding: ActivityMainBinding
    private val requirePermission = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.btStart.setOnClickListener { requestPermissionLauncher.launch(requirePermission) }
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

    /**
     * 현재 앱이 다른 앱 위에 그릴 수 있는 권한을 확인하고 요청하는 함수입니다.
     **/
    private fun checkPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            someActivityResultLauncher.launch(intent)
        } else {
            startService(Intent(this@MainActivity, MyService::class.java))
        }
    }


}