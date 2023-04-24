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


class MyService() : Service() {
    var wm: WindowManager? = null
    var mView: View? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val inflate = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            400,
            ViewGroup.LayoutParams.WRAP_CONTENT,

            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER or Gravity.BOTTOM
        mView = inflate.inflate(R.layout.view_in_service, null)
        mView?.let {

            val bt = it.findViewById<View>(R.id.bt) as ImageButton
            bt.setOnClickListener {
                bt.setImageResource(R.drawable.volvoback)

            }
            wm?.addView(it, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wm != null) {
            if (mView != null) {
                wm!!.removeView(mView)
                mView = null
            }
            wm = null
        }
    }
}