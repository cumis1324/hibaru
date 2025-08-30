package com.theflexproject.thunder;

import android.annotation.SuppressLint;

import java.util.Locale;
import java.util.Random;

public class Constants {
    @SuppressLint("ConstantLocale")
    public static final String language = Locale.getDefault().getLanguage();
    public static final String LIBRE_TRANSLATE_URL = "https://apertium.org/apy/translate"; // URL API LibreTranslate
    public static final String TMDB_GET_REQUEST_BASE_URL ="https://api.themoviedb.org/3/search/movie?api_key=";
    public static final String TMDB_BASE_URL = "https://api.themoviedb.org/3/";
    public static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    public static final String TMDB_BACKDROP_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w1280";
    public static final String FANART_IMAGE_BASE_URL = "https://webservice.fanart.tv/v3/";
    public static final String TMDB_API_KEY = "75399494372c92bd800f70079dff476b";
    public static final String SIMPLE_PROGRAM_DOWNLOAD_API = "https://geolocation.zindex.eu.org/generate.json?id=";
    public static final String CF_CACHE_TOKEN = "";
    public static final String background = "https://assets.nflxext.com/ffe/siteui/vlv3/03fdc4bf-72f6-4926-83a7-a76e6a1a5591/9f09b85f-530e-4090-82c3-a60ea2b3177f/US-en-20211115-popsignuptwoweeks-perspective_alpha_website_large.jpg";

    public static String getFanartApiKey(){
        final String[] KEYS = {
                "bd08c68d48b381d5b996c2b234c165da",
                "bd08c68d48b381d5b996c2b234c165da",
                ""};
        Random random = new Random();
        int index = random.nextInt(KEYS.length);
        return KEYS[index];
    }



}
