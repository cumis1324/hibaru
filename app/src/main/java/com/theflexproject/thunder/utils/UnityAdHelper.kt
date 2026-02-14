package com.theflexproject.thunder.utils

import android.app.Activity
import android.widget.FrameLayout
import com.unity3d.ads.*
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

object UnityAdHelper {
    private const val UNITY_GAME_ID = "5742136" // Your Unity Game ID
    private const val TEST_MODE = true
    private const val TAG = "UnityAdHelper"
    
    private const val REWARDED_PLACEMENT = "Iklan_Reward"
    private const val INTERSTITIAL_PLACEMENT = "Iklan_Inter"
    private const val BANNER_PLACEMENT = "Iklan_Banner"

    interface AdCallback {
        fun onAdComplete()
        fun onAdFailed()
    }

    fun init(activity: Activity) {
        android.util.Log.d(TAG, "Initializing Unity Ads SDK...")
        UnityAds.initialize(activity.applicationContext, UNITY_GAME_ID, TEST_MODE, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                android.util.Log.d(TAG, "Unity Ads Initialization Complete")
            }
            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                android.util.Log.e(TAG, "Unity Ads Initialization Failed: $message")
            }
        })
    }

    fun showRewardedAd(activity: Activity, callback: AdCallback) {
        UnityAds.show(activity, REWARDED_PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                callback.onAdFailed()
            }
            override fun onUnityAdsShowStart(placementId: String?) {}
            override fun onUnityAdsShowClick(placementId: String?) {}
            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    callback.onAdComplete()
                } else {
                    callback.onAdFailed()
                }
            }
        })
    }

    fun loadBanner(activity: Activity, container: FrameLayout) {
        val bannerView = BannerView(activity, BANNER_PLACEMENT, UnityBannerSize(320, 50))
        bannerView.listener = object : BannerView.IListener {
            override fun onBannerLoaded(bannerView: BannerView?) {
                container.removeAllViews()
                container.addView(bannerView)
            }
            override fun onBannerFailedToLoad(bannerView: BannerView?, error: BannerErrorInfo?) {}
            override fun onBannerClick(bannerView: BannerView?) {}
            override fun onBannerShown(bannerView: BannerView?) {}
            override fun onBannerLeftApplication(bannerView: BannerView?) {}
        }
        bannerView.load()
    }
}
