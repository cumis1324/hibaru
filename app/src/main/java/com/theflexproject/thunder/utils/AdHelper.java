package com.theflexproject.thunder.utils;

import android.content.Context;
import android.view.View;

import androidx.annotation.OptIn;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;

public class AdHelper {
    private static AdRequest adRequest;

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
}

