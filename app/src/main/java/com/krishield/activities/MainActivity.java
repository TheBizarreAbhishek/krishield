package com.krishield.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.krishield.R;
import com.krishield.models.WeatherModels;
import com.krishield.services.GeminiService;
import com.krishield.services.OpenMeteoService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_CODE = 100;

    private TextView tvLocation, tvWeatherDesc, tvTemperature;
    private TextView tvMoisture, tvPests, tvSunlight, tvWind;
    private TextView tvSchemeTitle, tvSchemeDesc;
    private View weatherPill, pillMarket, pillIrrigation, pillCommunity, geminiSearch;
    private ImageView btnSettings;

    // ... (skipping some lines)

    private void initializeViews() {
        tvLocation = findViewById(R.id.tv_location);
        tvWeatherDesc = findViewById(R.id.tv_weather_desc);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvMoisture = findViewById(R.id.tv_moisture);
        tvPests = findViewById(R.id.tv_pests);
        tvSunlight = findViewById(R.id.tv_sunlight);
        tvWind = findViewById(R.id.tv_wind);
        tvSchemeTitle = findViewById(R.id.tv_scheme_title);
        tvSchemeDesc = findViewById(R.id.tv_scheme_desc);

        weatherPill = findViewById(R.id.weather_pill);
        pillMarket = findViewById(R.id.pill_market);
        pillIrrigation = findViewById(R.id.pill_irrigation);
        pillCommunity = findViewById(R.id.pill_community);
        geminiSearch = findViewById(R.id.gemini_search);
        btnSettings = findViewById(R.id.btn_settings);
    }

    // ... (skipping some lines)

    private void setupClickListeners() {
        weatherPill.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
            startActivity(intent);
        });

        pillMarket.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MarketDashboardActivity.class);
            startActivity(intent);
        });

        pillIrrigation.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SmartIrrigationActivity.class);
            startActivity(intent);
        });

        pillCommunity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SchemesActivity.class);
            startActivity(intent);
        });

        geminiSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void requestLocationAndLoadData() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_CODE);
        } else {
            getLocationAndLoadData();
        }
    }

    private void getLocationAndLoadData() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                getLocationName(location.getLatitude(), location.getLongitude());
                loadWeatherData(location.getLatitude(), location.getLongitude());
                loadPestData(location.getLatitude(), location.getLongitude());
                loadGovernmentSchemes();
            } else {
                // Default to Delhi if location not available
                tvLocation.setText("📍 Delhi, India");
                loadWeatherData(28.6139, 77.2090);
                loadPestData(28.6139, 77.2090);
                loadGovernmentSchemes();
            }
        });
    }

    private void getLocationName(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                currentCity = address.getLocality() != null ? address.getLocality() : "";
                currentCountry = address.getCountryName() != null ? address.getCountryName() : "";
                tvLocation.setText("📍 " + currentCity + ", " + currentCountry);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
        }
    }

    private void loadWeatherData(double latitude, double longitude) {
        weatherService.getWeather(latitude, longitude, new OpenMeteoService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherModels.WeatherResponse data) {
                runOnUiThread(() -> {
                    if (data.currentWeather != null) {
                        tvTemperature.setText(String.format("%.0f°C", data.currentWeather.temperature));
                        tvWeatherDesc.setText(getWeatherDescription(data.currentWeather.weathercode));
                        tvWind.setText(String.format("%.1f km/h", data.currentWeather.windspeed));
                    }
                    tvMoisture.setText("45%"); // Placeholder for current moisture

                    // Calculate sunlight hours (simplified)
                    int sunlightHours = calculateSunlightHours(latitude);
                    tvSunlight.setText(sunlightHours + " hrs");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Weather error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadPestData(double latitude, double longitude) {
        // Use Gemini to get pest information based on location
        String prompt = String.format(
                "For farming location at coordinates %.2f, %.2f (%s, %s), " +
                        "what is the current pest risk level? " +
                        "Respond with ONLY a percentage (e.g., '15%%' or '30%%') indicating pest risk.",
                latitude, longitude, currentCity, currentCountry);

        geminiService.sendTextMessage(prompt, executor, new GeminiService.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    // Extract percentage from response
                    String pestRisk = extractPercentage(response);
                    tvPests.setText(pestRisk);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvPests.setText("Low");
                });
            }
        });
    }

    private String getWeatherDescription(int weatherCode) {
        // WMO Weather interpretation codes
        if (weatherCode == 0)
            return "Clear sky";
        if (weatherCode <= 3)
            return "Partly cloudy";
        if (weatherCode <= 48)
            return "Foggy";
        if (weatherCode <= 67)
            return "Rainy";
        if (weatherCode <= 77)
            return "Snowy";
        if (weatherCode <= 82)
            return "Rain showers";
        if (weatherCode <= 86)
            return "Snow showers";
        return "Thunderstorm";
    }

    private int calculateSunlightHours(double latitude) {
        // Simplified calculation based on latitude
        // Tropical regions: ~12 hours, temperate: varies by season
        if (Math.abs(latitude) < 23.5)
            return 12; // Tropics
        if (Math.abs(latitude) < 45)
            return 10; // Subtropical
        return 8; // Temperate
    }

    private String extractPercentage(String text) {
        // Extract percentage from Gemini response
        if (text.contains("%")) {
            String[] parts = text.split("%");
            if (parts.length > 0) {
                String numPart = parts[0].trim();
                // Get last word which should be the number
                String[] words = numPart.split("\\s+");
                return words[words.length - 1] + "%";
            }
        }
        return "15%"; // Default
    }

    private void loadGovernmentSchemes() {
        // Use Gemini to fetch latest government schemes for farmers
        String prompt = "What is the latest government scheme for farmers in India? " +
                "Provide ONLY the scheme name and a brief one-line description. " +
                "Format: 'Scheme Name: Description' (max 100 characters total)";

        geminiService.sendTextMessage(prompt, executor, new GeminiService.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    // Parse response to extract title and description
                    String[] parts = response.split(":", 2);
                    if (parts.length == 2) {
                        tvSchemeTitle.setText("🌾 " + parts[0].trim());
                        tvSchemeDesc.setText(parts[1].trim());
                    } else {
                        tvSchemeTitle.setText("🌾 " + response.substring(0, Math.min(50, response.length())));
                        tvSchemeDesc.setText("Tap to learn more...");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvSchemeTitle.setText("🌾 PM-KISAN Scheme");
                    tvSchemeDesc.setText("Direct income support for farmers. Check eligibility...");
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndLoadData();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                // Load default Delhi data
                tvLocation.setText("📍 Delhi, India");
                loadWeatherData(28.6139, 77.2090);
                loadPestData(28.6139, 77.2090);
                loadGovernmentSchemes();
            }
        }
    }
}
