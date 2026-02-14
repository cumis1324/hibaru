package com.theflexproject.thunder.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;

/**
 * AdHelper - Unified ad management using Unity Ads
 * Replaces all AdMob functionality
 */
public class AdHelper {
    private static final String TAG = "AdHelper";

    /**
     * Load and show rewarded ad using Unity Ads
     * Pauses playback during ad display
     */
    public static void loadReward(Context mCtx, Activity activity, Player player, PlayerView playerView) {
        Log.d(TAG, "Loading rewarded ad via Unity Ads");

        if (player != null) {
            player.setPlayWhenReady(false);
        }

        // Load the rewarded ad first
        UnityAdHelper.INSTANCE.loadRewardedAd(activity);

        // Then show it with callback
        UnityAdHelper.INSTANCE.showRewardedAd(activity, new UnityAdHelper.AdCallback() {
            @Override
            public void onAdComplete() {
                Log.d(TAG, "Reward ad completed successfully");
                if (player != null) {
                    player.setPlayWhenReady(true);
                }
                if (playerView != null) {
                    playerView.onResume();
                }
            }

            @Override
            public void onAdFailed() {
                Log.e(TAG, "Reward ad failed to show");
                if (player != null) {
                    player.setPlayWhenReady(true);
                }
                if (playerView != null) {
                    playerView.onResume();
                }
            }
        });
    }

    /**
     * Deprecated AdMob methods - replaced by Unity Ads
     * Kept as empty stubs for backward compatibility
     */
    @Deprecated
    public static void loadNative(Context mActivity, Object request, Object template) {
        Log.d(TAG, "loadNative is deprecated - using Unity Ads instead");
    }

    @Deprecated
    public static Object getAdRequest(Context context) {
        Log.d(TAG, "getAdRequest is deprecated - not needed for Unity Ads");
        return null;
    }
}

