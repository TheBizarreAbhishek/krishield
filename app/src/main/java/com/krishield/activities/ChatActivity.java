package com.krishield.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.IOException;
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

    private Bitmap selectedImage;

    private static final int CAMERA_PERMISSION_CODE = 100;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeViews();
        setupRecyclerView();
        setupGemini();
        setupListeners();
        setupImagePickers();

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
        geminiService = new GeminiService();
        executor = Executors.newSingleThreadExecutor();
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnAttachImage.setOnClickListener(v -> showImagePickerDialog());
        btnVoiceInput.setOnClickListener(v -> {
            Toast.makeText(this, "Voice input - coming soon!", Toast.LENGTH_SHORT).show();
        });

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
        }
    }
}
