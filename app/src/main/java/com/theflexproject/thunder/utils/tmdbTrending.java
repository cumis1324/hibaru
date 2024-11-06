package com.theflexproject.thunder.utils;

import static com.theflexproject.thunder.Constants.TMDB_API_KEY;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class tmdbTrending {

    private static final String TMDB_MOVIE_API_URL = "https://api.themoviedb.org/3/trending/movie/week";
    private static final String TMDB_SERIES_API_URL = "https://api.themoviedb.org/3/trending/tv/week";
    private static final String API_KEY = TMDB_API_KEY; // Ganti dengan API Key Anda

    // Metode untuk mendapatkan ID Trending Movies
    public List<String> getMovieTrending() {
        return getTrendingIdsFromTMDB(TMDB_MOVIE_API_URL);
    }

    // Metode untuk mendapatkan ID Trending Series
    public List<String> getSeriesTrending() {
        return getTrendingIdsFromTMDB(TMDB_SERIES_API_URL);
    }

    // Metode umum untuk mendapatkan ID Trending dari URL TMDB
    private List<String> getTrendingIdsFromTMDB(String apiUrl) {
        List<String> trendingIds = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String responseJsonStr = null;

        try {
            URL url = new URL(apiUrl + "?api_key=" + API_KEY);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }

            responseJsonStr = buffer.toString();
            trendingIds = parseTrendingIdsJson(responseJsonStr);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return trendingIds;
    }

    // Metode untuk parsing JSON dan mengembalikan daftar ID trending
    private List<String> parseTrendingIdsJson(String json) {
        List<String> trendingIds = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray resultsArray = jsonObject.getJSONArray("results");

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject mediaObject = resultsArray.getJSONObject(i);
                String mediaId = mediaObject.getString("id");
                trendingIds.add(mediaId);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return trendingIds;
    }
}
