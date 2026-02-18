package com.hse.visualriskassessor

import android.app.Application

class HSEApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: HSEApplication
            private set
    }
}
