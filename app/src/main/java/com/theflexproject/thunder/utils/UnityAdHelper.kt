package com.theflexproject.thunder.utils

import android.app.Activity
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

    interface AdCallback {
        fun onAdComplete()
        fun onAdFailed()
    }

    fun init(activity: Activity) {
        if (UnityAds.isInitialized) {
            android.util.Log.d(TAG, "Unity Ads SDK is already initialized")
            return
        }
        
        android.util.Log.d(TAG, "Initializing Unity Ads SDK with Activity context...")
        UnityAds.initialize(activity, UNITY_GAME_ID, TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                android.util.Log.d(TAG, "Unity Ads Initialization Complete")
                loadRewardedAd() // Preload on init
            }
            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                android.util.Log.e(TAG, "Unity Ads Initialization Failed: [Error: $error] $message")
            }
        })
    }

    fun loadRewardedAd() {
        android.util.Log.d(TAG, "Loading Rewarded Ad...")
        UnityAds.load(REWARDED_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                android.util.Log.d(TAG, "Rewarded Ad Loaded: $placementId")
            }
            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                android.util.Log.e(TAG, "Rewarded Ad Failed to Load: $message")
            }
        })
    }

    fun showRewardedAd(activity: Activity, callback: AdCallback) {
        android.util.Log.d(TAG, "Showing Rewarded Ad...")
        UnityAds.show(activity, REWARDED_PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                android.util.Log.e(TAG, "Rewarded Ad Show Failure: $message")
                callback.onAdFailed()
                loadRewardedAd() // Retry load
            }
            override fun onUnityAdsShowStart(placementId: String?) {
                android.util.Log.d(TAG, "Rewarded Ad Show Start")
            }
            override fun onUnityAdsShowClick(placementId: String?) {}
            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                android.util.Log.d(TAG, "Rewarded Ad Show Complete: $state")
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    callback.onAdComplete()
                } else {
                    callback.onAdFailed()
                }
                loadRewardedAd() // Preload next
            }
        })
    }

    fun loadBanner(activity: Activity, container: FrameLayout) {
        android.util.Log.d(TAG, "Loading Banner...")
        val bannerView = BannerView(activity, BANNER_PLACEMENT, UnityBannerSize(320, 50))
        bannerView.listener = object : BannerView.IListener {
            override fun onBannerLoaded(bannerView: BannerView?) {
                container.removeAllViews()
                container.addView(bannerView)
                container.visibility = android.view.View.VISIBLE
                android.util.Log.d(TAG, "Banner Loaded")
            }
            override fun onBannerFailedToLoad(bannerView: BannerView?, error: BannerErrorInfo?) {
                android.util.Log.e(TAG, "Banner Failed to Load: ${error?.errorMessage}")
                container.visibility = android.view.View.GONE
            }
            override fun onBannerClick(bannerView: BannerView?) {}
            override fun onBannerShown(bannerView: BannerView?) {}
            override fun onBannerLeftApplication(bannerView: BannerView?) {}
        }
        bannerView.load()
    }
}
