package com.theflexproject.thunder;

import android.content.Intent;
import android.graphics.Color;
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
        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        FirebaseApp.initializeApp(this);
        firebaseManager = new FirebaseManager();

        currentUser = firebaseManager.getCurrentUser();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handleSignIn();
            }
        }, 4000);

    }
    private void handleSignIn(){
        if (currentUser != null) {
            startActivity(new Intent(SplashScreenActivity.this, LoadingActivity.class));
            finish();
        }else {
            startActivity(new Intent(SplashScreenActivity.this, SignInActivity.class));
            finish();
        }
    }
}