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
        this.geminiService = new GeminiService(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void getMarketData(String city, String state, String season, boolean forceRefresh, MarketCallback callback) {
        String cacheKey = "market_data_" + city + "_" + state + "_" + season;
        long lastUpdateKey = "last_update_" + city + "_" + state + "_" + season;

        long lastUpdate = prefs.getLong(lastUpdateKey, 0);
        long currentTime = System.currentTimeMillis();

        if (!forceRefresh && (currentTime - lastUpdate < CACHE_DURATION)) {
            String cachedResponse = prefs.getString(cacheKey, null);
            if (cachedResponse != null && !cachedResponse.isEmpty()) {
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
                "Search Google for current market prices in %s, %s for %s crops. " +
                        "Provide:\n" +
                        "1. Top 5 crops with current mandi price (₹/quintal)\n" +
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
                // Save to cache
                prefs.edit()
                        .putString(cacheKey, response)
                        .putLong(timeKey, System.currentTimeMillis())
                        .apply();
                callback.onSuccess(response);
            }

            @Override
            public void onError(String error) {
                // Try to return old cache if API fails
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
