package com.krishield.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.krishield.R;
import com.krishield.services.GeminiService;

import java.util.concurrent.Executors;

public class SettingsActivity extends BaseActivity {

    private static final String PREFS_NAME = "KriShieldPrefs";
    private static final String KEY_GEMINI_API = "gemini_api_key";

    private TextInputEditText etApiKey;
    private MaterialButton btnSaveApiKey, btnTestApiKey, btnOpenApiSite;
    private TextView tvApiStatus;
    private android.widget.RadioGroup radioGroupLanguage;
    private android.widget.RadioButton radioEnglish, radioHindi;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        etApiKey = findViewById(R.id.et_api_key);
        btnSaveApiKey = findViewById(R.id.btn_save_api_key);
        btnTestApiKey = findViewById(R.id.btn_test_api_key);
        btnOpenApiSite = findViewById(R.id.btn_open_api_site);
        tvApiStatus = findViewById(R.id.tv_api_status);

        radioGroupLanguage = findViewById(R.id.radio_group_language);
        radioEnglish = findViewById(R.id.radio_english);
        radioHindi = findViewById(R.id.radio_hindi);

        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved API key
        loadApiKey();

        // Load saved Language
        loadLanguage();

        // Setup button listeners
        btnSaveApiKey.setOnClickListener(v -> saveApiKey());
        btnTestApiKey.setOnClickListener(v -> testApiKey());
        btnOpenApiSite.setOnClickListener(v -> openApiWebsite());

        radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_english) {
                setNewLocale("en");
            } else if (checkedId == R.id.radio_hindi) {
                setNewLocale("hi");
            }
        });
    }

    private void loadLanguage() {
        String currentLang = com.krishield.utils.LocaleHelper.getLanguage(this);
        if (currentLang.equals("hi")) {
            radioHindi.setChecked(true);
        } else {
            radioEnglish.setChecked(true);
        }
    }

    private void setNewLocale(String lang) {
        String currentLang = com.krishield.utils.LocaleHelper.getLanguage(this);
        if (!currentLang.equals(lang)) {
            com.krishield.utils.LocaleHelper.setLocale(this, lang);

            // Restart App to apply changes
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }

    private void loadApiKey() {
        String savedKey = prefs.getString(KEY_GEMINI_API, "");
        if (!savedKey.isEmpty()) {
            etApiKey.setText(savedKey);
            tvApiStatus.setText("✅ Status: API key saved");
            tvApiStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvApiStatus.setText("⚠️ Status: No API key set");
            tvApiStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        }
    }

    private void saveApiKey() {
        String apiKey = etApiKey.getText().toString().trim();

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please enter an API key", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to SharedPreferences
        prefs.edit().putString(KEY_GEMINI_API, apiKey).apply();

        Toast.makeText(this, "✅ API key saved successfully!", Toast.LENGTH_SHORT).show();
        tvApiStatus.setText("✅ Status: API key saved");
        tvApiStatus.setTextColor(getColor(android.R.color.holo_green_dark));
    }

    private void testApiKey() {
        String apiKey = etApiKey.getText().toString().trim();

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please enter an API key first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnTestApiKey.setEnabled(false);
        tvApiStatus.setText("🧪 Testing API key...");
        tvApiStatus.setTextColor(getColor(android.R.color.holo_blue_dark));

        // Test the API key with a simple request
        GeminiService geminiService = new GeminiService(apiKey);
        geminiService.sendTextMessage("Hello", Executors.newSingleThreadExecutor(),
                new GeminiService.ResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            Toast.makeText(SettingsActivity.this,
                                    "✅ API key is valid!", Toast.LENGTH_LONG).show();
                            tvApiStatus.setText("✅ Status: API key is valid");
                            tvApiStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                            btnTestApiKey.setEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(SettingsActivity.this,
                                    "❌ API key test failed: " + error, Toast.LENGTH_LONG).show();
                            tvApiStatus.setText("❌ Status: Invalid API key");
                            tvApiStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                            btnTestApiKey.setEnabled(true);
                        });
                    }
                });
    }

    private void openApiWebsite() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://aistudio.google.com/apikey"));
        startActivity(browserIntent);
    }

    /**
     * Get saved API key (static method for use in other activities)
     */
    public static String getSavedApiKey(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_GEMINI_API, "");
    }
}
