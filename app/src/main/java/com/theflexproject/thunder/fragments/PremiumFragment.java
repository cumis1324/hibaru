package com.theflexproject.thunder.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.utils.pembayaran.IklanPremium;
import com.theflexproject.thunder.utils.pembayaran.TrakteerPaymentStatusTask;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class PremiumFragment extends Fragment implements IklanPremium.SubscriptionStatusListener {

    private TextInputEditText oidEditText;
    private Button checkStatusButton;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private TextView masaAktif;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_premium, container, false);
        oidEditText = view.findViewById(R.id.oid);
        checkStatusButton = view.findViewById(R.id.btnSubscribe);
        masaAktif = view.findViewById(R.id.masaAktif);
        IklanPremium.checkSubscriptionStatus(this);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("subscriptions");
        checkStatusButton.setOnClickListener(v -> {
            String oid = oidEditText.getText().toString();
            if (!oid.isEmpty()) {
                checkPaymentStatus(oid);
            } else {
                Toast.makeText(getContext(), "Please enter OID", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
    @SuppressLint("SetTextI18n")
    @Override
    public void onSubscriptionActive(Long subscriptionEnd) {
        // Konversi subscriptionEnd dari milidetik menjadi objek Date
        Date orderDate = new Date(subscriptionEnd);

        // Gunakan format tanggal sesuai dengan locale pengguna
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());

        // Tampilkan masa aktif pada TextView dengan format tanggal yang sudah diubah
        masaAktif.setText("Aktif sampai: " + dateFormat.format(orderDate));

        // Sembunyikan editText dan button jika langganan aktif
        oidEditText.setVisibility(View.GONE);
        checkStatusButton.setVisibility(View.GONE);
    }


    @Override
    public void onSubscriptionInactive() {


    }

    private void checkPaymentStatus(String oid) {
        new TrakteerPaymentStatusTask(new TrakteerPaymentStatusTask.OnPaymentStatusListener() {
            @Override
            public void onStatusChecked(Map<String, String> paymentData) {
                if (paymentData != null) {
                    handleSubscription(paymentData);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        }).execute(oid);
    }

    private void handleSubscription(Map<String, String> paymentData) {
        String userId = mAuth.getCurrentUser().getUid();
        String orderId = paymentData.get("OrderId");
        String orderDateStr = paymentData.get("OrderDate");
        String totalStr = paymentData.get("Total");
        Log.i("payment", totalStr);
        int total = 0;

        if (totalStr != null && !totalStr.isEmpty()) {
            int totals = Integer.parseInt(totalStr.replaceAll("\\D+", ""));
            total = totals;
            // Continue processing if conversion is successful
        } else {
            // Handle case where "Total" is empty or null
            Toast.makeText(getContext(), "Total pembayaran tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }
        if (orderDateStr != null && orderDateStr.contains(",")) {
            orderDateStr = orderDateStr.split(",")[0];
        }

        // Adjusted date format to match "26 October 2024, 04:10 WIB"
        // Update the date format to match "26 October 2024, 04:10 WIB"
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);

        try {
            // Parse the date string
            Date orderDate = dateFormat.parse(orderDateStr);

            if (total >= 5000 && orderDate != null) {
                // Calculate subscriptionEnd as 30 days from orderDate
                long subscriptionEnd = orderDate.getTime() + (30L * 24 * 60 * 60 * 1000);

                // Reference to user's subscription data
                DatabaseReference userSubscriptionRef = databaseRef.child(userId);

                // Check if OID has already been used
                userSubscriptionRef.child("usedOids").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        boolean oidUsed = false;

                        for (DataSnapshot oidSnapshot : snapshot.getChildren()) {
                            String usedOid = oidSnapshot.getValue(String.class);
                            if (usedOid != null && usedOid.equals(orderId)) {
                                oidUsed = true;
                                break;
                            }
                        }

                        if (oidUsed) {
                            // OID already used, show error message
                            Toast.makeText(getContext(), "OID ini sudah pernah digunakan untuk langganan.", Toast.LENGTH_SHORT).show();
                        } else {
                            // OID belum pernah digunakan, simpan data langganan baru
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("subscriptionEnd", subscriptionEnd);
                            userSubscriptionRef.updateChildren(updates)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            // Save OID to usedOids list
                                            userSubscriptionRef.child("usedOids").push().setValue(orderId);
                                            Toast.makeText(getContext(), "Langganan berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("SubscriptionCheck", "Error: " + error.getMessage());
                    }
                });
                IklanPremium.checkSubscriptionStatus(this);
            } else {
                Toast.makeText(getContext(), "Minimal donasi belum terpenuhi.", Toast.LENGTH_SHORT).show();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Tanggal donasi tidak valid.", Toast.LENGTH_SHORT).show();
        }

    }


}


