package com.krishield.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.krishield.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvGreeting, tvDate;
    private View searchBar, weatherCard;
    private MaterialCardView cardAiChat, cardMarket, cardWeatherFull, cardSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupGreeting();
        setupClickListeners();
    }

    private void initializeViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvDate = findViewById(R.id.tv_date);
        searchBar = findViewById(R.id.search_bar);
        weatherCard = findViewById(R.id.weather_card);
        cardAiChat = findViewById(R.id.card_ai_chat);
        cardMarket = findViewById(R.id.card_market);
        cardWeatherFull = findViewById(R.id.card_weather_full);
        cardSettings = findViewById(R.id.card_settings);
    }

    private void setupGreeting() {
        // Set greeting based on time
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = "Hello, Good Morning";
        } else if (hour < 17) {
            greeting = "Hello, Good Afternoon";
        } else {
            greeting = "Hello, Good Evening";
        }
        tvGreeting.setText(greeting);

        // Set current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime());
        tvDate.setText(currentDate);
    }

    private void setupClickListeners() {
        // Search Bar - Opens Chat
        searchBar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("mode", "text");
            startActivity(intent);
        });

        // Weather Card - Opens Weather Details
        weatherCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
            startActivity(intent);
        });

        // AI Chat Card
        cardAiChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("mode", "text");
            startActivity(intent);
        });

        // Market Prices Card
        cardMarket.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MarketDashboardActivity.class);
            startActivity(intent);
        });

        // Weather Full Card
        cardWeatherFull.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
            startActivity(intent);
        });

        // Settings Card
        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }
}
