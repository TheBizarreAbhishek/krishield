package com.krishield.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Weather data models for Open-Meteo API
 */
public class WeatherModels {

    public static class WeatherResponse {
        @SerializedName("current_weather")
        public CurrentWeather currentWeather;

        @SerializedName("daily")
        public DailyForecast daily;

        @SerializedName("latitude")
        public double latitude;

        @SerializedName("longitude")
        public double longitude;
    }

    public static class CurrentWeather {
        @SerializedName("temperature")
        public float temperature;

        @SerializedName("weathercode")
        public int weathercode;

        @SerializedName("windspeed")
        public float windspeed;

        @SerializedName("time")
        public String time;
    }

    public static class DailyForecast {
        @SerializedName("time")
        public List<String> time;

        @SerializedName("temperature_2m_max")
        public List<Float> temperatureMax;

        @SerializedName("temperature_2m_min")
        public List<Float> temperatureMin;

        @SerializedName("weathercode")
        public List<Integer> weathercode;
    }

    /**
     * Helper class to represent a single day's forecast
     */
    public static class DayForecast {
        public String date;
        public float tempMax;
        public float tempMin;
        public int weatherCode;
        public String weatherDescription;
        public String weatherEmoji;

        public DayForecast(String date, float max, float min, int code) {
            this.date = date;
            this.tempMax = max;
            this.tempMin = min;
            this.weatherCode = code;
            this.weatherDescription = getWeatherDescription(code);
            this.weatherEmoji = getWeatherEmoji(code);
        }

        private String getWeatherDescription(int code) {
            if (code == 0)
                return "Clear sky";
            if (code >= 1 && code <= 3)
                return "Partly cloudy";
            if (code >= 45 && code <= 48)
                return "Foggy";
            if (code >= 51 && code <= 67)
                return "Rainy";
            if (code >= 71 && code <= 77)
                return "Snowy";
            if (code >= 80 && code <= 99)
                return "Thunderstorm";
            return "Unknown";
        }

        private String getWeatherEmoji(int code) {
            if (code == 0)
                return "☀️";
            if (code >= 1 && code <= 3)
                return "⛅";
            if (code >= 45 && code <= 48)
                return "🌫️";
            if (code >= 51 && code <= 67)
                return "🌧️";
            if (code >= 71 && code <= 77)
                return "❄️";
            if (code >= 80 && code <= 99)
                return "⛈️";
            return "🌤️";
        }
    }
}
