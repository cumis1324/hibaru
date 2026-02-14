package com.theflexproject.thunder

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.theflexproject.thunder.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
// import eightbitlab.com.blurview.RenderScriptBlur // Check if this import is needed or valid

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

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup Bottom Navigation
        binding.bottomNavigation.setupWithNavController(navController)

        // Setup BlurView (Optional/TODO: Verify library version and setup)
        /*
        val radius = 10f
        val decorView = window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground = decorView.background

        binding.blurView.setupWith(rootView)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
        */

        // Visibility logic
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.playerFragment, R.id.detailFragment -> {
                    binding.blurView.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.blurView.visibility = View.VISIBLE
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
