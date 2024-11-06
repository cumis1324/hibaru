package com.theflexproject.thunder.utils;

import com.theflexproject.thunder.model.Genres;

import java.util.ArrayList;
import java.util.List;

public class GenreUtils {

    public static List<Genres> getGenresList() {
        List<Genres> genres = new ArrayList<>();
        genres.add(new Genres(28, "Action"));
        genres.add(new Genres(12, "Adventure"));
        genres.add(new Genres(16, "Animation"));
        genres.add(new Genres(35, "Comedy"));
        genres.add(new Genres(80, "Crime"));
        genres.add(new Genres(99, "Documentary"));
        genres.add(new Genres(18, "Drama"));
        genres.add(new Genres(10751, "Family"));
        genres.add(new Genres(14, "Fantasy"));
        genres.add(new Genres(36, "History"));
        genres.add(new Genres(27, "Horror"));
        genres.add(new Genres(10402, "Music"));
        genres.add(new Genres(9648, "Mystery"));
        genres.add(new Genres(10749, "Romance"));
        genres.add(new Genres(878, "Science Fiction"));
        genres.add(new Genres(10770, "TV Movie"));
        genres.add(new Genres(53, "Thriller"));
        genres.add(new Genres(10752, "War"));
        genres.add(new Genres(37, "Western"));
        return genres;
    }
    public static List<Genres> getTvSeriesGenresList() {
        List<Genres> genres = new ArrayList<>();
        genres.add(new Genres(10759, "Action & Adventure"));
        genres.add(new Genres(16, "Animation"));
        genres.add(new Genres(35, "Comedy"));
        genres.add(new Genres(80, "Crime"));
        genres.add(new Genres(99, "Documentary"));
        genres.add(new Genres(18, "Drama"));
        genres.add(new Genres(10751, "Family"));
        genres.add(new Genres(10762, "Kids"));
        genres.add(new Genres(9648, "Mystery"));
        genres.add(new Genres(10763, "News"));
        genres.add(new Genres(10764, "Reality"));
        genres.add(new Genres(10765, "Sci-Fi & Fantasy"));
        genres.add(new Genres(10766, "Soap"));
        genres.add(new Genres(10767, "Talk"));
        genres.add(new Genres(10768, "War & Politics"));
        genres.add(new Genres(37, "Western"));
        return genres;
    }
}

