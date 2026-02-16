package com.krishield.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.krishield.R;
import com.krishield.adapters.ChatAdapter;
import com.krishield.models.ChatMessage;
import com.krishield.services.GeminiService;
import com.krishield.utils.TextToSpeechHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private MaterialButton btnSend, btnAttachImage, btnVoiceInput;
    private ImageView imagePreview;
    private ProgressBar progressBar;

    private ChatAdapter chatAdapter;
    private GeminiService geminiService;
    private Executor executor;
    private TextToSpeechHelper ttsHelper;

    private Bitmap selectedImage;
    private boolean isVoiceMode = false;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int GALLERY_PERMISSION_CODE = 101;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 102;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> voiceLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeViews();
        setupRecyclerView();
        setupGemini();
        setupListeners();
        setupImagePickers();
        setupVoiceRecognition();

        // Check if voice mode was requested
        String mode = getIntent().getStringExtra("mode");
        if ("voice".equals(mode)) {
            // TODO: Implement voice input
            Toast.makeText(this, "Voice mode - coming soon!", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttachImage = findViewById(R.id.btnAttachImage);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        imagePreview = findViewById(R.id.imagePreview);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(chatAdapter);

        // Add welcome message
        chatAdapter.addMessage(new ChatMessage(
                "Hello! I'm KriShield AI, your farming assistant. How can I help you today?",
                ChatMessage.MessageType.AI));
    }

    private void setupGemini() {
        String customApiKey = SettingsActivity.getSavedApiKey(this);
        geminiService = new GeminiService(customApiKey);
        executor = Executors.newSingleThreadExecutor();

        // Initialize TTS for voice responses
        ttsHelper = new TextToSpeechHelper(this, () -> {
            // When speech completes, re-enable voice input if in voice mode
            if (isVoiceMode) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    startVoiceRecognition();
                }, 500);
            }
        });
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnAttachImage.setOnClickListener(v -> showImagePickerDialog());
        btnVoiceInput.setOnClickListener(v -> startVoiceRecognition());

        editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void setupImagePickers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        selectedImage = (Bitmap) extras.get("data");
                        showImagePreview();
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            showImagePreview();
                        } catch (IOException e) {
                            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupVoiceRecognition() {
        voiceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            editTextMessage.setText(spokenText);
                            editTextMessage.setSelection(spokenText.length());

                            // Auto-send after voice input
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                isVoiceMode = true; // Enable voice mode for response
                                sendMessage();
                            }, 500); // Small delay for visual feedback
                        }
                    }
                });
    }

    private void startVoiceRecognition() {
        // Check for microphone permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO }, RECORD_AUDIO_PERMISSION_CODE);
            return;
        }

        // Create voice recognition intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Support multiple languages for Indian farmers
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN"); // Hindi
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your farming question...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        try {
            voiceLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice recognition not supported on this device",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePickerDialog() {
        String[] options = { "Take Photo", "Choose from Gallery" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_CODE);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(cameraIntent);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void showImagePreview() {
        imagePreview.setImageBitmap(selectedImage);
        imagePreview.setVisibility(View.VISIBLE);
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();

        if (messageText.isEmpty() && selectedImage == null) {
            return;
        }

        // Add user message
        ChatMessage userMessage = new ChatMessage(
                messageText.isEmpty() ? "Analyze this image" : messageText,
                ChatMessage.MessageType.USER,
                selectedImage);
        chatAdapter.addMessage(userMessage);
        scrollToBottom();

        // Clear input
        editTextMessage.setText("");

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        // Add placeholder AI message
        chatAdapter.addMessage(new ChatMessage("Thinking...", ChatMessage.MessageType.AI));
        scrollToBottom();

        // Send to Gemini
        if (selectedImage != null) {
            geminiService.analyzeImage(selectedImage, messageText, executor, new GeminiService.ResponseCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        chatAdapter.updateLastMessage(response);
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        scrollToBottom();

                        // Speak the response if in voice mode
                        if (isVoiceMode && ttsHelper != null) {
                            ttsHelper.speak(response);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        chatAdapter.updateLastMessage("Sorry, I encountered an error: " + error);
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        scrollToBottom();
                    });
                }
            });

            // Clear image
            selectedImage = null;
            imagePreview.setVisibility(View.GONE);
        } else {
            geminiService.sendTextMessage(messageText, executor, new GeminiService.ResponseCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        chatAdapter.updateLastMessage(response);
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        scrollToBottom();

                        // Speak the response if in voice mode
                        if (isVoiceMode && ttsHelper != null) {
                            ttsHelper.speak(response);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        chatAdapter.updateLastMessage("Sorry, I encountered an error: " + error);
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        scrollToBottom();
                    });
                }
            });
        }
    }

    private void scrollToBottom() {
        recyclerViewMessages.post(() -> recyclerViewMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
    }
}
