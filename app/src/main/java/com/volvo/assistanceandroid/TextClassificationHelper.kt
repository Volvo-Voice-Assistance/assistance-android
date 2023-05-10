package com.volvo.assistanceandroid

import android.content.Context

class TextClassificationHelper(
    val context: Context,
    val listener: TextResultsListener
) {

    interface TextResultsListener {
        fun onError(error: String)
        fun onResult(results: List<Category>, inferenceTime: Long)
    }



}