package com.theflexproject.thunder.utils;

import java.util.HashMap;
import java.util.Locale;

public class LanguageUtils {

    private static final HashMap<String, String> languageMapping;

    static {
        languageMapping = new HashMap<>();
        languageMapping.put("ms-ind", "Indonesia");
        languageMapping.put("en", "English");
        languageMapping.put("fr", "French");
        languageMapping.put("es", "Spanish");
        languageMapping.put("und", "Indonesia");
        // Tambahkan mapping manual lainnya
    }

    public static String getLanguageName(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return "Unknown";
        }

        // Cek mapping manual terlebih dahulu
        if (languageMapping.containsKey(languageCode)) {
            return languageMapping.get(languageCode);
        }

        // Gunakan Locale sebagai fallback
        Locale locale = new Locale(languageCode);
        return locale.getDisplayLanguage(new Locale("en")); // Nama dalam bahasa Inggris
    }
}

