package com.theflexproject.thunder.utils.pembayaran;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.model.FavHis;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class IklanPremium {
    private static final String TAG = "SubscriptionUtils";
    private static final DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("subscriptions");
    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Interface callback untuk menangani status langganan
    public interface SubscriptionStatusListener {
        void onSubscriptionActive(Long subscriptionEnd) throws ParseException;
        void onSubscriptionInactive();
    }
    public interface HistoryCallback {
        void onHistoryLoaded(List<String> history);
    }

    public interface FavoritCallback {
        void onFavoritLoaded(List<String> favorit);
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
    public static void checkHistory(Context context, HistoryCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("History").child(userId);

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<String> itemIds = new ArrayList<>();
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        String itemId = item.getKey();
                        if (itemId != null) {
                            itemIds.add(itemId);
                        }
                    }
                    callback.onHistoryLoaded(itemIds);  // Panggil callback dengan data history
                } else {
                    callback.onHistoryLoaded(new ArrayList<>());  // Data kosong jika tidak ada history
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
                Log.e("FirebaseError", "Error fetching history: " + error.getMessage());
                callback.onHistoryLoaded(new ArrayList<>());  // Data kosong jika gagal
            }
        });
    }

    // Memeriksa data favorit dari Firebase
    public static void checkFavorit(Context context, FavoritCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference favoritRef = FirebaseDatabase.getInstance().getReference("Favorit").child(userId);

        favoritRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<String> itemIds = new ArrayList<>();
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        String itemId = item.getKey();
                        if (itemId != null) {
                            itemIds.add(itemId);
                        }
                    }
                    callback.onFavoritLoaded(itemIds);  // Panggil callback dengan data favorit
                } else {
                    callback.onFavoritLoaded(new ArrayList<>());  // Data kosong jika tidak ada favorit
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
                Log.e("FirebaseError", "Error fetching favorit: " + error.getMessage());
                callback.onFavoritLoaded(new ArrayList<>());  // Data kosong jika gagal
            }
        });
    }


}
