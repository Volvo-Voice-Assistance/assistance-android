package com.volvo.assistanceandroid

class Constants {
    companion object{
        // [23/05/23] [YSI] Define intent name for communication.
        const val ACTION_VOICE_ASSISTANT_REQUEST = "com.volvo.assistanceandroid.request"
        const val ACTION_VOICE_ASSISTANT_RESULT = "com.volvo.assistanceandroid.result"

        // [23/05/23] [YSI] Define extra name for communication. Other app will get the request by this id.
        const val REQUEST = "request"

        // [23/05/23] [YSI] Result intent should be returned from other apps when finish the request.
        const val FAIL = 0
        const val SUCCESS = 1
    }
}