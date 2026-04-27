package com.example.sensehatclient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

final class SensorApiClient {
    private static final String TEMPERATURE = "/api/get_temperature";
    private static final String HUMIDITY = "/api/get_humidity";
    private static final String PRESSURE = "/api/get_pressure";
    private static final String NORTH = "/api/get_north";

    SensorReading fetchReading(SensorSource source, int timeoutMs) throws IOException, JSONException {
        double temperature = fetchNumber(source.baseUrl + TEMPERATURE, timeoutMs);
        double humidity = fetchNumber(source.baseUrl + HUMIDITY, timeoutMs);
        double pressure = fetchNumber(source.baseUrl + PRESSURE, timeoutMs);
        double north = fetchNumber(source.baseUrl + NORTH, timeoutMs);
        return new SensorReading(temperature, humidity, pressure, north);
    }

    private double fetchNumber(String url, int timeoutMs) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestProperty("Accept", "application/json");

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = readBody(stream).trim();
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status);
        }

        return parseNumber(body);
    }

    private String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private double parseNumber(String body) throws JSONException {
        if (body == null || body.trim().isEmpty()) {
            throw new JSONException("Empty response");
        }

        String trimmed = stripQuotes(body.trim());
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
            JSONObject object = new JSONObject(body);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                Object value = object.opt(keys.next());
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                if (value instanceof String) {
                    try {
                        return Double.parseDouble(((String) value).trim());
                    } catch (NumberFormatException ignoredAgain) {
                        // Continue checking the remaining values.
                    }
                }
            }
        }

        throw new JSONException("Invalid numeric payload");
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
