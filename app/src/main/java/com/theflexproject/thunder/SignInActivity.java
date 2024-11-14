package com.theflexproject.thunder;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private TextInputEditText emailEditText, passwordEditText;
    private Button signInButton, signUpButton, googleSignInButton;

    ViewGroup rootView;
    View decorView;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        decorView = getWindow().getDecorView();
        rootView = decorView.findViewById(android.R.id.content);

        emailEditText = findViewById(R.id.email);
        emailEditText.requestFocus();
        passwordEditText = findViewById(R.id.password);
        signInButton = findViewById(R.id.signinbtn);
        signUpButton = findViewById(R.id.signuppbtn);
        googleSignInButton = findViewById(R.id.google_signin_button);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()  // Request profile information
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        blurTop();

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            }
        });

        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
    }

    private void blurTop() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void signIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase authentication
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignInActivity.this, "Sign in successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignInActivity.this, LoadingActivity.class));
                        } else {
                            Toast.makeText(SignInActivity.this, "Sign in failed " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
                // You might want to save this info to your server or display it in the UI
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {

                            String userId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
                            DatabaseReference userReference = databaseReference.child(userId);
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("username", user.getDisplayName());
                            //userMap.put("firstName", user.getGivenName());
                            //userMap.put("lastName", user.getFamilyName());
                            userMap.put("email", user.getEmail());
                            assert user.getPhotoUrl() != null;
                            userMap.put("profileImage", user.getPhotoUrl().toString());


                            userReference.setValue(userMap);
                            Toast.makeText(SignInActivity.this, "Google sign in successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignInActivity.this, LoadingActivity.class));
                        } else {
                            Toast.makeText(SignInActivity.this, "Google sign in failed " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}

