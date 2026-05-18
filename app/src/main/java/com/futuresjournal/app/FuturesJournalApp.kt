package com.futuresjournal.app

import android.app.Application
import com.futuresjournal.app.api.ApiClient

class FuturesJournalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
