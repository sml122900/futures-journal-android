package com.futuresjournal.app.util

import android.util.Log

object Logger {
    private const val TAG = "FuturesJournal"

    fun error(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun debug(message: String) {
        Log.d(TAG, message)
    }
}
