package com.krishield.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.krishield.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupButtons();
    }

    private void setupButtons() {
        MaterialButton btnChatText = findViewById(R.id.btn_chat_text);
        MaterialButton btnChatVoice = findViewById(R.id.btn_chat_voice);

        btnChatText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("mode", "text");
            startActivity(intent);
        });

        btnChatVoice.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("mode", "voice");
            startActivity(intent);
        });

        // Setup other features (Weather, Schemes) - To be implemented
        // findViewById(R.id.card_weather).setOnClickListener(...)
    }
}
