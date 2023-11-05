package com.volvo.assistanceandroid

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier
import java.util.concurrent.ScheduledThreadPoolExecutor

class TextClassificationHelper(
    private val context: Context,
    private val listener: TextResultsListener
) {

    companion object {
        const val MODEL_NAME = "model.tflite"
    }

    private lateinit var bertClassifier: BertNLClassifier
    private lateinit var executor: ScheduledThreadPoolExecutor


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
            executor = ScheduledThreadPoolExecutor(1)

            executor.execute {
                // Use the appropriate classifier based on the selected model
                val results: List<Category> = bertClassifier.classify(text)
                listener.onResult(results)
            }
        }
    }

    interface TextResultsListener {
        fun onError(error: String)
        fun onResult(results: List<Category>)
    }

}