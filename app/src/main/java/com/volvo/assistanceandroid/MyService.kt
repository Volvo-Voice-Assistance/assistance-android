package com.volvo.assistanceandroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import org.tensorflow.lite.support.label.Category
import java.util.*


enum class AppState {
    STOPPED, WAKE, STT
}

class MyService : Service(), TextToSpeech.OnInitListener {

    private val notificationId = 1234
    private val channelId = "VoiceAssistanceServiceChannel"
    private var currentState: AppState = AppState.STOPPED
    lateinit var bt: ImageButton
    private lateinit var wm: WindowManager
    private lateinit var mView: View
    private lateinit var speechRecognizer: SpeechRecognizer // SpeechRecognizer 객체를 선언
    private lateinit var tts: TextToSpeech
    private lateinit var recognizerIntent: Intent
    private lateinit var audioManager: AudioManager
    private lateinit var classifierHelper: TextClassificationHelper


    private val listener = object : TextClassificationHelper.TextResultsListener {

        override fun onResult(results: List<Category>) {
            // softmax 최고 확률 결과
            var action = Action.NONE
            val maxCategory = results.maxByOrNull {
                it.score
            }
            if (maxCategory != null && maxCategory.score >= 0.7) {
                // 가장 높은 정확도가 70프로 아래라면 NONE으로 레이블링
                action = Action.values()[maxCategory.label.toInt()]
            }

            //action 처리
            processResult(action)
        }

        override fun onError(error: String) {

        }
    }

    private fun processResult(action: Action) {
        action
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        initUi()
        initClassifier()
        initSTT()
        initTTS()
        createNotification()
        changeState()
    }

    private fun initClassifier() {
        classifierHelper = TextClassificationHelper(
            context = this,
            listener = listener
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VolvoTest", "서비스가 시작되었습니다.")
        return START_STICKY // 서비스가 강제 종료되면 재시작하도록 설정
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
            bt = it.findViewById<View>(R.id.bt) as ImageButton
            wm.addView(it, params)
        }
    }

    private fun changeState() {
        when (currentState) {
            AppState.STT -> {
                bt.setImageResource(R.drawable.img_volvoback_logo)
                bt.visibility = View.VISIBLE
            }
            AppState.WAKE -> {
                bt.setImageResource(R.drawable.img_volvoback_logo)
                bt.visibility = View.VISIBLE
            }
            else -> {
                bt.visibility = View.GONE
            }
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
                NotificationManager.IMPORTANCE_LOW
            )
        )
        notificationManager.notify(notificationId, builder.build())
        val notification = builder.build()
        startForeground(notificationId, notification)
    }


    private fun initSTT() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager




        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 100000L)
        }



        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                Log.d("SpeechToTextService", "음성 인식 준비 완료")
            }

            override fun onBeginningOfSpeech() {
//                audioManager.abandonAudioFocusRequest(audioFocusRequest)

            }

            override fun onRmsChanged(rmsdB: Float) {
                // 음성의 크기가 변할 때 호출
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 녹음된 음성 데이터의 버퍼가 준비되면 호출
            }

            override fun onEndOfSpeech() {
                Log.d("SpeechToTextService", "음성 인식 종료")
//                audioManager.requestAudioFocus(audioFocusRequest)
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> displayError("Error recording audio.")
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> displayError("Insufficient permissions.")
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_NETWORK -> displayError(
                        "Network Error."
                    )
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        currentState = AppState.STOPPED
                        changeState()
                        playback(0)
                        return
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        return

                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> displayError("Recognition service is busy.")
                    SpeechRecognizer.ERROR_SERVER -> displayError("Server Error.")
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> displayError("No speech input.")
                    else -> displayError("Something wrong occurred.")
                }
                // 오류가 발생하면 다시 음성 인식을 시작
                speechRecognizer.stopListening()
            }


            override fun onResults(results: Bundle?) {
                // 음성 인식 결과를 받으면 호출, 결과는 문자열의 배열로 제공, 첫 번째 요소가 가장 정확도가 높은 결과
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val text = matches[0] // 가장 정확도가 높은 결과를 가져옴
                    Log.d("SpeechToTextService", "음성 인식 결과: $text")
                    // 음성 인식 결과를 사용하는 코드를 작성.
                    if (text.contains("볼보")) {
                        currentState = AppState.STT
                        changeState()
                        speakOut("네?")
                    } else if (currentState == AppState.STT) {
                        text.contains("라이트")
                        speakOut("네 라이트를 켜드릴게요")
                        currentState = AppState.STOPPED
                        changeState()
                    }
                }
                // 다시 음성 인식을 시작
                playback(1000)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // 음성 인식 중간 결과를 받으면 호출
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 음성 인식 관련 이벤트가 발생하면 호출됩니다. 필요에 따라 사용
            }
        })

        speechRecognizer.startListening(recognizerIntent)

    }

    private fun initTTS() {
        tts = TextToSpeech(this, this)

    }

    private fun speakOut(speakText: String) {
        speechRecognizer.stopListening()
        tts.setPitch(0.8.toFloat())
        tts.setSpeechRate(1.0.toFloat())
        tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "id1")

    }

    // 일정 시간 후에 동작
    private fun playback(milliSeconds: Int) {
        speechRecognizer.stopListening()
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentState != AppState.STOPPED) {
                currentState = AppState.STT
                changeState()
            }
            speechRecognizer.startListening(recognizerIntent)
        }, milliSeconds.toLong())
    }

    private fun displayError(message: String) {
        Log.d("SpeechToTextService", message)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
        if (::wm.isInitialized && ::mView.isInitialized) { // 변수가 초기화되었는지 확인
            wm.removeView(mView)
        }
        tts.stop()
        tts.shutdown();
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.KOREA)
            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e("TTS", "This Language is not supported")
            }
        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

}
