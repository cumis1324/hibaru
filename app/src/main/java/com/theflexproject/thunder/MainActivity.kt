package com.theflexproject.thunder

import android.app.UiModeManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.theflexproject.thunder.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    interface OnUserLeaveHintListener {
        fun onUserLeaveHint()
    }

    private var onUserLeaveHintListener: OnUserLeaveHintListener? = null

    fun setOnUserLeaveHintListener(listener: OnUserLeaveHintListener?) {
        this.onUserLeaveHintListener = listener
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        onUserLeaveHintListener?.onUserLeaveHint()
    }

    companion object {
        @JvmField
        var historyList: MutableList<String> = mutableListOf()
        @JvmField
        var favoritList: MutableList<String> = mutableListOf()
        @JvmField
        var historyAll: MutableList<String> = mutableListOf()
        
    }

    private var phoneBinding: ActivityMainBinding? = null
    private lateinit var navController: NavController
    private var blurView: View? = null
    private var navigationView: View? = null // Can be BottomNavigationView or NavigationRailView
    private val isTVDevice: Boolean by lazy {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        val isTelevision = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasNoTouch = !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        isTelevision || hasLeanback || hasNoTouch
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Hide system bars completely for true edge-to-edge
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController!!.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        if (isTVDevice) {
            // TV layout with Top Navigation
            setContentView(R.layout.main_tv)
            navigationView = findViewById(R.id.top_navigation)
        } else {
            // Phone layout with BottomNavigationView
            phoneBinding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(phoneBinding!!.root)
            blurView = phoneBinding!!.blurView
            navigationView = phoneBinding!!.bottomNavigation
        }

        // Handle Window Insets for Bottom Navigation (phone only)
        // TV layout doesn't have blurView at all
        if (!isTVDevice && blurView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(blurView!!) { v, insets ->
                v.setPadding(0, 0, 0, 0)
                insets
            }
        }

        // Find nav fragment (ID differs between layouts)
        val navHostFragment = if (isTVDevice) {
            supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        } else {
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        }
        navController = navHostFragment.navController

        // Setup Navigation (works for both BottomNavigationView and NavigationRailView)
        when (navigationView) {
            is BottomNavigationView -> (navigationView as BottomNavigationView).setupWithNavController(navController)
            is NavigationRailView -> (navigationView as NavigationRailView).setupWithNavController(navController)
        }

        // Visibility logic
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.playerFragment, R.id.detailFragment, R.id.tvShowDetailsFragment -> {
                    blurView?.visibility = View.GONE
                    navigationView?.visibility = View.GONE
                }
                else -> {
                    blurView?.visibility = View.VISIBLE
                    navigationView?.visibility = View.VISIBLE
                }
            }
        }

        // Handle incoming deep link
        handleDeepLink(intent)

        // Check permissions
        checkPermissions()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "nfgplus" && data.host == "video") {
            val videoIdStr = data.lastPathSegment ?: return
            val videoId = videoIdStr.toIntOrNull() ?: return
            val isMovie = data.getQueryParameter("isMovie")?.toBoolean() ?: true
            
            navController.navigate(R.id.playerFragment, Bundle().apply {
                putInt("videoId", videoId)
                putBoolean("isMovie", isMovie)
            })
        } else if (data.scheme == "https" && data.path?.contains("reviews.html") == true) {
            val itemId = data.getQueryParameter("id")?.toIntOrNull() ?: return
            val itemType = data.getQueryParameter("type")
            
            android.util.Log.d("DeepLink", "Web Link: id=$itemId, type=$itemType")
            
            if (itemType == "movie") {
                navController.navigate(R.id.playerFragment, Bundle().apply {
                    putInt("videoId", itemId)
                    putBoolean("isMovie", true)
                })
            } else {
                // Navigate to modern DetailScreen for TV shows or other types
                navController.navigate(R.id.detailFragment, Bundle().apply {
                    putInt("tv_show_id", itemId)
                })
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun checkPermissions() {
        if (!isNotificationPermissionGranted()) {
            showNotificationPermissionDialog();
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun showNotificationPermissionDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Notification Permission")
            .setMessage("To receive updates and alerts, please allow notification access in the settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}

