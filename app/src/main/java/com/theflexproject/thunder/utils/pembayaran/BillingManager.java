package com.theflexproject.thunder.utils.pembayaran;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BillingManager {

    private static final String TAG = "BillingManager";
    private final BillingClient billingClient;
    private final List<SkuDetails> skuDetailsList = new ArrayList<>();
    private final BillingCallback billingCallback;

    public BillingManager(Context context, BillingCallback callback) {
        this.billingCallback = callback;

        billingClient = BillingClient.newBuilder(context)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            callback.onPurchaseCompleted(purchase);
                        }
                    } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                        Log.d(TAG, "User canceled the purchase");
                    } else {
                        Log.e(TAG, "Error during purchase: " + billingResult.getDebugMessage());
                    }
                })
                .enablePendingPurchases()
                .build();
    }

    public void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup successful");
                    loadProductDetails();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e(TAG, "Billing service disconnected");
            }
        });
    }

    private void loadProductDetails() {
        List<String> skuList = new ArrayList<>();
        skuList.add("thank_you_message");
        skuList.add("super_thanks_50");
        skuList.add("super_thanks_100");

        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
                .build();

        billingClient.querySkuDetailsAsync(params, (billingResult, skuDetails) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetails != null) {
                skuDetailsList.clear();
                skuDetailsList.addAll(skuDetails);
                billingCallback.onProductsLoaded(skuDetailsList);
            } else {
                Log.e(TAG, "Failed to load products: " + billingResult.getDebugMessage());
            }
        });
    }

    public void startPurchase(Activity activity, SkuDetails skuDetails) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails) // Pastikan skuDetails sudah di-set sebelumnya
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, flowParams);

        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e("BillingManager", "Failed to launch billing flow: " + billingResult.getDebugMessage());
        }
    }


    public void endConnection() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }

    public interface BillingCallback {
        void onProductsLoaded(List<SkuDetails> products);

        void onPurchaseCompleted(Purchase purchase);
    }
    public void loadSubscriptionDetails(OnSkuDetailsLoadedListener listener) {
        List<String> skuList = Collections.singletonList("langganan_1_bulan");
        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.SUBS)
                .build();

        billingClient.querySkuDetailsAsync(params, (billingResult, skuDetails) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetails != null) {
                skuDetailsList.clear();
                skuDetailsList.addAll(skuDetails);
                listener.onSkuDetailsLoaded(skuDetails);
            } else {
                Log.e("BillingManager", "Failed to load subscriptions");
            }
        });
    }

    public void startSubscription(Activity activity, SkuDetails skuDetails) {
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }


    public interface OnSkuDetailsLoadedListener {
        void onSkuDetailsLoaded(List<SkuDetails> skuDetails);
    }
    public void checkSubscriptionStatus(OnSubscriptionStatusListener listener) {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, (billingResult, purchasesList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchasesList != null) {
                boolean isSubscribed = false;

                for (Purchase purchase : purchasesList) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                            && !purchase.isAcknowledged()) {
                        // Jika ada langganan aktif dan belum diakui
                        isSubscribed = true;
                        break;
                    }
                }

                listener.onSubscriptionStatusChecked(isSubscribed);
            } else {
                Log.e("BillingManager", "Failed to query purchases: " + billingResult.getDebugMessage());
                listener.onSubscriptionStatusChecked(false);
            }
        });
    }

    private void onPurchaseUpdated(BillingResult billingResult, List<Purchase> purchases) {
        // Proses pembaruan pembelian
    }

    public interface OnSubscriptionStatusListener {
        void onSubscriptionStatusChecked(boolean isSubscribed);
    }
}
