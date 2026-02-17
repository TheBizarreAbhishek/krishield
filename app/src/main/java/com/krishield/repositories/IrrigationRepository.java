package com.krishield.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import com.krishield.services.GeminiService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class IrrigationRepository {

    private static final String PREF_NAME = "KrishieldIrrigationCache";
    // Cache for 6 hours. Weather changes, but advice shouldn't flip too often in a
    // day.
    private static final long CACHE_DURATION = 6 * 60 * 60 * 1000;

    private final SharedPreferences prefs;
    private final GeminiService geminiService;
    private final Executor executor;

    public interface IrrigationCallback {
        void onSuccess(String response);

        void onError(String error);
    }

    public IrrigationRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String customApiKey = com.krishield.activities.SettingsActivity.getSavedApiKey(context);
        this.geminiService = new GeminiService(customApiKey);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void getIrrigationAdvice(String crop, String soil, String lastWateredDate, String weatherInfo,
            boolean forceRefresh, IrrigationCallback callback) {
        // Create a key based on inputs + today's date (so next day it refreshes)
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        // Sanitize string for key
        String safeCrop = crop.replaceAll("[^a-zA-Z0-9]", "");
        String key = "irrigation_" + safeCrop + "_" + soil + "_" + lastWateredDate + "_" + today;
        String timeKey = key + "_time";

        long lastUpdate = prefs.getLong(timeKey, 0);
        long currentTime = System.currentTimeMillis();

        if (!forceRefresh && (currentTime - lastUpdate < CACHE_DURATION)) {
            String cachedResponse = prefs.getString(key, null);
            if (cachedResponse != null && !cachedResponse.isEmpty() && !cachedResponse.contains("Error")) {
                callback.onSuccess(cachedResponse);
                return;
            }
        }

        // Fetch from API
        fetchFromGemini(crop, soil, lastWateredDate, weatherInfo, key, timeKey, callback);
    }

    private void fetchFromGemini(String crop, String soil, String date, String weather, String cacheKey, String timeKey,
            IrrigationCallback callback) {
        String prompt = String.format(
                "You are an expert agronomist. User Input:\n" +
                        "- Crop: %s\n" +
                        "- Soil: %s\n" +
                        "- Last Watered: %s\n" +
                        "- Current Location Weather: %s\n\n" +
                        "Task: Provide a recommendation (Water Today / Do Not Water / Wait) with a very short reason (2 lines max). "
                        +
                        "Keep it simple and direct for a farmer.",
                crop, soil, date, weather);

        geminiService.sendTextMessage(prompt, executor, new GeminiService.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                if (response != null && !response.isEmpty() && !response.toLowerCase().contains("quota")) {
                    prefs.edit()
                            .putString(cacheKey, response)
                            .putLong(timeKey, System.currentTimeMillis())
                            .apply();
                    callback.onSuccess(response);
                } else {
                    callback.onError("API Response Error: " + response);
                }
            }

            @Override
            public void onError(String error) {
                // Try fallback to cache if available
                String cachedResponse = prefs.getString(cacheKey, null);
                if (cachedResponse != null) {
                    callback.onSuccess(cachedResponse);
                } else {
                    callback.onError(error);
                }
            }
        });
    }
}
