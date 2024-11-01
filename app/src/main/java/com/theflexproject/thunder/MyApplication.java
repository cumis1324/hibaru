package com.theflexproject.thunder;

import android.app.Application;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingImpressionListener;
import com.google.firebase.inappmessaging.model.Action;
import com.google.firebase.inappmessaging.model.InAppMessage;

import java.util.Date;

public class MyApplication extends Application {

    private static AppOpenAd appOpenAd = null;
    private static long loadTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        MobileAds.initialize(this);
        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Initialize Firebase Analytics
        FirebaseAnalytics.getInstance(this);

        // Enable In-App Messaging
    }


}
