package com.krishield.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TextToSpeechHelper {
    private static final String TAG = "TextToSpeechHelper";
    private TextToSpeech tts;
    private boolean isInitialized = false;
    private SpeechCompletionListener completionListener;

    public interface SpeechCompletionListener {
        void onSpeechComplete();
    }

    public TextToSpeechHelper(Context context, SpeechCompletionListener listener) {
        this.completionListener = listener;

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Try Hindi first, fallback to English
                int result = tts.setLanguage(new Locale("hi", "IN"));

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Hindi not supported, using English");
                    tts.setLanguage(Locale.US);
                }

                // Set speech parameters for better quality
                tts.setPitch(1.0f);
                tts.setSpeechRate(0.9f); // Slightly slower for clarity

                isInitialized = true;
                Log.d(TAG, "TTS initialized successfully");

                // Set up utterance progress listener
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "Speech started");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(TAG, "Speech completed");
                        if (completionListener != null) {
                            new Handler(Looper.getMainLooper()).post(() -> completionListener.onSpeechComplete());
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "Speech error");
                    }
                });
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    public void speak(String text) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet");
            return;
        }

        if (text == null || text.isEmpty()) {
            return;
        }

        // Stop any ongoing speech
        stop();

        // Speak the text
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "KriShieldTTS");
    }

    public void stop() {
        if (tts != null && isInitialized) {
            tts.stop();
        }
    }

    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            isInitialized = false;
        }
    }

    public void setLanguage(String languageCode) {
        if (!isInitialized)
            return;

        Locale locale;
        if ("hi".equals(languageCode)) {
            locale = new Locale("hi", "IN");
        } else {
            locale = Locale.US;
        }

        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language " + languageCode + " not supported");
        }
    }
}
