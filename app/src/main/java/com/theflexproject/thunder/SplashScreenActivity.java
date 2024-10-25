package com.theflexproject.thunder;

import android.content.Intent;
import android.os.Bundle;

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
        FirebaseApp.initializeApp(this);
        firebaseManager = new FirebaseManager();

        currentUser = firebaseManager.getCurrentUser();
        handleSignIn();
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