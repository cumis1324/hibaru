package com.theflexproject.thunder.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.theflexproject.thunder.player.PlayerActivity;

public class AdHelper {
    private static AdRequest adRequest;
    private static RewardedAd rewardedAd;

    // Method untuk mendapatkan AdRequest
    public static AdRequest getAdRequest(Context context) {
        if (adRequest == null) {
            adRequest = new AdRequest.Builder().build();
        }
        return adRequest;
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void loadNative(Context mActivity, AdRequest request, TemplateView template){
        if (request == null) {
            Log.e("NativeAd", "AdRequest is null. Make sure it is initialized properly.");
            if (template != null) {
                template.setVisibility(View.GONE);
            }
            return;
        }

        AdLoader adLoader = new AdLoader.Builder(mActivity, "ca-app-pub-7142401354409440/7261340471")
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        NativeTemplateStyle styles = new NativeTemplateStyle.Builder().build();
                        template.setStyles(styles);
                        template.setNativeAd(nativeAd);
                        template.setVisibility(View.VISIBLE);
                    }
                })
                .withAdListener(new AdListener() {
                    @OptIn(markerClass = UnstableApi.class)
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        super.onAdFailedToLoad(adError);
                        Log.e("NativeAd", "Failed to load ad: " + adError.getMessage());
                        if (template != null) {
                            template.setVisibility(View.GONE);
                        }
                    }
                })
                .build();

        adLoader.loadAd(request);
    }
    public static void loadReward(Context mCtx, Activity activity, Player player, PlayerView playerView, AdRequest request){
        if (request != null) {
            RewardedAd.load(mCtx, "ca-app-pub-7142401354409440/7652952632",
                    request, new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            rewardedAd = null;
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedAd = ad;

                            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdClicked() {
                                    // Called when a click is recorded for an ad.

                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {

                                    rewardedAd = null;
                                    if (player != null) {
                                        player.setPlayWhenReady(true);
                                    }
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    // Called when ad fails to show.

                                    rewardedAd = null;
                                }

                                @Override
                                public void onAdImpression() {
                                    // Called when an impression is recorded for an ad.

                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    // Called when ad is shown.

                                    if (player != null) {
                                        player.setPlayWhenReady(false);
                                    }
                                }
                            });
                            if (rewardedAd != null) {
                                if (playerView != null) {
                                    playerView.onPause();
                                }
                                rewardedAd.show(activity, new OnUserEarnedRewardListener() {
                                    @Override
                                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                        // Handle the reward.

                                        int rewardAmount = rewardItem.getAmount();
                                        String rewardType = rewardItem.getType();

                                    }
                                });
                            }
                        }

                    });
        }
    }
}

