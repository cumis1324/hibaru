package com.theflexproject.thunder.utils;

import android.animation.ObjectAnimator;
import android.view.View;
import android.widget.TextView;

import com.google.android.ads.nativetemplates.TemplateView;

public class ColapsingTitle {
    public static void collapseTitle(TextView homeTitle) {
        ObjectAnimator animation = ObjectAnimator.ofFloat(homeTitle, "translationY", 0, -homeTitle.getHeight());
        animation.setDuration(300);
        animation.start();
        homeTitle.setVisibility(View.GONE); // Sembunyikan setelah animasi selesai
    }

    // Animasi untuk menampilkan searchTitle
    public static void expandTitle(TextView homeTitle) {
        homeTitle.setVisibility(View.VISIBLE); // Tampilkan sebelum animasi mulai
        ObjectAnimator animation = ObjectAnimator.ofFloat(homeTitle, "translationY", -homeTitle.getHeight(), 0);
        animation.setDuration(300);
        animation.start();
    }
}
