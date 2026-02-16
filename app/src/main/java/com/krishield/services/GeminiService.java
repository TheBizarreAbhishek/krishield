package com.krishield.services;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

public class GeminiService {
    private static final String TAG = "GeminiService";
    // API key is now loaded from BuildConfig (set via local.properties)
    private static final String API_KEY = com.krishield.BuildConfig.GEMINI_API_KEY;

    // Custom system prompt for farming assistance
    private static final String SYSTEM_INSTRUCTION = "You are KriShield AI, an expert agricultural assistant for Indian farmers. "
            +
            "Your role is to provide practical, region-specific farming advice including:\n" +
            "- Crop disease identification and treatment\n" +
            "- Pest management solutions\n" +
            "- Soil health recommendations\n" +
            "- Irrigation and water management\n" +
            "- Seasonal crop suggestions\n" +
            "- Organic farming methods\n" +
            "- Government scheme information\n\n" +
            "Always provide answers in simple language, considering the Indian farming context. " +
            "When analyzing crop images, identify diseases, pests, or deficiencies and suggest " +
            "immediate remedies using locally available resources.";

    private GenerativeModelFutures model;

    public GeminiService() {
        // Using latest Gemini 2.5 Flash model (GA June 2025)
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY);
        model = GenerativeModelFutures.from(gm);
    }

    /**
     * Send a text message to Gemini and get a response
     */
    public void sendTextMessage(String userMessage, Executor executor, ResponseCallback callback) {
        try {
            // Prepend system instruction to user message
            String fullPrompt = SYSTEM_INSTRUCTION + "\n\nUser: " + userMessage;

            Content content = new Content.Builder()
                    .addText(fullPrompt)
                    .build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String responseText = result.getText();
                    callback.onSuccess(responseText);
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Error generating content", t);
                    callback.onError(t.getMessage());
                }
            }, executor);

        } catch (Exception e) {
            Log.e(TAG, "Error in sendTextMessage", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * Analyze an image with optional text question
     */
    public void analyzeImage(Bitmap image, String question, Executor executor, ResponseCallback callback) {
        try {
            String fullPrompt = SYSTEM_INSTRUCTION + "\n\nUser: " +
                    (question != null && !question.isEmpty() ? question
                            : "Analyze this crop image and identify any diseases or issues.");

            Content content = new Content.Builder()
                    .addText(fullPrompt)
                    .addImage(image)
                    .build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String responseText = result.getText();
                    callback.onSuccess(responseText);
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Error analyzing image", t);
                    callback.onError(t.getMessage());
                }
            }, executor);

        } catch (Exception e) {
            Log.e(TAG, "Error in analyzeImage", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * Callback interface for responses
     */
    public interface ResponseCallback {
        void onSuccess(String response);

        void onError(String error);
    }
}
