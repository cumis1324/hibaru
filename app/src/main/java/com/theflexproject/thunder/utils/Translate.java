package com.theflexproject.thunder.utils;

import static com.theflexproject.thunder.Constants.LIBRE_TRANSLATE_URL;

import android.content.Context;
import android.util.Log;

import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Movie;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Translate {
    private static final String TAG = "Translate";
    public static String getTranslated(String text, String targetLanguage) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<String> callable = () -> translateText(text, targetLanguage);

        // Kirim tugas ke executor
        Future<String> future = executor.submit(callable);

        try {
            // Tunggu hasilnya
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null; // Kembalikan daftar kosong jika terjadi kesalahan
        } finally {
            // Pastikan executor ditutup
            executor.shutdown();
        }
    }
    public static String translateText(String text, String targetLanguage) throws Exception {
        // Log input text and target language
        Log.d(TAG, "Starting translation...");
        Log.d(TAG, "Text to translate: " + text);
        Log.d(TAG, "Target Language: " + targetLanguage);

        // URL API LibreTranslate
        URL url = new URL(LIBRE_TRANSLATE_URL);


        String encodedText = URLEncoder.encode(text, "UTF-8");
        Log.d(TAG, "Encoded text: " + encodedText);

        // Map Locale language code to Apertium's langpair
        String langPair = getLangPair(targetLanguage);

        // Membuat koneksi HTTP
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Format body request with valid langpair
        String urlParameters = "langpair=" + langPair + "&q=" + encodedText;
        Log.d(TAG, "Request body: " + urlParameters);

        // Mengirim request body
        OutputStream os = connection.getOutputStream();
        os.write(urlParameters.getBytes("UTF-8"));
        os.close();

        Log.d(TAG, "Request sent, reading response...");

        // Membaca response dari API
        int responseCode = connection.getResponseCode();
        BufferedReader in;
        StringBuilder response = new StringBuilder();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parsing JSON Response
            JSONObject responseJson = new JSONObject(response.toString());
            String translatedText = responseJson.getString("responseData");

            Log.d(TAG, "Translated Text: " + translatedText);
            return translatedText;

        } else {
            in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String inputLine;
            StringBuilder errorResponse = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                errorResponse.append(inputLine);
            }
            in.close();
            Log.e(TAG, "Request failed with code: " + responseCode);
            Log.e(TAG, "Error response: " + errorResponse.toString());
            return null;
        }
    }

    // Helper method to map the target language to the correct langpair for Apertium
    private static String getLangPair(String languageCode) {
        String langPair;
        switch (languageCode) {
            case "id": // Bahasa Indonesia
                langPair = "en|id";  // English to Indonesian
                break;
                case "in": // Bahasa Indonesia
                langPair = "en|id";  // English to Indonesian
                break;
            case "es": // Spanish
                langPair = "en|es";  // English to Spanish
                break;
            case "fr": // French
                langPair = "en|fr";  // English to French
                break;
            case "de": // German
                langPair = "en|de";  // English to German
                break;
            case "it": // Italian
                langPair = "en|it";  // English to Italian
                break;
            default:
                langPair = "en|id";  // Default to English to Indonesian
                break;
        }
        return langPair;
    }
}
