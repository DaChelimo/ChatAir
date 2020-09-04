package com.example.messenger

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import timber.log.Timber

class MainApplicationLifecycleObserver: LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun changeModeToOfflineInOnDestroy(){
        changeUserActivityToOffline()
        Timber.d("Observer onDestroy called")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun changeModeToOfflineOnStop(){
        changeUserActivityToOffline()
        Timber.d("Observer onStop called")
    }

}