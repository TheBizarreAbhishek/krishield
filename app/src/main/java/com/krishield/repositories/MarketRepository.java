package com.krishield.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import com.krishield.services.GeminiService;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MarketRepository {

    private static final String PREF_NAME = "KrishieldMarketCache";
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 hours

    private final SharedPreferences prefs;
    private final GeminiService geminiService;
    private final Executor executor;

    public interface MarketCallback {
        void onSuccess(String response);

        void onError(String error);
    }

    public MarketRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String customApiKey = com.krishield.activities.SettingsActivity.getSavedApiKey(context);
        this.geminiService = new GeminiService(customApiKey);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void getMarketData(String city, String state, String season, boolean forceRefresh, MarketCallback callback) {
        String cacheKey = "market_data_" + city + "_" + state + "_" + season;
        String lastUpdateKey = "last_update_" + city + "_" + state + "_" + season;

        long lastUpdate = prefs.getLong(lastUpdateKey, 0);
        long currentTime = System.currentTimeMillis();

        if (!forceRefresh && (currentTime - lastUpdate < CACHE_DURATION)) {
            String cachedResponse = prefs.getString(cacheKey, null);
            if (cachedResponse != null && !cachedResponse.isEmpty() && isValidResponse(cachedResponse)) {
                callback.onSuccess(cachedResponse);
                return;
            }
        }

        // Fetch from API
        fetchFromGemini(city, state, season, cacheKey, lastUpdateKey, callback);
    }

    private void fetchFromGemini(String city, String state, String season, String cacheKey, String timeKey,
            MarketCallback callback) {
        String prompt = String.format(
                "Act as an agricultural market expert. Estimate current market prices in %s, %s for %s crops based on recent trends. "
                        +
                        "Provide:\n" +
                        "1. Top 5 crops with estimated mandi price (₹/quintal)\n" +
                        "2. Brief 30-day trend (rising/falling/stable)\n" +
                        "3. Short selling recommendation\n\n" +
                        "Format as:\n" +
                        "CROPS:\n" +
                        "• Crop name: ₹price/quintal (trend)\n\n" +
                        "RECOMMENDATION:\n" +
                        "• Point 1\n" +
                        "• Point 2\n" +
                        "• Point 3",
                city, state, season);

        geminiService.sendTextMessage(prompt, executor, new GeminiService.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                // Validate response before caching
                if (isValidResponse(response)) {
                    prefs.edit()
                            .putString(cacheKey, response)
                            .putLong(timeKey, System.currentTimeMillis())
                            .apply();
                    callback.onSuccess(response);
                } else {
                    // If response is an error message (rate limit, etc.), valid but useless
                    // Do NOT cache it, and pass it to UI (or handle as error)
                    callback.onError("API Response Error: " + response);
                }
            }

            @Override
            public void onError(String error) {
                // Try to return old cache if API fails, BUT only if it's valid
                String cachedResponse = prefs.getString(cacheKey, null);
                if (cachedResponse != null && isValidResponse(cachedResponse)) {
                    callback.onSuccess(cachedResponse);
                } else {
                    callback.onError(error);
                }
            }
        });
    }

    // Helper to check if response looks like valid market data
    private boolean isValidResponse(String response) {
        if (response == null || response.isEmpty())
            return false;
        String lower = response.toLowerCase();
        // Check for common API error keywords
        return !lower.contains("rate limit") &&
                !lower.contains("quota exceeded") &&
                !lower.contains("internal error") &&
                !lower.contains("safety");
    }
}
