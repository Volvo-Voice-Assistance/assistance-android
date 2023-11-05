package com.volvo.assistanceandroid

import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category
import java.util.*


enum class AppState {
    STOPPED, WAKEWORD, STT, SAYING
}


class MyService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACCESS_KEY = "a12I6AIwSdf15uFkv+2M7993Bv5QUrtUCG0vDzR4G02LpTIB1Quh3g==" // Picovoice AccessKey
        const val CHANNEL_ID = "VoiceAssistanceServiceChannel"
        val KEYWORD_PATHS = arrayOf(
            "volvo_en_android_v2_2_0.ppn",
            "hey-ball-bo_en_android_v2_2_0.ppn",
            "hey-boll-bo_en_android_v2_2_0.ppn"
        )
        val SENSITIVITIES = floatArrayOf(
            0.7f, 0.7f, 0.7f
        )
    }

    private lateinit var porcupineManager: PorcupineManager
    private val notificationId = 1234
    private var currentState: AppState = AppState.STOPPED
    private lateinit var bt: ImageButton
    private lateinit var wm: WindowManager
    private lateinit var mView: View
    private lateinit var speechRecognizer: SpeechRecognizer // SpeechRecognizer 객체를 선언
    private lateinit var tts: TextToSpeech
    private lateinit var recognizerIntent: Intent
    private lateinit var classifierHelper: TextClassificationHelper

    private val classifierListener = object : TextClassificationHelper.TextResultsListener {

        override fun onResult(results: List<Category>) {
            // softmax 최고 확률 결과
            var action = Action.NONE

            val maxCategory = results.maxByOrNull {
                it.score
            }

            Log.d("SpeechToTextService", "${maxCategory?.label}")

            if (maxCategory != null && maxCategory.score >= 0.7) {
                // 가장 높은 정확도가 70프로 아래라면 NONE으로 레이블링
                action = Action.values()[maxCategory.label.toInt()]
            }

            //action 처리
            Log.d("SpeechToTextService", "$action")
            processResult(action)
        }

        override fun onError(error: String) {

        }
    }

    private fun sendRequest(action: Action) {
        val i = Intent()
        i.action = Constants.ACTION_VOICE_ASSISTANT_REQUEST
        i.putExtra(Constants.REQUEST, action.label)
        applicationContext.sendBroadcast(i)
        Log.d("DBG", "[sendRequest] Action.label : " + action.label)
    }

    override fun onCreate() {
        super.onCreate()
        initView()
        initWakeWordDetection()
        initSTT()
        initTTS()
        initClassifier()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()
        CoroutineScope(Dispatchers.Main).launch {
            playback(0)
        }
        return START_STICKY // 서비스가 강제 종료되면 재시작하도록 설정
    }

    private fun initWakeWordDetection() {
        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPaths(KEYWORD_PATHS)
                .setSensitivities(SENSITIVITIES)
                .build(applicationContext) {
                    Log.d("PORCUPINE", "Detection")
                    if (!tts.isSpeaking) {
                        speakOut("yes?")
                        try {
                            porcupineManager.stop()
                        } catch (e: PorcupineException) {
                            displayError("Failed to stop Porcupine.")
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            changeStateUi(AppState.STT)
                            speechRecognizer.startListening(recognizerIntent)
                        }
                    }
                }
        } catch (e: PorcupineException) {
            Log.e("PORCUPINE_SERVICE", e.toString())
        }
    }

    /** TODO 텍스트 의도 분석 후 이후 작업 처라**/
    private fun processResult(action: Action) {
        if (action != Action.NONE) sendRequest(action)
        speakOut(action.answer)
        CoroutineScope(Dispatchers.Main).launch {
            playback(0)
        }
    }


    /** 화면에 띄울 View를 초기화하는 함수 **/
    private fun initView() {
        val inflate = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        wm = getSystemService(WINDOW_SERVICE) as WindowManager // wm 변수를 초기화
        val params = WindowManager.LayoutParams(
            400,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER or Gravity.BOTTOM
        mView = inflate.inflate(R.layout.view_in_service, null) // mView 변수를 초기화
        mView.let {
            bt = it.findViewById<View>(R.id.bt) as ImageButton
            wm.addView(it, params)
        }
        CoroutineScope(Dispatchers.Main).launch {
            changeStateUi(AppState.WAKEWORD)
        }
    }

    /** 텍스트 분류를 위한 Helper Class 를 초기화 하는 함수 **/
    private fun initClassifier() {
        classifierHelper = TextClassificationHelper(
            context = this,
            listener = classifierListener
        )
    }

    private fun changeStateUi(appState: AppState) {
        currentState = appState
        when (currentState) {
            AppState.STT -> {
                bt.setImageResource(R.drawable.img_volvo_logo)
                bt.visibility = View.VISIBLE
            }
            AppState.SAYING -> {
                bt.setImageResource(R.drawable.img_volvoback_logo)
            }
            else -> {
                bt.visibility = View.GONE
            }
        }
    }


    /** ForeGround Notification 을 생성하는 함수 **/
    private fun createNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("VoiceAssistance Service")
        builder.setContentText("서비스 실행 중")
        builder.priority = NotificationCompat.PRIORITY_MAX
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
                CHANNEL_ID,
                "기본 채널",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        notificationManager.notify(notificationId, builder.build())
        val notification = builder.build()
        startForeground(notificationId, notification)
    }


    /**  SPEECH-TO-TEXT를 초기화 하는 함수 TODO audioManager 삭제 고려 **/
    private fun initSTT() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                60000
            )
        }


        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechToTextService", "음성 인식 준비 완료")
            }

            override fun onBeginningOfSpeech() {
                CoroutineScope(Dispatchers.Main).launch {
                    changeStateUi(AppState.SAYING)
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 녹음된 음성 데이터의 버퍼가 준비되면 호출
            }

            override fun onEndOfSpeech() {
                Log.d("SpeechToTextService", "음성 인식 종료")

            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> displayError("Error recording audio.")
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> displayError("Insufficient permissions.")
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_NETWORK -> displayError(
                        "Network Error."
                    )
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        displayError("No recognition result matched.");
                        CoroutineScope(Dispatchers.Main).launch {
                            playback(0)
                        }
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {}
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> displayError("Recognition service is busy.")
                    SpeechRecognizer.ERROR_SERVER -> displayError("Server Error.")
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> displayError("No speech input.")
                    else -> displayError("Something wrong occurred.")
                }
            }


            override fun onResults(results: Bundle?) {
                // 음성 인식 결과를 받으면 호출, 결과는 문자열의 배열로 제공, 첫 번째 요소가 가장 정확도가 높은 결과
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val speechText = matches[0] // 가장 정확도가 높은 결과를 가져옴
                    Log.d("SpeechToTextService", "음성 인식 결과: $speechText")
                    // 명령어 분석
                    classifierHelper.classify(speechText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // 음성 인식 중간 결과를 받으면 호출
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 음성 인식 관련 이벤트가 발생하면 호출됩니다. 필요에 따라 사용
            }
        })
    }


    /** Text-To-Speech 를 초기화 하는 함수 **/
    private fun initTTS() {
        tts = TextToSpeech(this, this)
    }

    /** 입력받은 Text를 Speech 하는 함수 **/
    private fun speakOut(speakText: String) {
        Log.d("speech", speakText)
        tts.setPitch(0.8.toFloat())
        tts.setSpeechRate(1.0.toFloat())
        tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "id1")
    }

    /** 일정 시간 후에 다시 WAKEWORD 상태로 전환하는 함수**/
    private suspend fun playback(milliSeconds: Int) {
        Log.d("speech", "playback")
        porcupineManager.stop()
        speechRecognizer.stopListening()
        delay(milliSeconds.toLong())
        changeStateUi(AppState.WAKEWORD)
        porcupineManager.start()
    }

    /** error 내용을 출력하는 함수 **/
    private fun displayError(message: String) {
        Log.e("SpeechToTextService", message)
    }


    /** service를 중단하는 함수 (리소스 해제) **/
    private fun stopService() {
        Log.d("EndService", "endService")
        try {
            porcupineManager.stop()
            porcupineManager.delete()
        } catch (e: PorcupineException) {
            displayError("Failed to stop porcupine.")
        }
        if (::wm.isInitialized && ::mView.isInitialized) {
            wm.removeView(mView)
        }
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
        currentState = AppState.STOPPED
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("stopservice", "stopservice")
        stopService()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e("TTS", "This Language is not supported")
            }
        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

}
