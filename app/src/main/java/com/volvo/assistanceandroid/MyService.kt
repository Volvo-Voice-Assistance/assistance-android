package com.volvo.assistanceandroid

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView


class MyService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var mView: View
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val inflate = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        wm = getSystemService(WINDOW_SERVICE) as WindowManager // wm 변수를 초기화
        val params = WindowManager.LayoutParams(
            400,
            ViewGroup.LayoutParams.WRAP_CONTENT,

            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER or Gravity.BOTTOM
        mView = inflate.inflate(R.layout.view_in_service, null) // mView 변수를 초기화
        mView.let {

            val bt = it.findViewById<View>(R.id.bt) as ImageButton
            bt.setOnClickListener {
                bt.setImageState(intArrayOf(android.R.attr.state_pressed), true) // 버튼의 상태에 따라 이미지를 변경합니다.
            }
            wm.addView(it, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wm.isInitialized && ::mView.isInitialized) { // 변수가 초기화되었는지 확인
            wm.removeView(mView)
        }
    }
}
