package com.krishield.models;

public class CommunityMessage {
    private String senderName;
    private String text;
    private long timestamp;
    private boolean isMe; // True if sent by the current user

    public CommunityMessage(String senderName, String text, long timestamp, boolean isMe) {
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
        this.isMe = isMe;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isMe() {
        return isMe;
    }
}
