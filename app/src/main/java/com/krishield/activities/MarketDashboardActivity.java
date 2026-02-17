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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MarketDashboardActivity extends AppCompatActivity {
    private static final String TAG = "MarketDashboard";
    private static final int LOCATION_PERMISSION_CODE = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private com.krishield.repositories.MarketRepository marketRepository;

    private EditText etSearch;
    private TextView tvLocation, tvMarketData, tvAiRecommendation;
    private ProgressBar progressBar;

    private String currentCity = "Delhi"; // Default
    private String currentState = "Delhi";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_dashboard);

        // Initialize UI
        etSearch = findViewById(R.id.et_search);
        tvLocation = findViewById(R.id.tv_location);
        tvMarketData = findViewById(R.id.tv_market_data);
        tvAiRecommendation = findViewById(R.id.tv_ai_recommendation);
        progressBar = findViewById(R.id.progress_bar);

        // Hide Season view if it exists (simplification)
        View tvSeason = findViewById(R.id.tv_season);
        if (tvSeason != null)
            tvSeason.setVisibility(View.GONE);

        // Initialize Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        marketRepository = new com.krishield.repositories.MarketRepository(this);

        // Setup Listeners
        findViewById(R.id.btn_search).setOnClickListener(v -> performSearch());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        // Start Flow
        checkLocationAndLoad();
    }

    // ... (location permission code unchanged) ...

    private void checkLocationAndLoad() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_CODE);
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            loadMarketData(); // Load with default
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),
                            1);
                    if (addresses != null && !addresses.isEmpty()) {
                        currentCity = addresses.get(0).getLocality();
                        currentState = addresses.get(0).getAdminArea();
                        if (currentCity == null)
                            currentCity = "Unknown";
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Geocoder failed", e);
                }
            }
            tvLocation.setText("📍 " + currentCity + ", " + currentState);
            loadMarketData();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            fetchLocation();
        }
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter crop name", Toast.LENGTH_SHORT).show();
            return;
        }
        loadMarketData(query);
    }

    private void loadMarketData() {
        loadMarketData(null);
    }

    private void loadMarketData(String searchQuery) {
        progressBar.setVisibility(View.VISIBLE);
        tvMarketData.setText("Loading...");
        tvAiRecommendation.setText("");

        // Use MarketRepository which handles caching
        // If searchQuery is present, we might want to bypass cache or use a different
        // key
        // For now, let's treat search as a fresh request or unique cache key

        // Pass to repository
        // Note: The repository expects city, state, season. We use "General" for season
        // if not searching.
        String seasonOrQuery = (searchQuery == null) ? "General" : searchQuery;
        boolean forceRefresh = (searchQuery != null); // Always refresh on search for now

        marketRepository.getMarketData(currentCity, currentState, seasonOrQuery, forceRefresh,
                new com.krishield.repositories.MarketRepository.MarketCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            parseAndDisplay(response);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            String msg = error.contains("quota") ? "Quota Exceeded. Try later." : "Error: " + error;
                            tvMarketData.setText(msg);
                        });
                    }
                });
    }

    private void parseAndDisplay(String jsonResponse) {
        try {
            // Clean markdown syntax if present
            String jsonStr = jsonResponse.replace("```json", "").replace("```", "").trim();
            JSONArray jsonArray = new JSONArray(jsonStr);

            StringBuilder cropsDisplay = new StringBuilder();
            String aiRec = "";

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String crop = obj.optString("crop", "Unknown");
                String price = obj.optString("price", "--");
                String unit = obj.optString("unit", "");
                String trend = obj.optString("trend", "Stable");

                // Collect recommendation from first item (or all)
                if (obj.has("recommendation")) {
                    aiRec = obj.getString("recommendation");
                }

                cropsDisplay.append(String.format("• %s: %s %s (%s)\n\n", crop, price, unit, trend));
            }

            if (cropsDisplay.length() == 0)
                cropsDisplay.append("No data found.");

            tvMarketData.setText(cropsDisplay.toString());
            tvAiRecommendation.setText("💡 Advice: " + aiRec);

        } catch (Exception e) {
            Log.e(TAG, "JSON Parse Error", e);
            // Fallback: Just show raw text if JSON fails
            tvMarketData.setText(jsonResponse);
        }
    }
}
