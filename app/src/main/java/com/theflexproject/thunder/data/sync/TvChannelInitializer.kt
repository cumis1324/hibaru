package com.theflexproject.thunder.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class TvChannelInitializer : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "android.media.tv.action.INITIALIZE_PROGRAMS" || 
            action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON") {
            android.util.Log.d("TvChannelInitializer", "Triggering sync from $action...")
            val legacySync = OneTimeWorkRequestBuilder<TvChannelSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(legacySync)
            val engageSync = OneTimeWorkRequestBuilder<EngageSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(engageSync)
        }
    }
}
