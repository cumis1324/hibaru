package com.theflexproject.thunder

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.theflexproject.thunder.data.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SyncActivity : AppCompatActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
