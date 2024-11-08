package com.theflexproject.thunder.utils.pembayaran;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;

public class IklanPremium {
    private static final String TAG = "SubscriptionUtils";
    private static final DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("subscriptions");
    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Interface callback untuk menangani status langganan
    public interface SubscriptionStatusListener {
        void onSubscriptionActive(Long subscriptionEnd) throws ParseException;
        void onSubscriptionInactive();
    }

    // Fungsi untuk mengecek status langganan
    public static void checkSubscriptionStatus(SubscriptionStatusListener listener) {
        String userId = mAuth.getCurrentUser().getUid();
        databaseRef.child(userId).child("subscriptionEnd").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Long subscriptionEnd = snapshot.getValue(Long.class);
                if (subscriptionEnd != null && subscriptionEnd > System.currentTimeMillis()) {
                    // Langganan aktif
                    try {
                        listener.onSubscriptionActive(subscriptionEnd);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // Langganan tidak aktif
                    listener.onSubscriptionInactive();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });
    }

}
