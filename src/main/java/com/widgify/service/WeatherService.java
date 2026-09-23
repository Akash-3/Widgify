package com.widgify.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getWeatherForLocation(String locationName) {
        Map<String, Object> response = new HashMap<>();
        String queryLocation = (locationName != null && !locationName.trim().isEmpty()) ? locationName.trim() : "Bhubaneswar";

        try {
            // Step 1: Resolve lat/lon using Open-Meteo Geocoding API or fallback coordinates
            double latitude = 20.2961;
            double longitude = 85.8245;
            String resolvedName = queryLocation;

            try {
                String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(queryLocation, StandardCharsets.UTF_8) + "&count=1";
                HttpRequest geoReq = HttpRequest.newBuilder()
                        .uri(URI.create(geoUrl))
                        .timeout(Duration.ofSeconds(4))
                        .GET()
                        .build();

                HttpResponse<String> geoResp = httpClient.send(geoReq, HttpResponse.BodyHandlers.ofString());
                if (geoResp.statusCode() == 200) {
                    Map<String, Object> geoMap = objectMapper.readValue(geoResp.body(), Map.class);
                    List<Map<String, Object>> results = (List<Map<String, Object>>) geoMap.get("results");
                    if (results != null && !results.isEmpty()) {
                        Map<String, Object> first = results.get(0);
                        latitude = ((Number) first.get("latitude")).doubleValue();
                        longitude = ((Number) first.get("longitude")).doubleValue();
                        resolvedName = String.valueOf(first.getOrDefault("name", queryLocation));
                        if (first.containsKey("country")) {
                            resolvedName += ", " + first.get("country");
                        }
                    }
                }
            } catch (Exception ignored) {
                // Use default location fallback if geocoding fails
            }

            // Step 2: Fetch live weather forecast from Open-Meteo
            String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
                    latitude, longitude
            );

            HttpRequest weatherReq = HttpRequest.newBuilder()
                    .uri(URI.create(weatherUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> weatherResp = httpClient.send(weatherReq, HttpResponse.BodyHandlers.ofString());
            if (weatherResp.statusCode() == 200) {
                Map<String, Object> weatherMap = objectMapper.readValue(weatherResp.body(), Map.class);
                Map<String, Object> current = (Map<String, Object>) weatherMap.get("current");

                if (current != null) {
                    double temp = ((Number) current.getOrDefault("temperature_2m", 0.0)).doubleValue();
                    double feelsLike = ((Number) current.getOrDefault("apparent_temperature", temp)).doubleValue();
                    int humidity = ((Number) current.getOrDefault("relative_humidity_2m", 0)).intValue();
                    double windSpeed = ((Number) current.getOrDefault("wind_speed_10m", 0.0)).doubleValue();
                    int weatherCode = ((Number) current.getOrDefault("weather_code", 0)).intValue();

                    response.put("success", true);
                    response.put("location", resolvedName);
                    response.put("temperature", Math.round(temp * 10.0) / 10.0);
                    response.put("feelsLike", Math.round(feelsLike * 10.0) / 10.0);
                    response.put("humidity", humidity);
                    response.put("windSpeed", Math.round(windSpeed * 10.0) / 10.0);
                    response.put("condition", decodeWeatherCode(weatherCode));
                    response.put("unit", "°C");
                    return response;
                }
            }
        } catch (Exception e) {
            // Graceful failure - return clear failure state
        }

        response.put("success", false);
        response.put("message", "Weather service currently unavailable.");
        return response;
    }

    private String decodeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear Sky";
            case 1, 2, 3 -> "Partly Cloudy";
            case 45, 48 -> "Foggy";
            case 51, 53, 55, 56, 57 -> "Drizzle";
            case 61, 63, 65, 66, 67 -> "Rain";
            case 71, 73, 75, 77 -> "Snow";
            case 80, 81, 82 -> "Rain Showers";
            case 85, 86 -> "Snow Showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Overcast";
        };
    }
}
