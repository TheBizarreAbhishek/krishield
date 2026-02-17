package com.krishield.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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

    private static final int LOCATION_PERMISSION_CODE = 101;

    private EditText etCropName;
    private Spinner spinnerSoilMoisture;
    private TextView tvLastWateredDate;
    private Button btnAnalyze;
    private ProgressBar progressBar;
    private View layoutResult;
    private TextView tvRecommendation;
    private View btnBack;

    private Calendar lastWateredCalendar = Calendar.getInstance();
    private FusedLocationProviderClient fusedLocationClient;
    private OpenMeteoService weatherService;
    private GeminiService geminiService;

    private WeatherModels.CurrentWeather currentWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_irrigation);

        // Initialize Views
        etCropName = findViewById(R.id.et_crop_name);
        spinnerSoilMoisture = findViewById(R.id.spinner_soil_moisture);
        tvLastWateredDate = findViewById(R.id.tv_last_watered_date);
        btnAnalyze = findViewById(R.id.btn_analyze);
        progressBar = findViewById(R.id.progress_bar);
        layoutResult = findViewById(R.id.layout_result);
        tvRecommendation = findViewById(R.id.tv_recommendation);
        btnBack = findViewById(R.id.btn_back);

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[] { "Normal (सामान्य)", "Dry (सूखी)", "Wet (गीली)" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSoilMoisture.setAdapter(adapter);

        // Setup Date Picker
        tvLastWateredDate.setOnClickListener(v -> showDatePicker());

        // Setup Back Button
        btnBack.setOnClickListener(v -> finish());

        // Initialize Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        weatherService = new OpenMeteoService();
        geminiService = new GeminiService(null);

        // Fetch Location & Weather automatically on start
        fetchLocationAndWeather();

        // Analyze Button Click
        btnAnalyze.setOnClickListener(v -> analyzeIrrigationNeeds());
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            lastWateredCalendar.set(Calendar.YEAR, year);
            lastWateredCalendar.set(Calendar.MONTH, month);
            lastWateredCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        },
                lastWateredCalendar.get(Calendar.YEAR),
                lastWateredCalendar.get(Calendar.MONTH),
                lastWateredCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        String myFormat = "dd MMM yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        tvLastWateredDate.setText(sdf.format(lastWateredCalendar.getTime()));
    }

    private void fetchLocationAndWeather() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                fetchWeather(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(this, "Could not get location. Using default weather data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchWeather(double lat, double lon) {
        weatherService.getWeather(lat, lon, new OpenMeteoService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherModels.WeatherResponse data) {
                if (data != null) {
                    currentWeather = data.currentWeather;
                }
            }

            @Override
            public void onError(String error) {
                // Determine if it's a critical error or just data missing
                runOnUiThread(() -> {
                    Toast.makeText(SmartIrrigationActivity.this, "Weather Update Failed: " + error, Toast.LENGTH_SHORT)
                            .show();
                });
            }
        });
    }

    private void analyzeIrrigationNeeds() {
        String crop = etCropName.getText().toString().trim();
        String soil = spinnerSoilMoisture.getSelectedItem().toString();
        String lastWatered = tvLastWateredDate.getText().toString();

        if (crop.isEmpty()) {
            etCropName.setError("Please enter crop name");
            return;
        }
        if (lastWatered.equals("Select Date")) {
            Toast.makeText(this, "Please select last watered date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare Prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("Act as an expert agronomist advisor for Indian farmers. ");
        prompt.append("Help me decide if I need to water my crops today.\n\n");
        prompt.append("Data Provided:\n");
        prompt.append("- Crop: ").append(crop).append("\n");
        prompt.append("- Soil Moisture: ").append(soil).append("\n");
        prompt.append("- Last Watered: ").append(lastWatered).append("\n");

        if (currentWeather != null) {
            prompt.append("- Current Temp: ").append(currentWeather.temperature).append("°C\n");
            prompt.append("- Wind Speed: ").append(currentWeather.windspeed).append(" km/h\n");
        } else {
            prompt.append("- Current Temp: Unknown (Assume typical Indian season temp)\n");
        }

        prompt.append("\nTask:\n");
        prompt.append("1. Analyze the crop's water needs based on soil and weather.\n");
        prompt.append("2. Give a DIRECT recommendation: 'Water Today', 'Wait X days', or 'Do not water'.\n");
        prompt.append("3. Provide a short reason (2-3 bullet points).\n");
        prompt.append("4. Mention any precautions.\n");

        // UI Update
        progressBar.setVisibility(View.VISIBLE);
        layoutResult.setVisibility(View.GONE);
        btnAnalyze.setEnabled(false);

        // Call Gemini
        geminiService.sendTextMessage(prompt.toString(), Executors.newSingleThreadExecutor(),
                new GeminiService.ResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnAnalyze.setEnabled(true);
                            layoutResult.setVisibility(View.VISIBLE);
                            tvRecommendation.setText(response);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnAnalyze.setEnabled(true);
                            String errorMsg = error.contains("quota") ? "Quota Limit Reached. Try again later."
                                    : "Analysis Failed: " + error;
                            tvRecommendation.setText(errorMsg);
                            layoutResult.setVisibility(View.VISIBLE);
                            Toast.makeText(SmartIrrigationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }
}
