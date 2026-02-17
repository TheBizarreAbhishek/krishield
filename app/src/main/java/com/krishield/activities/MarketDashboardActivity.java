package com.krishield.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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
import com.krishield.repositories.MarketRepository;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MarketDashboardActivity extends AppCompatActivity {
    private static final String TAG = "MarketDashboard";
    private static final int LOCATION_PERMISSION_CODE = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private GeminiService geminiService;

    private EditText etSearch;
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
        etSearch = findViewById(R.id.et_search);
        tvLocation = findViewById(R.id.tv_location);
        tvSeason = findViewById(R.id.tv_season);
        tvMarketData = findViewById(R.id.tv_market_data);
        tvAiRecommendation = findViewById(R.id.tv_ai_recommendation);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geminiService = new GeminiService(null);

        // Search button click handler
        findViewById(R.id.btn_search).setOnClickListener(v -> {
            String searchQuery = etSearch.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                searchCropPrice(searchQuery);
            } else {
                Toast.makeText(this, "Please enter crop/vegetable name", Toast.LENGTH_SHORT).show();
            }
        });

        // Search on keyboard enter
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String searchQuery = etSearch.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                searchCropPrice(searchQuery);
                return true;
            }
            return false;
        });

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

        // Use MarketRepository for caching
        MarketRepository repository = new MarketRepository(this);
        repository.getMarketData(currentCity, currentState, currentSeason, false,
                new MarketRepository.MarketCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            parseAndDisplayResponse(response);

                            // Show small toast if loaded from cache (optional debugging, or just silent)
                            // Toast.makeText(MarketDashboardActivity.this, "Data Loaded",
                            // Toast.LENGTH_SHORT).show();
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

    private void searchCropPrice(String cropName) {
        progressBar.setVisibility(View.VISIBLE);
        tvMarketData.setText("Searching for " + cropName + "...");
        tvAiRecommendation.setText("Analyzing...");

        String location = currentCity.isEmpty() ? "India" : currentCity + ", " + currentState;

        String prompt = String.format(
                "Act as an agricultural market expert. Estimate the current market price of %s in %s based on recent trends. "
                        +
                        "Provide:\n" +
                        "1. Estimated mandi price (₹/quintal or ₹/kg)\n" +
                        "2. Price trend (last 30 days)\n" +
                        "3. Best selling time\n" +
                        "4. Demand status\n\n" +
                        "Format as:\n" +
                        "PRICE:\n" +
                        "• %s: ₹price/unit\n" +
                        "• Trend: rising/falling/stable\n" +
                        "• Quality grade prices if available\n\n" +
                        "RECOMMENDATION:\n" +
                        "• Best time to sell\n" +
                        "• Market demand\n" +
                        "• Price forecast",
                cropName, location, cropName);

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
                            tvMarketData.setText("Service Unavailable");
                            tvAiRecommendation
                                    .setText("Error: " + error + "\nTry checking your internet or try again later.");
                        });
                    }
                });
    }
}
