package com.theflexproject.thunder.utils.pembayaran;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.Purchase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.model.FavHis;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                    List<Pair<String, ZonedDateTime>> itemList = new ArrayList<>();

                    // Sesuaikan format dengan format penyimpanan di Firebase
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        String itemId = item.getKey();
                        String lastPlayedStr = item.child("lastPlayed").getValue(String.class);

                        Log.d("FirebaseData", "Item ID: " + itemId + ", lastPlayed: " + lastPlayedStr);

                        if (itemId != null && lastPlayedStr != null) {
                            try {
                                ZonedDateTime lastPlayed = ZonedDateTime.parse(lastPlayedStr, formatter);
                                itemList.add(new Pair<>(itemId, lastPlayed));
                            } catch (Exception e) {
                                Log.e("DateParseError", "Error parsing date: " + lastPlayedStr, e);
                            }
                        }
                    }

                    Log.d("BeforeSort", "Before sorting: " + itemList.toString());

                    itemList.sort((a, b) -> b.second.compareTo(a.second));

                    Log.d("AfterSort", "After sorting: " + itemList.toString());


                    // Ambil hanya itemIds dalam urutan yang telah diurutkan
                    List<String> sortedItemIds = new ArrayList<>();
                    for (Pair<String, ZonedDateTime> pair : itemList) {
                        sortedItemIds.add(pair.first);
                    }

                    callback.onHistoryLoaded(sortedItemIds); // Panggil callback dengan daftar yang sudah diurutkan
                } else {
                    callback.onHistoryLoaded(new ArrayList<>()); // Data kosong jika tidak ada history
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching history: " + error.getMessage());
                callback.onHistoryLoaded(new ArrayList<>()); // Data kosong jika gagal
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
