package com.fittrack.app

import android.app.Application
import com.fittrack.app.billing.ProManager
import com.fittrack.app.di.AppModule

class FitTrackApp : Application() {
    lateinit var appModule: AppModule
        private set
    lateinit var proManager: ProManager
        private set

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
        proManager = ProManager(this)
        proManager.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        proManager.destroy()
    }
}
