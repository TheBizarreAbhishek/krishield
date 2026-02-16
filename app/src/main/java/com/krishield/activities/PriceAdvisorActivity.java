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
import com.google.android.material.textfield.TextInputEditText;
import com.krishield.R;
import com.krishield.services.GeminiService;

import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PriceAdvisorActivity extends AppCompatActivity {

    private Spinner spinnerCrop;
    private TextInputEditText etCurrentPrice, etLastWeekPrice;
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
        getSupportActionBar().setTitle("Price Advisor");
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        spinnerCrop = findViewById(R.id.spinner_crop);
        etCurrentPrice = findViewById(R.id.et_current_price);
        etLastWeekPrice = findViewById(R.id.et_last_week_price);
        btnAnalyze = findViewById(R.id.btn_analyze);
        progressBar = findViewById(R.id.progress_bar);
        cardRecommendation = findViewById(R.id.card_recommendation);
        tvTrend = findViewById(R.id.tv_trend);
        tvRecommendation = findViewById(R.id.tv_recommendation);
        tvReasoning = findViewById(R.id.tv_reasoning);
        tvAction = findViewById(R.id.tv_action);

        // Setup crop spinner
        setupCropSpinner();

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

    private void analyzePriceTrend() {
        // Validate inputs
        String crop = spinnerCrop.getSelectedItem().toString();
        String currentPriceStr = etCurrentPrice.getText().toString().trim();
        String lastWeekPriceStr = etLastWeekPrice.getText().toString().trim();

        if (crop.equals("Select Crop")) {
            Toast.makeText(this, "Please select a crop", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPriceStr.isEmpty() || lastWeekPriceStr.isEmpty()) {
            Toast.makeText(this, "Please enter both prices", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double currentPrice = Double.parseDouble(currentPriceStr);
            double lastWeekPrice = Double.parseDouble(lastWeekPriceStr);

            // Show loading
            progressBar.setVisibility(View.VISIBLE);
            btnAnalyze.setEnabled(false);
            cardRecommendation.setVisibility(View.GONE);

            // Create AI prompt
            String prompt = createAnalysisPrompt(crop, currentPrice, lastWeekPrice);

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

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid prices", Toast.LENGTH_SHORT).show();
        }
    }

    private String createAnalysisPrompt(String crop, double currentPrice, double lastWeekPrice) {
        double priceChange = currentPrice - lastWeekPrice;
        double percentChange = (priceChange / lastWeekPrice) * 100;

        return String.format(
                "You are a market analysis expert for Indian farmers.\n\n" +
                        "Analyze this price trend for %s:\n" +
                        "- Last week's price: ₹%.2f per quintal\n" +
                        "- Current price: ₹%.2f per quintal\n" +
                        "- Price change: ₹%.2f (%.1f%%)\n\n" +
                        "Provide a selling recommendation in this EXACT JSON format:\n" +
                        "{\n" +
                        "  \"trend\": \"rising\" or \"falling\" or \"stable\",\n" +
                        "  \"recommendation\": \"SELL NOW\" or \"WAIT\",\n" +
                        "  \"confidence\": \"high\" or \"medium\" or \"low\",\n" +
                        "  \"reasoning\": \"Brief explanation in 2-3 sentences\",\n" +
                        "  \"action\": \"Specific advice for the farmer\"\n" +
                        "}\n\n" +
                        "Consider:\n" +
                        "- Price trend direction\n" +
                        "- Magnitude of change\n" +
                        "- Typical seasonal patterns for %s\n" +
                        "- Market demand factors\n\n" +
                        "Respond ONLY with valid JSON, no other text.",
                crop, lastWeekPrice, currentPrice, priceChange, percentChange, crop);
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

            String trend = json.getString("trend");
            String recommendation = json.getString("recommendation");
            String reasoning = json.getString("reasoning");
            String action = json.getString("action");

            // Display results
            tvTrend.setText("Trend: " + trend.toUpperCase());
            tvRecommendation.setText(recommendation);
            tvReasoning.setText(reasoning);
            tvAction.setText("💡 " + action);

            // Set colors based on recommendation
            if (recommendation.contains("SELL")) {
                tvRecommendation.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                tvRecommendation.setTextColor(getColor(android.R.color.holo_orange_dark));
            }

            cardRecommendation.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            // Fallback: display raw response
            tvTrend.setText("Analysis Result");
            tvRecommendation.setText("AI Recommendation");
            tvReasoning.setText(response);
            tvAction.setText("Review the analysis above");
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
