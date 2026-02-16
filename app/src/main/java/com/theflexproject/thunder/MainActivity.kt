package com.theflexproject.thunder

import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        if (isTVDevice) {
            // TV layout with NavigationRailView
            setContentView(R.layout.main_tv)
            blurView = findViewById(R.id.blurView)
            navigationView = findViewById(R.id.side_navigation)
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
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
