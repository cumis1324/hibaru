package com.theflexproject.thunder.utils;

import java.util.regex.Pattern;

public class MovieQualityExtractor {
    public static String extractQualtiy(String name) {
        // Menambahkan pola untuk Blu-ray dengan resolusi
        if (Pattern.compile(Pattern.quote("Blu-ray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "Blu-ray 4K";
        if (Pattern.compile(Pattern.quote("Blu-ray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "Blu-ray 1080p";
        if (Pattern.compile(Pattern.quote("Blu-ray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("720")) return "Blu-ray 720p";
        if (Pattern.compile(Pattern.quote("Blu-ray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("540")) return "Blu-ray 540p";
        if (Pattern.compile(Pattern.quote("Blu-ray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("480")) return "Blu-ray 480p";

        if (Pattern.compile(Pattern.quote("Bluray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "Blu-ray 4K";
        if (Pattern.compile(Pattern.quote("Bluray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "Blu-ray 1080p";
        if (Pattern.compile(Pattern.quote("bluray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("720")) return "Blu-ray 720p";
        if (Pattern.compile(Pattern.quote("bluray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("540")) return "Blu-ray 540p";
        if (Pattern.compile(Pattern.quote("bluray"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("480")) return "Blu-ray 480p";

        if (Pattern.compile(Pattern.quote("webdl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "WEBDL 4K";
        if (Pattern.compile(Pattern.quote("webdl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "WEBDL 1080p";
        if (Pattern.compile(Pattern.quote("webdl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("720")) return "WEBDL 720p";
        if (Pattern.compile(Pattern.quote("webdl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("540")) return "WEBDL 540p";
        if (Pattern.compile(Pattern.quote("webdl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("480")) return "WEBDL 480p";

        if (Pattern.compile(Pattern.quote("Web-dl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "WEBDL 4K";
        if (Pattern.compile(Pattern.quote("Web-dl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "WEBDL 1080p";
        if (Pattern.compile(Pattern.quote("Web-dl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("720")) return "WEBDL 720p";
        if (Pattern.compile(Pattern.quote("Web-dl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("540")) return "WEBDL 540p";
        if (Pattern.compile(Pattern.quote("Web-dl"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("480")) return "WEBDL 480p";

        if (Pattern.compile(Pattern.quote("webrip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "WEBRip 4K";
        if (Pattern.compile(Pattern.quote("webrip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "WEBRip 1080p";
        if (Pattern.compile(Pattern.quote("webrip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("720")) return "WEBRip 720p";
        if (Pattern.compile(Pattern.quote("webrip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("540")) return "WEBRip 540p";
        if (Pattern.compile(Pattern.quote("webrip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("480")) return "WEBRip 480p";

        if (Pattern.compile(Pattern.quote("WEB-Rip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "WEBRip 4K";
        if (Pattern.compile(Pattern.quote("WEB-Rip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "WEBRip 1080p";
        if (Pattern.compile(Pattern.quote("WEB-Rip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("720")) return "WEBRip 720p";
        if (Pattern.compile(Pattern.quote("WEB-Rip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("540")) return "WEBRip 540p";
        if (Pattern.compile(Pattern.quote("WEB-Rip"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("480")) return "WEBRip 480p";

        if (Pattern.compile(Pattern.quote("CAM"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "CAM 4K";
        if (Pattern.compile(Pattern.quote("CAM"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "CAM 1080p";
        if (Pattern.compile(Pattern.quote("CAM"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("720")) return "CAM 720p";
        if (Pattern.compile(Pattern.quote("CAM"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("540")) return "CAM 540p";
        if (Pattern.compile(Pattern.quote("CAM"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("480")) return "CAM 480p";

        // Logika yang ada untuk DOVI, HDR, dan lainnya
        if (Pattern.compile(Pattern.quote("DOVI"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "4K Dolby Vision";
        if (Pattern.compile(Pattern.quote("DOVI"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "1080p Dolby Vision";
        if (Pattern.compile(Pattern.quote("HDR"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("2160")) return "4K HDR";
        if (Pattern.compile(Pattern.quote("HDR"), Pattern.CASE_INSENSITIVE).matcher(name).find() && name.contains("1080")) return "1080p HDR";

        if (Pattern.compile(Pattern.quote("Dolby Vision"), Pattern.CASE_INSENSITIVE).matcher(name).find() ||
                Pattern.compile(Pattern.quote("DVSUX"), Pattern.CASE_INSENSITIVE).matcher(name).find()) return "Dolby Vision";

        // Pola untuk resolusi tanpa Blu-ray
        if (name.contains("2160")) return "4K";
        if (name.contains("1080")) return "1080p";
        if (name.contains("720")) return "720p";
        if (name.contains("540")) return "540p";
        if (name.contains("480")) return "480p";

        // Pola tambahan untuk format tertentu
        if (name.contains("XviD") || name.contains("XVID")) return "XVID";
        if (name.contains("ION10")) return "ION10";

        return "Unknown";
    }

}
