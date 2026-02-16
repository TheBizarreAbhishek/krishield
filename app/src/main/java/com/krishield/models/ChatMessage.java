package com.krishield.models;

import android.graphics.Bitmap;

public class ChatMessage {
    public enum MessageType {
        USER,
        AI,
        SYSTEM
    }

    private String text;
    private MessageType type;
    private Bitmap image;
    private long timestamp;

    public ChatMessage(String text, MessageType type) {
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String text, MessageType type, Bitmap image) {
        this.text = text;
        this.type = type;
        this.image = image;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
