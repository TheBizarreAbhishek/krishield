package com.krishield.services;

import android.util.Log;

import com.krishield.models.WeatherModels;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Service to fetch weather data from Open-Meteo API
 * Free API - No API key required!
 */
public class OpenMeteoService {

    private static final String TAG = "OpenMeteoService";
    private static final String BASE_URL = "https://api.open-meteo.com/v1/";

    private final OpenMeteoAPI api;

    public interface OpenMeteoAPI {
        @GET("forecast")
        Call<WeatherModels.WeatherResponse> getWeather(
                @Query("latitude") double latitude,
                @Query("longitude") double longitude,
                @Query("current_weather") boolean currentWeather,
                @Query("daily") String daily,
                @Query("timezone") String timezone);
    }

    public interface WeatherCallback {
        void onSuccess(WeatherModels.WeatherResponse weather);

        void onError(String error);
    }

    public OpenMeteoService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(OpenMeteoAPI.class);
    }

    public void getWeather(double latitude, double longitude, WeatherCallback callback) {
        // Request current weather + 7-day forecast
        String dailyParams = "temperature_2m_max,temperature_2m_min,weathercode";
        String timezone = "Asia/Kolkata";

        Call<WeatherModels.WeatherResponse> call = api.getWeather(
                latitude, longitude, true, dailyParams, timezone);

        call.enqueue(new Callback<WeatherModels.WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherModels.WeatherResponse> call,
                    Response<WeatherModels.WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "Weather API error: " + response.code());
                    callback.onError("Failed to fetch weather");
                }
            }

            @Override
            public void onFailure(Call<WeatherModels.WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Weather API failure", t);
                String error;
                if (t instanceof java.net.UnknownHostException) {
                    error = "No internet connection";
                } else if (t instanceof java.net.SocketTimeoutException) {
                    error = "Request timed out";
                } else {
                    error = "Network error";
                }
                callback.onError(error);
            }
        });
    }
}
