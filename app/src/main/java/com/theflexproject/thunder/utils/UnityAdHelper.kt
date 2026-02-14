package com.theflexproject.thunder.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import com.unity3d.ads.*
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

object UnityAdHelper {
    private const val UNITY_GAME_ID = "5928984" // Your Unity Game ID
    private const val TEST_MODE = true
    private const val TAG = "UnityAdHelper"

    private const val REWARDED_PLACEMENT = "Iklan_Reward"
    private const val BANNER_PLACEMENT = "Iklan_Banner"

    private var initializationAttempts = 0
    private const val MAX_INIT_ATTEMPTS = 5
    private var isInitializing = false
    private var isInitialized = false
    private var adsDisabled = false // Disable ads if too many failures
    private var initializationHandler: Handler? = null
    private val pendingInitCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var consecutiveInternalErrors = 0
    private const val MAX_INTERNAL_ERROR_THRESHOLD = 3

    interface AdCallback {
        fun onAdComplete()
        fun onAdFailed()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun init(activity: Activity, callback: ((Boolean) -> Unit)? = null) {
        if (callback != null) {
            pendingInitCallbacks.add(callback)
        }

        // If ads are disabled due to repeated failures, don't try again
        if (adsDisabled) {
            android.util.Log.d(TAG, "Unity Ads is permanently disabled due to repeated initialization failures")
            pendingInitCallbacks.forEach { it(false) }
            pendingInitCallbacks.clear()
            return
        }

        // If already initialized, call callbacks immediately
        if (isInitialized) {
            android.util.Log.d(TAG, "Unity Ads already initialized successfully")
            pendingInitCallbacks.forEach { it(true) }
            pendingInitCallbacks.clear()
            return
        }

        if (isInitializing) {
            android.util.Log.d(TAG, "Unity Ads initialization already in progress, queuing callback")
            return
        }

        if (initializationAttempts >= MAX_INIT_ATTEMPTS) {
            android.util.Log.e(TAG, "Max initialization attempts ($MAX_INIT_ATTEMPTS) reached. Giving up.")
            adsDisabled = true
            pendingInitCallbacks.forEach { it(false) }
            pendingInitCallbacks.clear()
            return
        }

        // Check network availability
        if (!isNetworkAvailable(activity)) {
            android.util.Log.w(TAG, "Network is not available. Scheduling retry in 3 seconds...")
            if (initializationHandler == null) {
                initializationHandler = Handler(Looper.getMainLooper())
            }
            initializationHandler?.postDelayed({
                init(activity)
            }, 3000)
            return
        }

        isInitializing = true
        initializationAttempts++

        val delayMs = when (initializationAttempts) {
            1 -> 0L
            2 -> 3000L
            3 -> 7000L
            4 -> 12000L
            else -> 15000L
        }

        android.util.Log.d(TAG, "Initializing Unity Ads SDK (Attempt $initializationAttempts/$MAX_INIT_ATTEMPTS)${if (delayMs > 0) " after ${delayMs}ms delay" else ""}... [Consecutive internal errors: $consecutiveInternalErrors]")

        if (initializationHandler == null) {
            initializationHandler = Handler(Looper.getMainLooper())
        }

        initializationHandler?.postDelayed({
            try {
                android.util.Log.d(TAG, "Calling UnityAds.initialize() with Game ID: $UNITY_GAME_ID, Test Mode: $TEST_MODE")
                val initListener = object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        android.util.Log.d(TAG, "✓ Unity Ads Initialization SUCCESSFUL on attempt $initializationAttempts")
                        isInitializing = false
                        isInitialized = true
                        initializationAttempts = 0
                        consecutiveInternalErrors = 0

                        // Call all pending callbacks with success
                        pendingInitCallbacks.forEach { it(true) }
                        pendingInitCallbacks.clear()
                    }
                    override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                        android.util.Log.e(TAG, "✗ Unity Ads Initialization FAILED (Attempt $initializationAttempts/$MAX_INIT_ATTEMPTS)")
                        android.util.Log.e(TAG, "  Error Type: $error")
                        android.util.Log.e(TAG, "  Error Message: $message")
                        isInitializing = false

                        if (error == UnityAds.UnityAdsInitializationError.INTERNAL_ERROR) {
                            consecutiveInternalErrors++
                            android.util.Log.e(TAG, "  ⚠ INTERNAL_ERROR detected (Count: $consecutiveInternalErrors/$MAX_INTERNAL_ERROR_THRESHOLD)")

                            // If we get too many consecutive internal errors, disable ads
                            if (consecutiveInternalErrors >= MAX_INTERNAL_ERROR_THRESHOLD) {
                                android.util.Log.e(TAG, "  ✗ Too many INTERNAL_ERRORs. Disabling ads permanently.")
                                adsDisabled = true
                                pendingInitCallbacks.forEach { it(false) }
                                pendingInitCallbacks.clear()
                                return
                            }
                        } else {
                            consecutiveInternalErrors = 0 // Reset counter for non-internal errors
                        }

                        if (initializationAttempts < MAX_INIT_ATTEMPTS) {
                            val nextDelayMs = when {
                                error == UnityAds.UnityAdsInitializationError.INTERNAL_ERROR -> 10000L + (initializationAttempts * 3000L)
                                else -> 5000L + (initializationAttempts * 1500L)
                            }
                            android.util.Log.d(TAG, "  → Scheduling retry in ${nextDelayMs}ms...")
                            initializationHandler?.postDelayed({
                                init(activity)
                            }, nextDelayMs)
                        } else {
                            android.util.Log.e(TAG, "  ✗ Max attempts reached. Disabling ads.")
                            adsDisabled = true
                            pendingInitCallbacks.forEach { it(false) }
                            pendingInitCallbacks.clear()
                        }
                    }
                }
                UnityAds.initialize(activity, UNITY_GAME_ID, TEST_MODE, initListener)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "✗ Exception during Unity Ads initialization", e)
                android.util.Log.e(TAG, "  Exception Type: ${e.javaClass.simpleName}")
                android.util.Log.e(TAG, "  Exception Message: ${e.message}")
                isInitializing = false

                if (initializationAttempts < MAX_INIT_ATTEMPTS) {
                    val nextDelayMs = 8000L + (initializationAttempts * 2000L)
                    android.util.Log.d(TAG, "  → Scheduling retry after exception in ${nextDelayMs}ms...")
                    initializationHandler?.postDelayed({
                        init(activity)
                    }, nextDelayMs)
                } else {
                    android.util.Log.e(TAG, "  ✗ Max attempts reached. Disabling ads.")
                    adsDisabled = true
                    pendingInitCallbacks.forEach { it(false) }
                    pendingInitCallbacks.clear()
                }
            }
        }, delayMs)
    }

    fun loadRewardedAd(activity: Activity) {
        if (adsDisabled || !isInitialized) {
            android.util.Log.w(TAG, "Cannot load rewarded ad - Ads disabled: $adsDisabled, Initialized: $isInitialized")
            return
        }

        try {
            android.util.Log.d(TAG, "Loading rewarded ad with placement: $REWARDED_PLACEMENT")
            UnityAds.load(REWARDED_PLACEMENT, UnityAdsLoadOptions(), object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    android.util.Log.d(TAG, "✓ Rewarded ad loaded successfully")
                }
                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                    android.util.Log.e(TAG, "✗ Rewarded ad failed to load - Error: $error, Message: $message")
                }
            })
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception while loading rewarded ad: ${e.message}", e)
        }
    }

    fun showRewardedAd(activity: Activity, callback: AdCallback) {
        if (adsDisabled || !isInitialized) {
            android.util.Log.w(TAG, "Cannot show rewarded ad - Ads disabled: $adsDisabled, Initialized: $isInitialized")
            callback.onAdFailed()
            return
        }

        try {
            android.util.Log.d(TAG, "Showing rewarded ad with placement: $REWARDED_PLACEMENT")
            UnityAds.show(activity, REWARDED_PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                    android.util.Log.e(TAG, "Rewarded ad show failed - Placement: $placementId, Error: $error, Message: $message")
                    callback.onAdFailed()
                }
                override fun onUnityAdsShowStart(placementId: String?) {
                    android.util.Log.d(TAG, "Rewarded ad started")
                }
                override fun onUnityAdsShowClick(placementId: String?) {
                    android.util.Log.d(TAG, "Rewarded ad clicked")
                }
                override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                    android.util.Log.d(TAG, "Rewarded ad completed - State: $state")
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        callback.onAdComplete()
                    } else {
                        callback.onAdFailed()
                    }
                }
            })
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception while showing rewarded ad: ${e.message}", e)
            callback.onAdFailed()
        }
    }

    fun loadBanner(activity: Activity, container: FrameLayout) {
        if (adsDisabled || !isInitialized) {
            android.util.Log.w(TAG, "Skipping banner load - Ads disabled: $adsDisabled, Initialized: $isInitialized")
            return
        }

        try {
            android.util.Log.d(TAG, "Loading banner with placement: $BANNER_PLACEMENT")
            val bannerView = BannerView(activity, BANNER_PLACEMENT, UnityBannerSize(320, 50))
            bannerView.listener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerView: BannerView?) {
                    android.util.Log.d(TAG, "✓ Banner loaded successfully")
                    container.removeAllViews()
                    container.addView(bannerView)
                }
                override fun onBannerFailedToLoad(bannerView: BannerView?, error: BannerErrorInfo?) {
                    android.util.Log.e(TAG, "✗ Banner failed to load: $error")
                }
                override fun onBannerClick(bannerView: BannerView?) {
                    android.util.Log.d(TAG, "Banner clicked")
                }
                override fun onBannerShown(bannerView: BannerView?) {
                    android.util.Log.d(TAG, "Banner shown")
                }
                override fun onBannerLeftApplication(bannerView: BannerView?) {
                    android.util.Log.d(TAG, "User left application from banner")
                }
            }
            bannerView.load()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception while loading banner: ${e.message}", e)
        }
    }

    // Debug function to check current status
    fun getStatus(): String {
        return """
            Unity Ads Status:
            - Initialized: $isInitialized
            - Disabled: $adsDisabled
            - Currently Initializing: $isInitializing
            - Initialization Attempts: $initializationAttempts/$MAX_INIT_ATTEMPTS
            - Consecutive Internal Errors: $consecutiveInternalErrors/$MAX_INTERNAL_ERROR_THRESHOLD
        """.trimIndent()
    }
}
