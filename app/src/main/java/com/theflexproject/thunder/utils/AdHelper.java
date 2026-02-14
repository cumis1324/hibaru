package com.theflexproject.thunder.utils;

import android.app.Activity;
import android.content.Context;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import com.theflexproject.thunder.utils.UnityAdHelper;

/**
 * Legacy AdHelper refactored to bridge to Unity Ads.
 * AdMob dependencies have been removed.
 */
public class AdHelper {

    public interface AdCallback {
        void onAdComplete();

        void onAdFailed();
    }

    public static void loadReward(Context mCtx, Activity activity, Player player, PlayerView playerView) {
        // Bridge to Unity Rewarded Ad
        UnityAdHelper.INSTANCE.showRewardedAd(activity, new UnityAdHelper.AdCallback() {
            @Override
            public void onAdComplete() {
                if (player != null) {
                    player.setPlayWhenReady(true);
                }
            }

            @Override
            public void onAdFailed() {
                // If ad fails, we might still want to play or show error
                if (player != null) {
                    player.setPlayWhenReady(true); // Fallback to play anyway or handle as you wish
                }
            }
        });
    }

    // Stub for native which is now handled directly via Unity Banner containers in
    // layouts
    public static void loadNative(Context mActivity, Object request, android.widget.FrameLayout template) {
        if (mActivity instanceof Activity) {
            UnityAdHelper.INSTANCE.loadBanner((Activity) mActivity, template);
        }
    }

    // Stub for getting request
    public static Object getAdRequest(Context context) {
        return new Object();
    }
}
