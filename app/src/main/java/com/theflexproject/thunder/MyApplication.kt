package com.theflexproject.thunder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.theflexproject.thunder.data.sync.TvChannelSyncWorker
import com.theflexproject.thunder.data.sync.EngageSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {
    
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    companion object {
        private lateinit var instance: MyApplication

        @JvmStatic
        fun getContext(): android.content.Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase Analytics
        FirebaseAnalytics.getInstance(this)

        scheduleTvSync()
    }

    private fun scheduleTvSync() {
        val workManager = WorkManager.getInstance(this)
        
        // 1. Legacy TV Provider Sync
        val legacyRequest = PeriodicWorkRequestBuilder<TvChannelSyncWorker>(24, TimeUnit.HOURS)
            .addTag("tv_sync")
            .build()
        workManager.enqueueUniquePeriodicWork(
            "tv_channel_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            legacyRequest
        )

        // 2. Google Engage SDK Sync
        val engageRequest = PeriodicWorkRequestBuilder<EngageSyncWorker>(24, TimeUnit.HOURS)
            .addTag("engage_sync")
            .build()
        workManager.enqueueUniquePeriodicWork(
            "engage_sdk_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            engageRequest
        )
    }
}
