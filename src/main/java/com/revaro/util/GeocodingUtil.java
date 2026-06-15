package com.revaro.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Geocodes city+state to lat/lng using OpenStreetMap Nominatim (free, no API key).
 */
@Component
public class GeocodingUtil {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String LAT_MARKER = "\"lat\":\"";
    private static final String LON_MARKER = "\"lon\":\"";
    private static final String QUOTE = "\"";

    public double[] geocode(String city, String state) {
        try {
            String q = city + (state != null && !state.isBlank() ? ", " + state : "") + ", USA";
            String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
            String urlStr = NOMINATIM_URL + "?q=" + encoded + "&format=json&limit=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Revaro-App/1.0");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() != 200) return null;

            try (InputStream is = conn.getInputStream()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (!body.contains(LAT_MARKER)) return null;

                int latStart = body.indexOf(LAT_MARKER) + LAT_MARKER.length();
                int latEnd = body.indexOf(QUOTE, latStart);
                int lonStart = body.indexOf(LON_MARKER) + LON_MARKER.length();
                int lonEnd = body.indexOf(QUOTE, lonStart);

                double lat = Double.parseDouble(body.substring(latStart, latEnd));
                double lon = Double.parseDouble(body.substring(lonStart, lonEnd));
                return new double[]{lat, lon};
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Geocoding failed for " + city + ": " + e.getMessage());
            return null;
        }
    }
}
