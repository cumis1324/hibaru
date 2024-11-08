package com.theflexproject.thunder.utils.pembayaran;

import android.os.AsyncTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TrakteerPaymentStatusTask extends AsyncTask<String, Void, Map<String, String>> {

    private final OnPaymentStatusListener listener;

    public interface OnPaymentStatusListener {
        void onStatusChecked(Map<String, String> paymentData);
        void onError(String error);
    }

    public TrakteerPaymentStatusTask(OnPaymentStatusListener listener) {
        this.listener = listener;
    }

    @Override
    protected Map<String, String> doInBackground(String... params) {
        String oid = params[0];
        Map<String, String> result = new HashMap<>();

        try {
            String url = "https://trakteer.id/payment-status/" + oid;
            Document doc = Jsoup.connect(url)
                    .header("Referer", "https://trakteer.id")
                    .userAgent("Mozilla/5.0")
                    .get();

            // Parsing data from the page
            Elements orderId = doc.select("#wrapper div div div div:nth-child(4) div:nth-child(1) div:nth-child(1) div:nth-child(3)");  // Replace with correct selector
            Elements cendol = doc.select("#wrapper div div div div:nth-child(3) div:nth-child(2) div div:nth-child(1) span:nth-child(2)");
            Elements orderDate = doc.select("#wrapper div div div div:nth-child(4) div:nth-child(1) div:nth-child(1) div:nth-child(2)");
            Elements paymentMethod = doc.select("#wrapper div div div div:nth-child(4) div:nth-child(1) div:nth-child(2) div:nth-child(2)");
            Elements amount = doc.select("#wrapper div div div div:nth-child(3) div:nth-child(3)");
            System.out.println("OrderId: " + orderId.text());
            System.out.println("OrderDate: " + orderDate.text());
            System.out.println("PaymentMethod: " + paymentMethod.text());
            System.out.println("CendolCount: " + cendol.text());
            System.out.println("Total: " + amount.text());


            result.put("OrderId", orderId.text());
            result.put("OrderDate", orderDate.text());
            result.put("PaymentMethod", paymentMethod.text());
            result.put("CendolCount", cendol.text());
            result.put("Total", amount.text());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    @Override
    protected void onPostExecute(Map<String, String> paymentData) {
        if (paymentData != null) {
            listener.onStatusChecked(paymentData);
        } else {
            listener.onError("Failed to fetch payment status.");
        }
    }
}
