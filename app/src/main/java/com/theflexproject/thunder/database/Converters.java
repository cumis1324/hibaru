package com.theflexproject.thunder.database;

import android.util.Log;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.theflexproject.thunder.model.Data;
import com.theflexproject.thunder.model.Genres;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.Season;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class Converters {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static String fromData(Data data) {
        if (data == null)
            return null;
        return new Gson().toJson(data);
    }

    @TypeConverter
    public static Data fromStringToData(String string) {
        if (string == null || string.trim().isEmpty())
            return null;
        try {
            Gson gson = new Gson();
            return gson.fromJson(string, Data.class);
        } catch (Throwable t) {
            Log.e("Converters", "Failed to parse Data JSON: " + string, t);
            return null;
        }
    }

    // @TypeConverter
    // public static Long fromModifiedTime(Date date) {
    // if(date==null) return null;
    // return date.getTime();
    // }
    //
    // @TypeConverter
    // public static Date fromLong(Long date) {
    // if(date==null) return null;
    // return new Date(date);
    // }

    @TypeConverter
    public static String fromEpisodes(ArrayList<Episode> episodes) {
        if (episodes == null) {
            return null;
        }
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < episodes.size(); i++) {
            Episode e = episodes.get(i);
            sb.append(gson.toJson(e));
            sb.append("\t");
        }
        return sb.toString();
    }

    @TypeConverter
    public static ArrayList<Episode> fromStringToEpisodes(String episodesString) {
        if (episodesString == null || episodesString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Gson gson = new Gson();
        // "Mon Oct 10 22:07:31 GMT+05:30 2022"
        // Gson gson = new GsonBuilder().setDateFormat("E MMM dd HH:mm:ss 'Z'
        // yyyy").create();
        // SimpleDateFormat df = new SimpleDateFormat("EE MMM dd HH:mm:ss zzzz yyyy");

        Gson gson = new GsonBuilder().setDateFormat("E MMM dd HH:mm:ss z yyyy").create();
        // ObjectMapper om = new ObjectMapper();

        // ObjectMapper gson = new ObjectMapper();
        // SimpleDateFormat df=new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy",
        // Locale.US);
        // gson.setDateFormat(df);

        String[] arr = episodesString.split("\t");
        ArrayList<Episode> episodes = new ArrayList<>();
        for (String s : arr) {
            if (s.trim().isEmpty())
                continue;
            try {
                Episode episode = gson.fromJson(s, Episode.class);
                // Episode episode = gson.readValue(s , Episode.class);
                // System.out.println("Episode inside For Loop" + episode);
                if (episode != null) {
                    episodes.add(episode);
                }
            } catch (Throwable t) {
                Log.e("Converters", "Failed to parse episode: " + s, t);
            }
        }
        // System.out.println("episodesString in Converter" + episodesString);
        // System.out.println("episodesList in Converter" + episodes);
        return episodes;
    }

    @TypeConverter
    public static String fromSeasons(ArrayList<Season> seasons) {
        if (seasons == null) {
            return null;
        }
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seasons.size(); i++) {
            Season s = seasons.get(i);
            sb.append(gson.toJson(s));
            sb.append("\t");
        }

        return sb.toString();
    }

    @TypeConverter
    public static ArrayList<Season> fromStringToSeasons(String seasonsString) {
        if (seasonsString == null || seasonsString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        String[] arr = seasonsString.split("\t");
        ArrayList<Season> seasons = new ArrayList<>();
        for (String s : arr) {
            if (s.trim().isEmpty())
                continue;
            try {
                Season season = gson.fromJson(s, Season.class);
                if (season != null) {
                    seasons.add(season);
                }
            } catch (Throwable t) {
                Log.e("Converters", "Failed to parse season: " + s, t);
            }
        }
        return seasons;
    }

    @TypeConverter
    public static String fromGenreList(ArrayList<Genres> genres) {
        if (genres == null) {
            return null;
        }
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < genres.size(); i++) {
            Genres s = genres.get(i);
            sb.append(gson.toJson(s));
            sb.append("\t");
        }

        return sb.toString();
    }

    @TypeConverter
    public static ArrayList<Genres> fromStringToGenres(String genresString) {
        if (genresString == null || genresString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        String[] arr = genresString.split("\t");
        ArrayList<Genres> genres = new ArrayList<>();

        for (String s : arr) {
            if (s.trim().isEmpty())
                continue;
            try {
                Genres genre = gson.fromJson(s, Genres.class);
                if (genre != null) {
                    genres.add(genre);
                }
            } catch (Throwable t) {
                Log.e("Converters", "CRITICAL: Failed to parse genre string. Offending value: [" + s + "]", t);
                // Return empty object instead of crashing
            }
        }
        return genres;
    }
}
