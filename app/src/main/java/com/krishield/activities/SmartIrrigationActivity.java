package com.krishield.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.krishield.R;
import com.krishield.models.WeatherModels;
import com.krishield.services.GeminiService;
import com.krishield.services.OpenMeteoService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

public class SmartIrrigationActivity extends AppCompatActivity {
    private static final String TAG = "SmartIrrigation";
    private static final int LOCATION_PERMISSION_CODE = 101;

    // UI Components
    private EditText etCrop;
    private Spinner spSoil;
    private TextView tvDate, tvResult;
    private Button btnAnalyze;
    private ProgressBar progressBar;
    private View layoutResult;

    // Data
    private Calendar lastWateredDate = Calendar.getInstance();
    private double currentLat = 28.61, currentLon = 77.20; // Default Delhi
    private String weatherInfo = "Weather data unavailable";

    // Services
    private FusedLocationProviderClient locationClient;
    private OpenMeteoService weatherService;
    private GeminiService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_irrigation);

        // 1. Init UI
        initViews();

        // 2. Init Services
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        weatherService = new OpenMeteoService();
        geminiService = new GeminiService(null);

        // 3. Start Location & Weather Fetch
        fetchLocationAndWeather();
    }

    private void initViews() {
        etCrop = findViewById(R.id.et_crop_name);
        spSoil = findViewById(R.id.spinner_soil_moisture);
        tvDate = findViewById(R.id.tv_last_watered_date);
        tvResult = findViewById(R.id.tv_recommendation);
        btnAnalyze = findViewById(R.id.btn_analyze);
        progressBar = findViewById(R.id.progress_bar);
        layoutResult = findViewById(R.id.layout_result);
        View btnBack = findViewById(R.id.btn_back);

        // Spinner Setup
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[] { "Normal (सामान्य)", "Dry (सूखी)", "Wet (गीली)" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSoil.setAdapter(adapter);

        // Listeners
        if (btnBack != null)
            btnBack.setOnClickListener(v -> finish());

        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                lastWateredDate.set(year, month, day);
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                tvDate.setText(sdf.format(lastWateredDate.getTime()));
            }, lastWateredDate.get(Calendar.YEAR), lastWateredDate.get(Calendar.MONTH),
                    lastWateredDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnAnalyze.setOnClickListener(v -> analyze());
    }

    private void fetchLocationAndWeather() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_CODE);
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();

                // Fetch Weather
                weatherService.getWeather(currentLat, currentLon, new OpenMeteoService.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherModels.WeatherResponse data) {
                        if (data != null && data.currentWeather != null) {
                            weatherInfo = String.format("Temp: %.1f°C, Wind: %.1f km/h",
                                    data.currentWeather.temperature, data.currentWeather.windspeed);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        weatherInfo = "Weather API Error: " + error;
                    }
                });
            }
        });
    }

    private void analyze() {
        String crop = etCrop.getText().toString().trim();
        String date = tvDate.getText().toString();
        String soil = spSoil.getSelectedItem().toString();

        if (crop.isEmpty() || date.equals("Select Date")) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutResult.setVisibility(View.GONE);
        btnAnalyze.setEnabled(false);

        String prompt = String.format(
                "You are an expert agronomist. User Input:\n" +
                        "- Crop: %s\n" +
                        "- Soil: %s\n" +
                        "- Last Watered: %s\n" +
                        "- Current Location Weather: %s\n\n" +
                        "Task: Provide a recommendation (Water Today / Do Not Water / Wait) with a very short reason (2 lines max). "
                        +
                        "Keep it simple and direct for a farmer.",
                crop, soil, date, weatherInfo);

        geminiService.sendTextMessage(prompt, Executors.newSingleThreadExecutor(),
                new GeminiService.ResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnAnalyze.setEnabled(true);
                            layoutResult.setVisibility(View.VISIBLE);
                            tvResult.setText(response);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnAnalyze.setEnabled(true);
                            Toast.makeText(SmartIrrigationActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather();
        }

    }
}
