package com.revaro.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// Looks up coordinates with OpenStreetMap's free Nominatim API so the homepage can sort by distance
@Component
public class GeocodingUtil {

    private static final Logger log = LoggerFactory.getLogger(GeocodingUtil.class);
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";
    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper;

    public GeocodingUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Returns {latitude, longitude}, or null if the city is blank or the lookup fails
    public double[] geocode(String city, String state) {
        if (city == null || city.isBlank()) {
            return null;
        }
        String query = city + (state == null || state.isBlank() ? "" : ", " + state) + ", USA";
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(NOMINATIM_URL + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                .header("User-Agent", "Revaro/1.0 (revaromeet.com)")
                .timeout(TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            JsonNode results = objectMapper.readTree(response.body());
            if (!results.isArray() || results.isEmpty()) {
                return null;
            }
            JsonNode first = results.get(0);
            return new double[]{first.get("lat").asDouble(), first.get("lon").asDouble()};
        } catch (IOException e) {
            log.warn("Geocoding failed for {}: {}", query, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
