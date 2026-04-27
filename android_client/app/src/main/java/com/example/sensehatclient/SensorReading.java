package com.example.sensehatclient;

final class SensorReading {
    final double temperature;
    final double humidity;
    final double pressure;
    final double north;

    SensorReading(double temperature, double humidity, double pressure, double north) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
        this.north = normalizeDegrees(north);
    }

    static double normalizeDegrees(double value) {
        double normalized = value % 360.0;
        if (normalized < 0) normalized += 360.0;
        return Double.isFinite(normalized) ? normalized : 0.0;
    }

    static String direction(double degrees) {
        String[] labels = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(normalizeDegrees(degrees) / 45.0) % labels.length;
        return labels[index];
    }
}
