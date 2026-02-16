package com.krishield.models;

public class Scheme {
    public String title;
    public String description;
    public String benefits;
    public String eligibility;
    public String url;
    public String iconEmoji;

    public Scheme(String title, String description, String benefits, String eligibility, String url, String iconEmoji) {
        this.title = title;
        this.description = description;
        this.benefits = benefits;
        this.eligibility = eligibility;
        this.url = url;
        this.iconEmoji = iconEmoji;
    }
}
