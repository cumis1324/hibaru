package com.theflexproject.thunder.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class TvChannelInitializer : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("TvChannelInitializer", "onReceive: action=${intent.action}")
        if (intent.action == "android.media.tv.action.INITIALIZE_PROGRAMS") {
            android.util.Log.d("TvChannelInitializer", "Triggering initial sync (Legacy + Engage)...")
            
            val legacySync = OneTimeWorkRequestBuilder<TvChannelSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(legacySync)
            
            val engageSync = OneTimeWorkRequestBuilder<EngageSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(engageSync)
        }
    }
}
