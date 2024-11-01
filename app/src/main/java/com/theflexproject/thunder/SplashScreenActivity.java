package com.theflexproject.thunder;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.theflexproject.thunder.model.FirebaseManager;


public class SplashScreenActivity extends AppCompatActivity {
    FirebaseManager firebaseManager;
    static FirebaseUser currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent().getData();
        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        FirebaseApp.initializeApp(this);
        firebaseManager = new FirebaseManager();
        currentUser = firebaseManager.getCurrentUser();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handleSignIn(data);
            }
        }, 4000);

    }
    private void handleSignIn(Uri data){
        Intent intent;
        if (currentUser != null) {
            intent = new Intent(SplashScreenActivity.this, LoadingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            if (data != null) {
                intent.setData(data);  // Tambahkan URI ke Intent
            }
        } else {
            intent = new Intent(SplashScreenActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
        finish();
    }
}