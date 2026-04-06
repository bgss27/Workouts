package com.fittrack.app

import android.app.Application
import com.fittrack.app.di.AppModule

class FitTrackApp : Application() {
    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
    }
}
