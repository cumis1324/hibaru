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
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject

@AndroidEntryPoint
class SyncActivity : AppCompatActivity() {

    @Inject
    lateinit var syncManager: SyncManager
    
    private val isTVDevice: Boolean by lazy {
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTelevision = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val packageManager = packageManager
        val hasLeanback = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        val hasNoTouch = !packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TOUCHSCREEN)
        isTelevision || hasLeanback || hasNoTouch
    }

    private lateinit var progressCircular: android.widget.ProgressBar
    private lateinit var statusText: android.widget.TextView
    private lateinit var btnRetry: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isTVDevice) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        // Enable Edge-to-Edge (only for phones, not TV)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Hide system bars for phone devices only
        if (!isTVDevice) {
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController?.apply {
                hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        
        setContentView(R.layout.activity_loading)

        progressCircular = findViewById<android.widget.ProgressBar>(R.id.progress_circular)
        statusText = findViewById<android.widget.TextView>(R.id.loading_message)
        btnRetry = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_retry)

        subscribeToFcmTopic()
        
        val appVersionText = findViewById<android.widget.TextView>(R.id.app_version)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            appVersionText?.text = "v${pInfo.versionName}"
        } catch (e: Exception) {
            appVersionText?.text = ""
        }

        btnRetry.setOnClickListener {
            performSync()
        }
        performSync()

    }

    private fun performSync() {
        // Reset UI to loading state
        progressCircular.setVisibility(android.view.View.VISIBLE)
        statusText.setVisibility(android.view.View.GONE)
        btnRetry.setVisibility(android.view.View.GONE)

        if (!isNetworkAvailable()) {
            showError("Tidak ada koneksi internet. Periksa jaringan Anda.")
            return
        }

        lifecycleScope.launch {
            try {
                syncManager.syncAll()
                navigateToMain()
            } catch (e: Exception) {
                showError("Gagal menyinkronkan data: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        progressCircular.setVisibility(android.view.View.GONE)
        statusText.text = message
        statusText.setVisibility(android.view.View.VISIBLE)
        btnRetry.setVisibility(android.view.View.VISIBLE)
        btnRetry.requestFocus() // Ensure D-pad focus on button for TV
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun navigateToMain() {
        val nextIntent = Intent(this, MainActivity::class.java)
        if (this.intent.data != null) {
            nextIntent.data = this.intent.data
        }
        startActivity(nextIntent)
        finish()
    }

    private fun subscribeToFcmTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("latest_update")
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) "Successfully subscribed to topic" else "Failed to subscribe to topic"
                android.util.Log.d("SyncActivity", msg)
            }
    }
}
