package com.krishield.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.krishield.R;
import com.krishield.models.WeatherModels;
import com.krishield.services.OpenMeteoService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_CODE = 100;

    private TextView tvLocation, tvWeatherEmoji, tvTemperature, tvWeatherDescription, tvWindSpeed;
    private LinearLayout forecastContainer;
    private MaterialButton btnRefresh;
    private ProgressBar progressBar;

    private FusedLocationProviderClient fusedLocationClient;
    private OpenMeteoService weatherService;

    private double currentLat = 0;
    private double currentLon = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        tvLocation = findViewById(R.id.tv_location);
        tvWeatherEmoji = findViewById(R.id.tv_weather_emoji);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvWeatherDescription = findViewById(R.id.tv_weather_description);
        tvWindSpeed = findViewById(R.id.tv_wind_speed);
        forecastContainer = findViewById(R.id.forecast_container);
        btnRefresh = findViewById(R.id.btn_refresh);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        weatherService = new OpenMeteoService();

        // Setup refresh button
        btnRefresh.setOnClickListener(v -> fetchWeather());

        // Get location and fetch weather
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_CODE);
        } else {
            getLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission needed for weather",
                        Toast.LENGTH_LONG).show();
                tvLocation.setText("📍 Location permission denied");
            }
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        tvLocation.setText("📍 Detecting location...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLon = location.getLongitude();
                        getAddressFromLocation(location);
                        fetchWeather();
                    } else {
                        tvLocation.setText("📍 Unable to detect location");
                        Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    tvLocation.setText("📍 Location error");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void getAddressFromLocation(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality() != null ? address.getLocality() : "";
                String state = address.getAdminArea() != null ? address.getAdminArea() : "";

                if (!city.isEmpty() || !state.isEmpty()) {
                    tvLocation.setText("📍 " + city + ", " + state);
                } else {
                    tvLocation.setText("📍 Location detected");
                }
            }
        } catch (Exception e) {
            tvLocation.setText("📍 Location detected");
        }
    }

    private void fetchWeather() {
        if (currentLat == 0 && currentLon == 0) {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);

        weatherService.getWeather(currentLat, currentLon, new OpenMeteoService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherModels.WeatherResponse weather) {
                runOnUiThread(() -> {
                    displayWeather(weather);
                    progressBar.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(WeatherActivity.this, "Error: " + error,
                            Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                });
            }
        });
    }

    private void displayWeather(WeatherModels.WeatherResponse weather) {
        // Display current weather
        if (weather.currentWeather != null) {
            WeatherModels.CurrentWeather current = weather.currentWeather;

            tvTemperature.setText(String.format(Locale.getDefault(), "%.1f°C", current.temperature));
            tvWindSpeed.setText(String.format(Locale.getDefault(), "💨 Wind: %.1f km/h", current.windspeed));

            WeatherModels.DayForecast dayInfo = new WeatherModels.DayForecast(
                    "", 0, 0, current.weathercode);
            tvWeatherEmoji.setText(dayInfo.weatherEmoji);
            tvWeatherDescription.setText(dayInfo.weatherDescription);
        }

        // Display 7-day forecast
        if (weather.daily != null) {
            forecastContainer.removeAllViews();

            List<WeatherModels.DayForecast> forecasts = new ArrayList<>();
            for (int i = 0; i < Math.min(7, weather.daily.time.size()); i++) {
                forecasts.add(new WeatherModels.DayForecast(
                        weather.daily.time.get(i),
                        weather.daily.temperatureMax.get(i),
                        weather.daily.temperatureMin.get(i),
                        weather.daily.weathercode.get(i)));
            }

            for (WeatherModels.DayForecast forecast : forecasts) {
                addForecastDay(forecast);
            }
        }
    }

    private void addForecastDay(WeatherModels.DayForecast forecast) {
        View dayView = LayoutInflater.from(this).inflate(R.layout.item_forecast_day, forecastContainer, false);

        TextView tvDayEmoji = dayView.findViewById(R.id.tv_day_emoji);
        TextView tvDayDate = dayView.findViewById(R.id.tv_day_date);
        TextView tvDayDescription = dayView.findViewById(R.id.tv_day_description);
        TextView tvDayTempMax = dayView.findViewById(R.id.tv_day_temp_max);
        TextView tvDayTempMin = dayView.findViewById(R.id.tv_day_temp_min);

        tvDayEmoji.setText(forecast.weatherEmoji);
        tvDayDate.setText(formatDate(forecast.date));
        tvDayDescription.setText(forecast.weatherDescription);
        tvDayTempMax.setText(String.format(Locale.getDefault(), "%.0f°", forecast.tempMax));
        tvDayTempMin.setText(String.format(Locale.getDefault(), "%.0f°", forecast.tempMin));

        forecastContainer.addView(dayView);
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }
}
