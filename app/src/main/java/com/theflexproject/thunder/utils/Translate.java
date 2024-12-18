package com.theflexproject.thunder.utils;

import static com.theflexproject.thunder.Constants.LIBRE_TRANSLATE_URL;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Translate {
    public static String translateText(String text, String targetLanguage) throws Exception {

        // URL API LibreTranslate
        URL url = new URL(LIBRE_TRANSLATE_URL);

        // Membuat koneksi HTTP
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Membuat body request dalam format JSON
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("q", text);
        jsonBody.put("source", "auto");  // "auto" untuk deteksi bahasa sumber otomatis
        jsonBody.put("target", targetLanguage);

        // Mengirim request JSON
        OutputStream os = connection.getOutputStream();
        os.write(jsonBody.toString().getBytes("UTF-8"));
        os.close();

        // Membaca response dari API
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Menutup koneksi
        connection.disconnect();

        // Parsing JSON Response
        JSONObject responseJson = new JSONObject(response.toString());
        String translatedText = responseJson.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");

        return translatedText;
    }
}
