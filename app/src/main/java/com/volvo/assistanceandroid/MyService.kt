package com.volvo.assistanceandroid

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


class MyService : Service() {
    private val notificationId = 1234
    private val channelId = "VoiceAssistanceServiceChannel"
    private lateinit var wm: WindowManager
    private lateinit var mView: View


    private lateinit var speechRecognizer: SpeechRecognizer // SpeechRecognizer 객체를 선언합니다.
    private lateinit var recognizerIntent: Intent // RecognizerIntent 객체를 선언합니다.

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        initUi()
        initSTT()
        createNotification()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VolvoTest", "서비스가 시작되었습니다.")
        speechRecognizer.startListening(recognizerIntent)
        return START_STICKY // 서비스가 강제 종료되면 재시작하도록 설정합니다.
    }

    private fun initUi() {
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
                bt.setImageResource(R.drawable.img_volvoback_logo)
            }
            wm.addView(it, params)
        }
    }


    private fun createNotification() {
        val builder = NotificationCompat.Builder(this, channelId)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("VoiceAssistance Service")
        builder.setContentText("서비스 실행 중")
        builder.color = Color.RED
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent) // 알림 클릭 시 이동

        // 알림 표시
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "기본 채널",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        notificationManager.notify(notificationId, builder.build()) // id : 정의해야하는 각 알림의 고유한 int값
        val notification = builder.build()
        startForeground(notificationId, notification)
    }


    private fun initSTT(){
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechToTextService", "음성 인식 준비 완료")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechToTextService", "음성 인식 시작")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 음성의 크기가 변할 때 호출됩니다. 필요에 따라 사용하세요.
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 녹음된 음성 데이터의 버퍼가 준비되면 호출됩니다. 필요에 따라 사용하세요.
            }

            override fun onEndOfSpeech() {
                Log.d("SpeechToTextService", "음성 인식 종료")
            }

            override fun onError(error: Int) {
                Log.d("SpeechToTextService", "음성 인식 오류: $error")
                // 오류가 발생하면 다시 음성 인식을 시작합니다.
                speechRecognizer.startListening(recognizerIntent)
            }

            override fun onResults(results: Bundle?) {
                // 음성 인식 결과를 받으면 호출, 결과는 문자열의 배열로 제공, 첫 번째 요소가 가장 정확도가 높은 결과
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val text = matches[0] // 가장 정확도가 높은 결과를 가져옴
                    Log.d("SpeechToTextService", "음성 인식 결과: $text")
                    // 음성 인식 결과를 사용하는 코드를 작성하세요. 예를 들어, 텍스트를 다른 앱에 전달하거나, 특정 명령어에 따라 작업을 수행하거나, 텍스트를 음성으로 변환하거나 등등...
                }
                // 음성 인식을 계속 수행하려면 다시 음성 인식을 시작합니다.
                speechRecognizer.startListening(recognizerIntent)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // 음성 인식 중간 결과를 받으면 호출됩니다. 필요에 따라 사용하세요.
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 음성 인식 관련 이벤트가 발생하면 호출됩니다. 필요에 따라 사용하세요.
            }
        })
    }

    // 음성 인식 서비스의 상태를 확인하는 메소드

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
        if (::wm.isInitialized && ::mView.isInitialized) { // 변수가 초기화되었는지 확인
            wm.removeView(mView)
        }
    }

}
