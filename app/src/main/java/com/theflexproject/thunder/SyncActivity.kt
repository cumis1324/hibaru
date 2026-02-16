package com.theflexproject.thunder

import android.app.UiModeManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.theflexproject.thunder.data.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SyncActivity : AppCompatActivity() {

    @Inject
    lateinit var syncManager: SyncManager
    
    private val isTVDevice: Boolean by lazy {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        val isTelevision = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasNoTouch = !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        isTelevision || hasLeanback || hasNoTouch
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isTVDevice) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        // Enable Edge-to-Edge (only for phones, not TV)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Hide system bars for phone devices only
        if (!isTVDevice) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController?.apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        
        setContentView(R.layout.activity_loading) // Reusing existing loader layout

        val statusText = findViewById<TextView>(R.id.loading_message) ?: TextView(this)
        val progressBar = findViewById<ProgressBar>(R.id.progress_datar)

        statusText.text = "Syncing with cloud..."

        lifecycleScope.launch {
            try {
                syncManager.syncAll()
                statusText.text = "Sync Complete!"
                navigateToMain()
            } catch (e: Exception) {
                statusText.text = "Sync Error: ${e.message}"
                // Proceed anyway after short delay? or Retry?
                // For now, proceed.
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // Pass any deep link data if present
        if (intent.data != null) {
            intent.data = this.intent.data
        }
        startActivity(intent)
        finish()
    }
}
