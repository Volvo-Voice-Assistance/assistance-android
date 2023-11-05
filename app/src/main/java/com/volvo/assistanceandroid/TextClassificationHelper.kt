package com.volvo.assistanceandroid

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier

class TextClassificationHelper(
    private val context: Context,
    private val listener: TextResultsListener
) {

    companion object {
        const val MODEL_NAME = "model.tflite"
    }

    private lateinit var bertClassifier: BertNLClassifier


    init {
        initClassifier()
    }

    /** 텍스트 분류 모델을 초기화하는 함수  **/
    private fun initClassifier() {
        val baseOptionsBuilder = BaseOptions.builder()
        val baseOptions = baseOptionsBuilder.build()

        val options = BertNLClassifier.BertNLClassifierOptions
            .builder()
            .setBaseOptions(baseOptions)
            .build()

        bertClassifier = BertNLClassifier.createFromFileAndOptions(
            context,
            MODEL_NAME,
            options
        )
    }

    /** 텍스트를 분류하는 함수 **/
    fun classify(text: String) {
        CoroutineScope(Dispatchers.Default).launch {
            // 코루틴 컨텍스트 내에서 분류를 수행합니다.
            // CPU 집약적인 작업인 경우, Dispatchers.Default를 그대로 사용합니다.
            val results: List<Category> = withContext(Dispatchers.Default) {
                bertClassifier.classify(text)
            }
            // 메인 스레드로 결과를 전달합니다.
            withContext(Dispatchers.Main) {
                listener.onResult(results)
            }
        }
    }


    interface TextResultsListener {
        fun onError(error: String)
        fun onResult(results: List<Category>)
    }

}