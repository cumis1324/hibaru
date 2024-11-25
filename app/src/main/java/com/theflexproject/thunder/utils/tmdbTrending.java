package com.theflexproject.thunder.utils;

import static com.theflexproject.thunder.Constants.TMDB_API_KEY;

import android.util.Log;

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
    private static final String TMDB_SIMILAR_API_URL_TEMPLATE = "https://api.themoviedb.org/3/movie/%d/similar";
    private static final String API_KEY = TMDB_API_KEY;

    public List<String> getMovieTrending() {
        return getTrendingIdsFromTMDB(TMDB_MOVIE_API_URL);
    }

    public List<String> getSeriesTrending() {
        return getTrendingIdsFromTMDB(TMDB_SERIES_API_URL);
    }


    private List<String> getTrendingIdsFromTMDB(String apiUrl) {
        List<String> trendingIds = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(apiUrl + "?api_key=" + API_KEY);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error: Failed to fetch data from TMDB. HTTP Response Code: " + responseCode);
                return trendingIds;
            }

            InputStream inputStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            trendingIds = parseTrendingIdsJson(buffer.toString());

        } catch (IOException e) {
            System.err.println("Error: Network issue while fetching data - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: Unexpected issue - " + e.getMessage());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error: Failed to close reader - " + e.getMessage());
                }
            }
        }

        return trendingIds;
    }
    public List<String> getSimilarMovie(int id) {
        List<String> trendingIds = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            String similarApiUrl = String.format("https://api.themoviedb.org/3/movie/%d/similar", id) + "?api_key=" + API_KEY;
            System.out.println("URL formed: " + similarApiUrl);

            URL url = new URL(similarApiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            System.out.println("Opening connection...");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed: HTTP error code: " + responseCode);
                return trendingIds;
            }

            InputStream inputStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            System.out.println("Response JSON: " + buffer.toString());
            trendingIds = parseTrendingIdsJson(buffer.toString());

        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Exception occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error closing reader: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return trendingIds;
    }
    public List<String> getRecommendationMovie(int id) {
        List<String> trendingIds = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            String similarApiUrl = String.format("https://api.themoviedb.org/3/movie/%d/recommendations", id) + "?api_key=" + API_KEY;
            System.out.println("URL formed: " + similarApiUrl);

            URL url = new URL(similarApiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            System.out.println("Opening connection...");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed: HTTP error code: " + responseCode);
                return trendingIds;
            }

            InputStream inputStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            System.out.println("Response JSON: " + buffer.toString());
            trendingIds = parseTrendingIdsJson(buffer.toString());

        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Exception occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error closing reader: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return trendingIds;
    }
    public List<String> getTopRatedMovie() {
        List<String> trendingIds = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            String similarApiUrl = "https://api.themoviedb.org/3/movie/top_rated" + "?api_key=" + API_KEY;
            System.out.println("URL formed: " + similarApiUrl);

            URL url = new URL(similarApiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            System.out.println("Opening connection...");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed: HTTP error code: " + responseCode);
                return trendingIds;
            }

            InputStream inputStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            System.out.println("Response JSON: " + buffer.toString());
            trendingIds = parseTrendingIdsJson(buffer.toString());

        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Exception occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error closing reader: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return trendingIds;
    }
    public List<String> getLatest() {
        List<String> trendingIds = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            String similarApiUrl = "https://api.themoviedb.org/3/movie/latest" + "?api_key=" + API_KEY;
            System.out.println("URL formed: " + similarApiUrl);

            URL url = new URL(similarApiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            System.out.println("Opening connection...");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed: HTTP error code: " + responseCode);
                return trendingIds;
            }

            InputStream inputStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            System.out.println("Response JSON: " + buffer.toString());
            trendingIds = parseTrendingIdsJson(buffer.toString());

        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Exception occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error closing reader: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return trendingIds;
    }


    private List<String> parseTrendingIdsJson(String json) {
        List<String> trendingIds = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray resultsArray = jsonObject.getJSONArray("results");

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject mediaObject = resultsArray.getJSONObject(i);
                String mediaId = mediaObject.optString("id", null);
                if (mediaId != null) {
                    trendingIds.add(mediaId);
                }
            }

        } catch (JSONException e) {
            System.err.println("Error: Failed to parse JSON response - " + e.getMessage());
        }

        return trendingIds;
    }
}
