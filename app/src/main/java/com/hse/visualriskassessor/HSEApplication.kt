package com.hse.visualriskassessor

import android.app.Application
import com.hse.visualriskassessor.data.database.AppDatabase
import com.hse.visualriskassessor.data.repository.AssessmentRepository

class HSEApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val assessmentRepository: AssessmentRepository by lazy {
        AssessmentRepository(database.assessmentDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: HSEApplication
            private set
    }
}
