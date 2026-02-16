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
                // Vegetables (15)
                "Potato (आलू)", "Onion (प्याज)", "Tomato (टमाटर)", "Cabbage (पत्तागोभी)",
                "Cauliflower (फूलगोभी)", "Brinjal (बैंगन)", "Okra (भिंडी)", "Carrot (गाजर)",
                "Radish (मूली)", "Peas (मटर)", "Beans (बीन्स)", "Capsicum (शिमला मिर्च)",
                "Cucumber (खीरा)", "Bitter Gourd (करेला)", "Bottle Gourd (लौकी)",

                // Cereals (7)
                "Wheat (गेहूं)", "Rice (चावल)", "Maize (मक्का)", "Bajra (बाजरा)",
                "Jowar (ज्वार)", "Ragi (रागी)", "Barley (जौ)",

                // Pulses (7)
                "Arhar/Tur (अरहर)", "Moong (मूंग)", "Urad (उड़द)", "Masoor (मसूर)",
                "Chana (चना)", "Rajma (राजमा)", "Lobia (लोबिया)",

                // Oilseeds (6)
                "Groundnut (मूंगफली)", "Soybean (सोयाबीन)", "Mustard (सरसों)",
                "Sunflower (सूरजमुखी)", "Sesame (तिल)", "Safflower (कुसुम)",

                // Cash Crops (4)
                "Cotton (कपास)", "Sugarcane (गन्ना)", "Jute (जूट)", "Tobacco (तंबाकू)",

                // Spices (7)
                "Turmeric (हल्दी)", "Chilli (मिर्च)", "Coriander (धनिया)", "Cumin (जीरा)",
                "Ginger (अदरक)", "Garlic (लहसुन)", "Fenugreek (मेथी)",

                // Fruits (8)
                "Mango (आम)", "Banana (केला)", "Apple (सेब)", "Grapes (अंगूर)",
                "Orange (संतरा)", "Pomegranate (अनार)", "Papaya (पपीता)", "Guava (अमरूद)"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                crops);
        spinnerCrop.setAdapter(adapter);
    }

    private void setupMarketSpinner() {
        String[] markets = {
                "Select Market/State",

                // Major Cities (12)
                "Delhi", "Mumbai", "Bangalore", "Kolkata", "Chennai", "Hyderabad",
                "Pune", "Ahmedabad", "Jaipur", "Lucknow", "Kanpur", "Nagpur",

                // Punjab (3)
                "Punjab - Ludhiana", "Punjab - Amritsar", "Punjab - Jalandhar",

                // Haryana (3)
                "Haryana - Karnal", "Haryana - Hisar", "Haryana - Rohtak",

                // Uttar Pradesh (3)
                "Uttar Pradesh - Meerut", "Uttar Pradesh - Agra", "Uttar Pradesh - Varanasi",

                // Rajasthan (3)
                "Rajasthan - Kota", "Rajasthan - Udaipur", "Rajasthan - Jodhpur",

                // North (3)
                "Himachal Pradesh - Shimla", "Uttarakhand - Dehradun", "Jammu & Kashmir - Srinagar",

                // Gujarat (3)
                "Gujarat - Surat", "Gujarat - Rajkot", "Gujarat - Vadodara",

                // Maharashtra (4)
                "Maharashtra - Nashik", "Maharashtra - Aurangabad", "Maharashtra - Solapur", "Goa - Panaji",

                // Karnataka (3)
                "Karnataka - Mysore", "Karnataka - Hubli", "Karnataka - Belgaum",

                // Tamil Nadu (3)
                "Tamil Nadu - Coimbatore", "Tamil Nadu - Madurai", "Tamil Nadu - Salem",

                // Andhra Pradesh & Telangana (4)
                "Andhra Pradesh - Vijayawada", "Andhra Pradesh - Visakhapatnam",
                "Telangana - Warangal", "Telangana - Nizamabad",

                // Kerala (2)
                "Kerala - Kochi", "Kerala - Thiruvananthapuram",

                // East India (6)
                "West Bengal - Siliguri", "West Bengal - Durgapur",
                "Bihar - Patna", "Bihar - Muzaffarpur",
                "Odisha - Bhubaneswar", "Odisha - Cuttack",

                // Jharkhand (2)
                "Jharkhand - Ranchi", "Jharkhand - Jamshedpur",

                // Central India (5)
                "Madhya Pradesh - Indore", "Madhya Pradesh - Bhopal", "Madhya Pradesh - Jabalpur",
                "Chhattisgarh - Raipur", "Chhattisgarh - Bilaspur",

                // Northeast (4)
                "Assam - Guwahati", "Meghalaya - Shillong", "Manipur - Imphal", "Tripura - Agartala"
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

        if (market.equals("Select Market/State")) {
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
        // Get current date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy");
        String currentDate = sdf.format(new java.util.Date());

        return String.format(
                "You are an expert market analyst for Indian agriculture.\n\n" +
                        "CURRENT DATE: %s (February 2026)\n\n" +
                        "TASK: Analyze CURRENT market conditions for %s in %s and provide selling recommendation.\n\n" +
                        "CRITICAL INSTRUCTIONS:\n" +
                        "1. Search for LATEST FEBRUARY 2026 prices for %s in %s\n" +
                        "2. DO NOT use outdated 2024 or 2025 Agmarknet data\n" +
                        "3. Find CURRENT price trends for last 30 days (Jan-Feb 2026)\n" +
                        "4. Use fresh internet search: news, mandi reports, live prices\n" +
                        "5. Consider current seasonal factors and market demand\n" +
                        "6. Analyze if prices are rising, falling, or stable RIGHT NOW\n\n" +
                        "PREFERRED DATA SOURCES:\n" +
                        "✅ Recent news articles (Feb 2026)\n" +
                        "✅ Live mandi price reports\n" +
                        "✅ Agricultural news websites\n" +
                        "✅ Current government portals\n" +
                        "✅ Market intelligence reports\n\n" +
                        "AVOID:\n" +
                        "❌ Agmarknet 2024 data\n" +
                        "❌ Old historical reports\n" +
                        "❌ Outdated sources\n\n" +
                        "OUTPUT FORMAT (JSON only):\n" +
                        "{\n" +
                        "  \"current_price\": \"₹X per quintal (Feb 2026)\",\n" +
                        "  \"trend\": \"rising\" or \"falling\" or \"stable\",\n" +
                        "  \"trend_percentage\": \"+X%%\" or \"-X%%\",\n" +
                        "  \"recommendation\": \"SELL NOW\" or \"WAIT\",\n" +
                        "  \"confidence\": \"high\" or \"medium\" or \"low\",\n" +
                        "  \"reasoning\": \"2-3 sentences with CURRENT factors\",\n" +
                        "  \"action\": \"Specific advice with price targets\",\n" +
                        "  \"data_source\": \"Source with date (must be 2026)\"\n" +
                        "}\n\n" +
                        "IMPORTANT:\n" +
                        "- Use ONLY current 2026 data\n" +
                        "- If Feb 2026 unavailable, use Jan 2026 and mention it\n" +
                        "- State data freshness clearly\n" +
                        "- Provide actionable advice for selling NOW\n\n" +
                        "Respond ONLY with valid JSON.",
                currentDate, crop, market, crop, market);
    }

    private void parseAndDisplayRecommendation(String response) {
        try {
            // Extract JSON
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json"))
                jsonStr = jsonStr.substring(7);
            if (jsonStr.startsWith("```"))
                jsonStr = jsonStr.substring(3);
            if (jsonStr.endsWith("```"))
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            jsonStr = jsonStr.trim();

            JSONObject json = new JSONObject(jsonStr);

            String currentPrice = json.optString("current_price", "Price unavailable");
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

            // Set colors
            if (recommendation.contains("SELL")) {
                tvRecommendation.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                tvRecommendation.setTextColor(getColor(android.R.color.holo_orange_dark));
            }

            cardRecommendation.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            // Fallback
            tvTrend.setText("AI Analysis Result");
            tvRecommendation.setText("Market Recommendation");
            tvReasoning.setText(response);
            tvAction.setText("Review analysis above");
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
