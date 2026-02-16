package com.krishield.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.krishield.R;
import com.krishield.services.GeminiService;

import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PriceAdvisorActivity extends AppCompatActivity {

    private Spinner spinnerCrop, spinnerMarket;
    private MaterialButton btnAnalyze;
    private ProgressBar progressBar;
    private MaterialCardView cardRecommendation;
    private TextView tvTrend, tvRecommendation, tvReasoning, tvAction;

    private GeminiService geminiService;
    private Executor executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_advisor);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("AI Price Advisor");
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        spinnerCrop = findViewById(R.id.spinner_crop);
        spinnerMarket = findViewById(R.id.spinner_market);
        btnAnalyze = findViewById(R.id.btn_analyze);
        progressBar = findViewById(R.id.progress_bar);
        cardRecommendation = findViewById(R.id.card_recommendation);
        tvTrend = findViewById(R.id.tv_trend);
        tvRecommendation = findViewById(R.id.tv_recommendation);
        tvReasoning = findViewById(R.id.tv_reasoning);
        tvAction = findViewById(R.id.tv_action);

        // Setup spinners
        setupCropSpinner();
        setupMarketSpinner();

        // Initialize Gemini
        geminiService = new GeminiService();
        executor = Executors.newSingleThreadExecutor();

        // Setup analyze button
        btnAnalyze.setOnClickListener(v -> analyzePriceTrend());
    }

    private void setupCropSpinner() {
        String[] crops = {
                "Select Crop",
                "Potato (आलू)",
                "Onion (प्याज)",
                "Tomato (टमाटर)",
                "Wheat (गेहूं)",
                "Rice (चावल)",
                "Sugarcane (गन्ना)",
                "Cotton (कपास)",
                "Soybean (सोयाबीन)",
                "Maize (मक्का)",
                "Groundnut (मूंगफली)"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                crops);
        spinnerCrop.setAdapter(adapter);
    }

    private void setupMarketSpinner() {
        String[] markets = {
                "Select Market",
                "Delhi",
                "Mumbai",
                "Bangalore",
                "Kolkata",
                "Chennai",
                "Hyderabad",
                "Pune",
                "Ahmedabad",
                "Jaipur",
                "Lucknow"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                markets);
        spinnerMarket.setAdapter(adapter);
    }

    private void analyzePriceTrend() {
        // Validate inputs
        String crop = spinnerCrop.getSelectedItem().toString();
        String market = spinnerMarket.getSelectedItem().toString();

        if (crop.equals("Select Crop")) {
            Toast.makeText(this, "Please select a crop", Toast.LENGTH_SHORT).show();
            return;
        }

        if (market.equals("Select Market")) {
            Toast.makeText(this, "Please select a market", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);
        cardRecommendation.setVisibility(View.GONE);

        // Create AI prompt for automatic price analysis
        String prompt = createAutomaticAnalysisPrompt(crop, market);

        // Call Gemini
        geminiService.sendTextMessage(prompt, executor, new GeminiService.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    parseAndDisplayRecommendation(response);
                    progressBar.setVisibility(View.GONE);
                    btnAnalyze.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PriceAdvisorActivity.this,
                            "Analysis failed: " + error, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnAnalyze.setEnabled(true);
                });
            }
        });
    }

    private String createAutomaticAnalysisPrompt(String crop, String market) {
        return String.format(
                "You are an expert market analyst for Indian agriculture.\n\n" +
                        "TASK: Analyze current market conditions for %s in %s market and provide a selling recommendation.\n\n"
                        +
                        "INSTRUCTIONS:\n" +
                        "1. Search for CURRENT (today's) %s prices in %s market\n" +
                        "2. Find price trends for the LAST 30 DAYS if available\n" +
                        "3. Consider seasonal factors and market demand\n" +
                        "4. Analyze if prices are rising, falling, or stable\n" +
                        "5. Provide a clear SELL NOW or WAIT recommendation\n\n" +
                        "REQUIRED OUTPUT FORMAT (JSON only, no other text):\n" +
                        "{\n" +
                        "  \"current_price\": \"₹X per quintal (source: Y)\",\n" +
                        "  \"trend\": \"rising\" or \"falling\" or \"stable\",\n" +
                        "  \"trend_percentage\": \"+X%%\" or \"-X%%\",\n" +
                        "  \"recommendation\": \"SELL NOW\" or \"WAIT\",\n" +
                        "  \"confidence\": \"high\" or \"medium\" or \"low\",\n" +
                        "  \"reasoning\": \"2-3 sentences explaining the trend and factors\",\n" +
                        "  \"action\": \"Specific advice: when to sell, expected price range, etc.\",\n" +
                        "  \"data_source\": \"Where you found the price data\"\n" +
                        "}\n\n" +
                        "IMPORTANT:\n" +
                        "- Use REAL current market data from your search\n" +
                        "- If exact data unavailable, use best available recent data and mention it\n" +
                        "- Consider Indian market conditions and seasonal patterns\n" +
                        "- Provide actionable, practical advice for farmers\n\n" +
                        "Respond ONLY with valid JSON, no additional text.",
                crop, market, crop, market);
    }

    private void parseAndDisplayRecommendation(String response) {
        try {
            // Extract JSON from response
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            JSONObject json = new JSONObject(jsonStr);

            String currentPrice = json.optString("current_price", "Price data unavailable");
            String trend = json.getString("trend");
            String trendPercentage = json.optString("trend_percentage", "");
            String recommendation = json.getString("recommendation");
            String reasoning = json.getString("reasoning");
            String action = json.getString("action");
            String dataSource = json.optString("data_source", "");

            // Display results
            tvTrend.setText("📊 Current: " + currentPrice + "\n" +
                    "📈 Trend: " + trend.toUpperCase() + " " + trendPercentage);
            tvRecommendation.setText(recommendation);
            tvReasoning.setText(reasoning);
            tvAction.setText("💡 " + action);

            if (!dataSource.isEmpty()) {
                tvAction.append("\n\n📌 Source: " + dataSource);
            }

            // Set colors based on recommendation
            if (recommendation.contains("SELL")) {
                tvRecommendation.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                tvRecommendation.setTextColor(getColor(android.R.color.holo_orange_dark));
            }

            cardRecommendation.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            // Fallback: display raw response
            tvTrend.setText("AI Analysis Result");
            tvRecommendation.setText("Market Recommendation");
            tvReasoning.setText(response);
            tvAction.setText("Review the detailed analysis above");
            cardRecommendation.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor = null;
        }
    }
}
