package com.example.sensehatclient;

final class SensorSource {
    final String label;
    final String baseUrl;

    SensorSource(String label, String baseUrl) {
        this.label = label;
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
