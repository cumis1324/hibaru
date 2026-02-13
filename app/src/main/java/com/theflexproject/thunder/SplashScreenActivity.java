package com.theflexproject.thunder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.theflexproject.thunder.model.FirebaseManager;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {
    FirebaseManager firebaseManager;
    static FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent().getData();
        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        firebaseManager = new FirebaseManager();
        currentUser = firebaseManager.getCurrentUser();

        HandlerCompat.postDelayed(
                new android.os.Handler(Looper.getMainLooper()),
                () -> {
                    currentUser = firebaseManager.getCurrentUser();
                    handleSignIn(data);
                },
                null,
                4000 // Add null for the optional token argument
        );
    }

    private void handleSignIn(Uri data) {
        Intent intent;
        if (currentUser != null) {
            intent = new Intent(SplashScreenActivity.this, SyncActivity.class);
            if (data != null) {
                intent.setData(data); // Tambahkan URI ke Intent
            }
        } else {
            intent = new Intent(SplashScreenActivity.this, SignInActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
