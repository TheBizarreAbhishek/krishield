package com.krishield.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.krishield.models.Scheme;
import com.krishield.services.GeminiService;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SchemesRepository {

    private static final String PREF_NAME = "KrishieldCache";
    private static final String KEY_SCHEMES = "cached_schemes";
    private static final String KEY_LAST_UPDATE = "schemes_last_update";
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 hours

    private final SharedPreferences prefs;
    private final GeminiService geminiService;
    private final Gson gson;

    public interface SchemesCallback {
        void onSuccess(List<Scheme> schemes);

        void onError(String error);
    }

    public SchemesRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String customApiKey = com.krishield.activities.SettingsActivity.getSavedApiKey(context);
        this.geminiService = new GeminiService(customApiKey);
        this.gson = new Gson();
    }

    public void getSchemes(boolean forceRefresh, SchemesCallback callback) {
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        long currentTime = System.currentTimeMillis();

        if (!forceRefresh && (currentTime - lastUpdate < CACHE_DURATION)) {
            String json = prefs.getString(KEY_SCHEMES, null);
            if (json != null) {
                Type type = new TypeToken<List<Scheme>>() {
                }.getType();
                List<Scheme> cachedSchemes = gson.fromJson(json, type);
                if (cachedSchemes != null && !cachedSchemes.isEmpty()) {
                    callback.onSuccess(cachedSchemes);
                    return;
                }
            }
        }

        // Fetch from API if cache expired or empty or forced
        fetchFromGemini(callback);
    }

    private void fetchFromGemini(SchemesCallback callback) {
        geminiService.getGovernmentSchemes(new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    // Clean code blocks if present
                    String cleanJson = response.replaceAll("```json", "").replaceAll("```", "").trim();

                    Type type = new TypeToken<List<Scheme>>() {
                    }.getType();
                    List<Scheme> schemes = gson.fromJson(cleanJson, type);

                    if (schemes != null) {
                        saveToCache(schemes);
                        callback.onSuccess(schemes);
                    } else {
                        callback.onError("Failed to parse schemes");
                    }
                } catch (Exception e) {
                    callback.onError("Parsing error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                // If API fails, try to return cache even if expired
                String json = prefs.getString(KEY_SCHEMES, null);
                if (json != null) {
                    Type type = new TypeToken<List<Scheme>>() {
                    }.getType();
                    List<Scheme> cachedSchemes = gson.fromJson(json, type);
                    if (cachedSchemes != null) {
                        callback.onSuccess(cachedSchemes);
                        return;
                    }
                }
                callback.onError(error);
            }
        });
    }

    private void saveToCache(List<Scheme> schemes) {
        String json = gson.toJson(schemes);
        prefs.edit()
                .putString(KEY_SCHEMES, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply();
    }
}
