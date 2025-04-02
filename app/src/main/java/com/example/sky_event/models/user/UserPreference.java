package com.example.sky_event.models.user;

import com.google.firebase.firestore.PropertyName;

public class UserPreference {
    private String userId;
    private boolean darkMode;
    private boolean notificationsEnabled;
    private boolean locationEnabled;
    private long updatedAt;

    public UserPreference() {
        this.userId = "";
        this.darkMode = false;
        this.notificationsEnabled = true;
        this.locationEnabled = true;
        this.updatedAt = System.currentTimeMillis();
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("darkMode")
    public boolean isDarkMode() {
        return darkMode;
    }

    @PropertyName("darkMode")
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        this.updatedAt = System.currentTimeMillis();
    }

    @PropertyName("notificationsEnabled")
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    @PropertyName("notificationsEnabled")
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
        this.updatedAt = System.currentTimeMillis();
    }

    @PropertyName("locationEnabled")
    public boolean isLocationEnabled() {
        return locationEnabled;
    }

    @PropertyName("locationEnabled")
    public void setLocationEnabled(boolean locationEnabled) {
        this.locationEnabled = locationEnabled;
        this.updatedAt = System.currentTimeMillis();
    }

    @PropertyName("updatedAt")
    public long getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updatedAt")
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
} 