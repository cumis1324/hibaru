package com.theflexproject.thunder.data.sync

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SyncPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    var lastSyncTime: Long
        get() = prefs.getLong("last_sync_time", 0)
        set(value) = prefs.edit().putLong("last_sync_time", value).apply()

    var isSyncEnabled: Boolean
        get() = prefs.getBoolean("is_sync_enabled", true)
        set(value) = prefs.edit().putBoolean("is_sync_enabled", value).apply()

    var isDemoMode: Boolean
        get() = prefs.getBoolean("is_demo_mode", false)
        set(value) = prefs.edit().putBoolean("is_demo_mode", value).apply()

    var trendingItemsJson: String?
        get() = prefs.getString("trending_items_json", null)
        set(value) = prefs.edit().putString("trending_items_json", value).apply()

    var cachedGenresJson: String?
        get() = prefs.getString("cached_genres_json", null)
        set(value) = prefs.edit().putString("cached_genres_json", value).apply()
}
