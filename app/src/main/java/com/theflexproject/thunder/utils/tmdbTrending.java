package com.theflexproject.thunder.utils;

import static com.theflexproject.thunder.Constants.TMDB_API_KEY;

import android.util.Log;

import com.theflexproject.thunder.model.Cast;
import com.theflexproject.thunder.model.Credits;
import com.theflexproject.thunder.model.Crew;

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
    public List<String> getRecommendation(List<String> ids) {
        List<String> trendingIds = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Iterasi untuk setiap ID dalam daftar
        for (String id : ids) {
            try {
                String similarApiUrl = String.format("https://api.themoviedb.org/3/movie/%s/recommendations", id) + "?api_key=" + API_KEY;
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
                    continue;  // Jika request gagal, lanjutkan dengan ID berikutnya
                }

                InputStream inputStream = urlConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                System.out.println("Response JSON: " + buffer.toString());
                // Parse response JSON untuk mendapatkan rekomendasi
                List<String> idsFromResponse = parseTrendingIdsJson(buffer.toString());
                if (idsFromResponse != null) {
                    trendingIds.addAll(idsFromResponse);
                }

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

    public Credits getMovieCredits(int id) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        Credits movieCredits = null;

        try {
            String apiUrl = String.format("https://api.themoviedb.org/3/movie/%d/credits?api_key=%s", id, API_KEY);
            URL url = new URL(apiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = urlConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                movieCredits = parseCreditsJson(buffer.toString());
            } else {
                System.err.println("HTTP Error: " + urlConnection.getResponseCode());
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
            if (reader != null) try { reader.close(); } catch (IOException e) { e.printStackTrace(); }
        }

        return movieCredits;
    }
    public Credits getTvCredits(int id) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        Credits movieCredits = null;

        try {
            String apiUrl = String.format("https://api.themoviedb.org/3/tv/%d/credits?api_key=%s", id, API_KEY);
            URL url = new URL(apiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = urlConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                movieCredits = parseCreditsJson(buffer.toString());
            } else {
                System.err.println("HTTP Error: " + urlConnection.getResponseCode());
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
            if (reader != null) try { reader.close(); } catch (IOException e) { e.printStackTrace(); }
        }

        return movieCredits;
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
    private Credits parseCreditsJson(String json) {
        List<Cast> castList = new ArrayList<>();
        List<Crew> crewList = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(json);

            // Ambil array 'cast'
            JSONArray castArray = jsonObject.optJSONArray("cast");
            if (castArray != null) {
                for (int i = 0; i < castArray.length(); i++) {
                    JSONObject castMember = castArray.getJSONObject(i);
                    int id = castMember.optInt("id", -1);
                    String name = castMember.optString("name", null);
                    String character = castMember.optString("character", null);
                    String profilePath = castMember.optString("profile_path", null);

                    castList.add(new Cast(id, name, character, profilePath));
                }
            }

            // Ambil array 'crew'
            JSONArray crewArray = jsonObject.optJSONArray("crew");
            if (crewArray != null) {
                for (int i = 0; i < crewArray.length(); i++) {
                    JSONObject crewMember = crewArray.getJSONObject(i);
                    int id = crewMember.optInt("id", -1);
                    String name = crewMember.optString("name", null);
                    String job = crewMember.optString("job", null);
                    String departement = crewMember.optString("departement", null);
                    String profilePath = crewMember.optString("profile_path", null);

                    crewList.add(new Crew(id, name, job, departement, profilePath));
                }
            }

        } catch (JSONException e) {
            System.err.println("Error: Failed to parse JSON response - " + e.getMessage());
        }

        return new Credits(castList, crewList);
    }

}
