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

import com.krishield.BuildConfig;

public class GeminiService {
    private static final String TAG = "GeminiService";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    private static final String SYSTEM_INSTRUCTION = "You are KriShield AI - a specialized farming assistant for Indian farmers.\n\n"
            +
            "STRICT RULES:\n" +
            "1. ONLY answer farming-related questions (crops, diseases, weather, market prices, farming techniques, seeds, fertilizers, irrigation, pests, soil, harvesting)\n"
            +
            "2. For non-farming questions, politely decline in Hindi: 'मैं KriShield हूं और केवल खेती से जुड़े सवालों का जवाब दे सकता हूं। कृपया खेती से संबंधित प्रश्न पूछें।'\n"
            +
            "3. Keep responses CONCISE and POINT-BASED\n" +
            "4. Use bullet points (•), maximum 3-5 points\n" +
            "5. No extra information - only what's asked\n" +
            "6. Be accurate, precise, and practical\n" +
            "7. Focus on Indian farming conditions and practices\n\n" +
            "RESPONSE FORMAT:\n" +
            "• Point 1 (concise)\n" +
            "• Point 2 (concise)\n" +
            "• Point 3 (concise)\n\n" +
            "When analyzing crop images for diseases:\n" +
            "• Identify disease name\n" +
            "• List 2-3 immediate remedies using locally available resources\n" +
            "• Mention prevention tip\n\n" +
            "When analyzing prices, search for current market data and provide data-driven recommendations in bullet points.";

    private GenerativeModelFutures model;
    private String apiKey;

    /**
     * Constructor with default API key from BuildConfig
     */
    public GeminiService() {
        this(API_KEY);
    }

    /**
     * Constructor with custom API key (for user-provided keys)
     */
    public GeminiService(String customApiKey) {
        this.apiKey = (customApiKey != null && !customApiKey.isEmpty())
                ? customApiKey
                : API_KEY;
        GenerativeModel gm = new GenerativeModel("gemini-3-flash-preview", this.apiKey);
        model = GenerativeModelFutures.from(gm);
    }

    /**
     * Send a text message to Gemini and get a response
     */
    public void sendTextMessage(String userMessage, Executor executor, ResponseCallback callback) {
        try {
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
                            : "Analyze this crop image and identify any diseases or issues.")
                    +
                    "\n\nIMPORTANT INSTRUCTIONS:\n" +
                    "\n\nIMPORTANT INSTRUCTIONS:\n" +
                    "1. ANALYZE the image to the best of your ability, even if it is slightly blurry or dark.\n" +
                    "2. IDENTIFY any visible crops, diseases, pests, or nutrient deficiencies.\n" +
                    "3. PROVIDE practical remedies and solutions.\n" +
                    "4. ONLY if the image is completely black or unrecognizable, then ask to retake.\n\n" +
                    "Response format:\n" +
                    "**Analysis:** [Disease Name / Issue Identified / Healthy]\n" +
                    "**Confidence:** [High / Medium / Low]\n\n" +
                    "**Remedies:**\n" +
                    "• Step 1\n" +
                    "• Step 2\n" +
                    "• Preventative Tip";

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
     * Fetch latest government schemes in JSON format
     */
    public void getGovernmentSchemes(GeminiCallback callback) {
        String prompt = "List 10 specific government schemes for Indian farmers in JSON format. " +
                "Use this exact structure: " +
                "[{\"title\": \"Scheme Name\", \"description\": \"Short description\", \"benefits\": \"Key benefits\", \"eligibility\": \"Who can apply\", \"url\": \"Official website URL\", \"iconEmoji\": \"Emoji representing the scheme\"}]. "
                +
                "Include PM-KISAN, Fasal Bima Yojana, KCC, and soil health card schemes. " +
                "Do not add any markdown formatting like ```json or ```, just return the raw JSON array.";

        Content content = new Content.Builder()
                .addText(prompt)
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
                Log.e(TAG, "Error fetching schemes", t);
                callback.onError(t.getMessage());
            }
        }, java.util.concurrent.Executors.newSingleThreadExecutor());
    }

    public interface GeminiCallback {
        void onSuccess(String response);

        void onError(String error);
    }

    /**
     * Callback interface for responses
     */
    public interface ResponseCallback {
        void onSuccess(String response);

        void onError(String error);
    }
}
