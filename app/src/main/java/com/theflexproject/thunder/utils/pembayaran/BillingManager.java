package com.theflexproject.thunder.utils.pembayaran;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
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
    private final List<SkuDetails> skuSubList = new ArrayList<>();
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
                    loadProductDetails();
                    loadSubscriptionDetails();
                    Log.d("BillingManager", "Connected: " + billingResult.getDebugMessage());

                } else {
                    Log.d("BillingManager", "Query Purchases Debug Message: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e(TAG, "Billing service disconnected");
            }
        });
    }
    public void startChecking(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    checkSubscriptionStatus();
                    checkItemStatus();

                } else {
                    Log.d("BillingManager", "Query Purchases Debug Message: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e(TAG, "Billing service disconnected");
            }
        });
    }

    private void checkItemStatus() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, (billingResult, purchasesList) -> {
            Log.d("BillingManager", "Query Purchases Result: " + billingResult.getResponseCode());
            Log.d("BillingManager", "Query Purchases Debug Message: " + billingResult.getDebugMessage());

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchasesList != null) {
                Log.d("BillingManager", "Number of purchases found: " + purchasesList.size());
                for (Purchase purchase : purchasesList) {
                    Log.d("BillingManager", "Purchase found: " + purchase.getSkus());
                    Log.d("BillingManager", "Purchase State: " + purchase.getPurchaseState());
                    Log.d("BillingManager", "Purchase Token: " + purchase.getPurchaseToken());

                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                        Log.d("BillingManager", "Valid item found for SKU: " + purchase.getSkus());
                            acknowledgePurchase(purchase); // Hanya akui jika belum diakui
                        Log.d("BillingManager", "item telah diakui: " + purchase.getSkus());
                        break; // Hentikan iterasi setelah langganan ditemukan
                    }
                }

            } else {
                Log.e("BillingManager", "Failed to query purchases: " + billingResult.getDebugMessage());
            }
        });
    }

    public void loadProductDetails() {
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
        void onSubscriptionLoaded(List<SkuDetails> subscriptions);

        void onPurchaseCompleted(Purchase purchase);
        void onSubscriptionStatus(boolean isSubscribed);
    }
    public void loadSubscriptionDetails() {
        List<String> skuList = new ArrayList<>();
        String sku = "langganan_1_bulan";
        skuList.add(sku);
        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.SUBS)
                .build();

        billingClient.querySkuDetailsAsync(params, (billingResult, skuDetails) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetails != null) {


                skuSubList.clear();
                skuSubList.addAll(skuDetails);
                billingCallback.onSubscriptionLoaded(skuSubList);
            } else {
                Log.e("BillingManager", "Failed to load subscriptions: " + billingResult.getDebugMessage());
            }
        });


    }

    public void startSubscription(Activity activity, SkuDetails skuDetails) {
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }



    public void checkSubscriptionStatus() {
        Log.d("BillingManager", "Starting to query purchases...");
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, (billingResult, purchasesList) -> {
            Log.d("BillingManager", "Query Purchases Result: " + billingResult.getResponseCode());
            Log.d("BillingManager", "Query Purchases Debug Message: " + billingResult.getDebugMessage());

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchasesList != null) {
                Log.d("BillingManager", "Number of purchases found: " + purchasesList.size());
                boolean isSubscribed = false;

                for (Purchase purchase : purchasesList) {
                    Log.d("BillingManager", "Purchase found: " + purchase.getSkus());
                    Log.d("BillingManager", "Purchase State: " + purchase.getPurchaseState());
                    Log.d("BillingManager", "Purchase Token: " + purchase.getPurchaseToken());

                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        Log.d("BillingManager", "Valid subscription found for SKU: " + purchase.getSkus());
                        isSubscribed = true;

                        if (!purchase.isAcknowledged()) {
                            acknowledgePurchase(purchase); // Hanya akui jika belum diakui
                        }

                        break; // Hentikan iterasi setelah langganan ditemukan
                    }
                }

                Log.d("BillingManager", "Subscription status: " + isSubscribed);
                billingCallback.onSubscriptionStatus(isSubscribed);
            } else {
                Log.e("BillingManager", "Failed to query purchases: " + billingResult.getDebugMessage());
                billingCallback.onSubscriptionStatus(false);
            }
        });
    }
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d("BillingManager", "Purchase acknowledged successfully.");
            } else {
                Log.e("BillingManager", "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
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
