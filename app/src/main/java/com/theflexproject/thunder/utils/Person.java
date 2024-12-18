package com.theflexproject.thunder.utils;

import static com.theflexproject.thunder.Constants.TMDB_API_KEY;

import android.annotation.SuppressLint;

import com.theflexproject.thunder.model.CombinedCredits;
import com.theflexproject.thunder.model.MovieCredit;
import com.theflexproject.thunder.model.PersonDetails;
import com.theflexproject.thunder.model.TvCredit;

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

public class Person {
    public PersonDetails getPersonDetails(int personId) {
        PersonDetails personDetails = null;
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            @SuppressLint("DefaultLocale") String personDetailsApiUrl = String.format("https://api.themoviedb.org/3/person/%d?api_key=%s", personId, TMDB_API_KEY);
            URL url = new URL(personDetailsApiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to fetch person details. HTTP error code: " + responseCode);
                return personDetails;
            }

            InputStream inputStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            personDetails = parsePersonDetailsJson(buffer.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeResources(urlConnection, reader);
        }

        return personDetails;
    }

    // Mengambil data combined credits dari TMDb API
    public CombinedCredits getCombinedCredits(int personId) {
        CombinedCredits combinedCredits = null;
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            @SuppressLint("DefaultLocale") String combinedCreditsApiUrl = String.format("https://api.themoviedb.org/3/person/%d/combined_credits?api_key=%s", personId, TMDB_API_KEY);
            URL url = new URL(combinedCreditsApiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to fetch combined credits. HTTP error code: " + responseCode);
                return combinedCredits;
            }

            InputStream inputStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            combinedCredits = parseCombinedCreditsJson(buffer.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeResources(urlConnection, reader);
        }

        return combinedCredits;
    }

    // Parsing JSON untuk person details
    private PersonDetails parsePersonDetailsJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            String name = jsonObject.optString("name", "Unknown");
            String biography = jsonObject.optString("biography", "No biography available.");
            String profilePath = jsonObject.optString("profile_path", null);
            String birthday = jsonObject.optString("birthday", "Unknown"); // Menambahkan tanggal lahir

            // Membuat objek PersonDetails dengan data yang diperoleh
            return new PersonDetails(name, biography, profilePath, birthday);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    // Parsing JSON untuk combined credits
    private CombinedCredits parseCombinedCreditsJson(String json) {
        List<MovieCredit> movieCredits = new ArrayList<>();
        List<TvCredit> tvCredits = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray castArray = jsonObject.optJSONArray("cast");
            JSONArray crewArray = jsonObject.optJSONArray("crew");

            if (castArray != null) {
                for (int i = 0; i < castArray.length(); i++) {
                    JSONObject castObject = castArray.getJSONObject(i);
                    movieCredits.add(new MovieCredit(
                            castObject.optInt("id", -1),
                            castObject.optString("title", "Unknown"),
                            castObject.optString("character", "Unknown"),
                            castObject.optString("poster_path", null)
                    ));
                }
            }

            if (crewArray != null) {
                for (int i = 0; i < crewArray.length(); i++) {
                    JSONObject crewObject = crewArray.getJSONObject(i);
                    tvCredits.add(new TvCredit(
                            crewObject.optInt("id", -1),
                            crewObject.optString("name", "Unknown"),
                            crewObject.optString("job", "Unknown"),
                            crewObject.optString("poster_path", null)
                    ));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new CombinedCredits(movieCredits, tvCredits);
    }

    // Menutup koneksi dan reader
    private void closeResources(HttpURLConnection urlConnection, BufferedReader reader) {
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
}
