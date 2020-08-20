package com.example.messenger

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import timber.log.Timber

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        mainContext = applicationContext
        Timber.plant(Timber.DebugTree())

        ProcessLifecycleOwner.get().lifecycle.addObserver(MainApplicationLifecycleObserver())
        changeUserActivityToOnline()
    }

}