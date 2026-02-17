package com.krishield.models;

public class Community {
    private String id;
    private String name;
    private String description;
    private int memberCount;
    private boolean isJoined;

    public Community(String id, String name, String description, int memberCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.memberCount = memberCount;
        this.isJoined = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public boolean isJoined() {
        return isJoined;
    }

    public void setJoined(boolean joined) {
        isJoined = joined;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }
}
