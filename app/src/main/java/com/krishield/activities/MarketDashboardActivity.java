package com.krishield.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.krishield.R;
import com.krishield.services.GeminiService;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MarketDashboardActivity extends AppCompatActivity {
    private static final String TAG = "MarketDashboard";
    private static final int LOCATION_PERMISSION_CODE = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private GeminiService geminiService;

    private TextView tvLocation, tvSeason, tvMarketData, tvAiRecommendation;
    private ProgressBar progressBar;

    private String currentCity = "";
    private String currentState = "";
    private String currentSeason = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_dashboard);

        // Initialize views
        tvLocation = findViewById(R.id.tv_location);
        tvSeason = findViewById(R.id.tv_season);
        tvMarketData = findViewById(R.id.tv_market_data);
        tvAiRecommendation = findViewById(R.id.tv_ai_recommendation);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geminiService = new GeminiService(null);

        // Detect season
        currentSeason = getCurrentSeason();
        tvSeason.setText("🌾 " + currentSeason + " Season");

        // Get location and fetch data
        if (checkLocationPermission()) {
            getLocationAndFetchData();
        } else {
            requestLocationPermission();
        }
    }

    private String getCurrentSeason() {
        int month = Calendar.getInstance().get(Calendar.MONTH);
        // Rabi: Oct-Mar (9-2), Kharif: Jun-Sep (5-8), Zaid: Mar-Jun (2-5)
        if (month >= 9 || month <= 2) {
            return "Rabi (रबी)";
        } else if (month >= 5 && month <= 8) {
            return "Kharif (खरीफ)";
        } else {
            return "Zaid (जायद)";
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndFetchData();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
                // Use default location
                currentCity = "Delhi";
                currentState = "Delhi";
                tvLocation.setText("📍 " + currentCity + ", " + currentState);
                fetchMarketData();
            }
        }
    }

    private void getLocationAndFetchData() {
        if (!checkLocationPermission()) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    1);

                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                currentCity = address.getLocality() != null ? address.getLocality() : "Unknown";
                                currentState = address.getAdminArea() != null ? address.getAdminArea() : "Unknown";

                                tvLocation.setText("📍 " + currentCity + ", " + currentState);
                                fetchMarketData();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Geocoder error", e);
                            useDefaultLocation();
                        }
                    } else {
                        useDefaultLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Location fetch failed", e);
                    useDefaultLocation();
                });
    }

    private void useDefaultLocation() {
        currentCity = "Delhi";
        currentState = "Delhi";
        tvLocation.setText("📍 " + currentCity + ", " + currentState);
        fetchMarketData();
    }

    private void fetchMarketData() {
        progressBar.setVisibility(View.VISIBLE);
        tvMarketData.setText("Loading market data...");
        tvAiRecommendation.setText("Analyzing prices...");

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
                currentCity, currentState, currentSeason);

        geminiService.sendTextMessage(prompt, Executors.newSingleThreadExecutor(),
                new GeminiService.ResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            parseAndDisplayResponse(response);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvMarketData.setText("Error loading data: " + error);
                            tvAiRecommendation.setText("Unable to fetch recommendations");
                        });
                    }
                });
    }

    private void parseAndDisplayResponse(String response) {
        try {
            // Split response into crops and recommendation sections
            String[] parts = response.split("RECOMMENDATION:");

            if (parts.length >= 1) {
                String cropsData = parts[0].replace("CROPS:", "").trim();
                tvMarketData.setText(cropsData);
            }

            if (parts.length >= 2) {
                String recommendation = parts[1].trim();
                tvAiRecommendation.setText("💡 AI Recommendation\n\n" + recommendation);
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
            tvMarketData.setText(response);
            tvAiRecommendation.setText("Check market data above");
        }
    }
}
