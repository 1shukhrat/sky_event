package com.example.sky_event.models.event;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class EventLocation implements Serializable {
    private double latitude;
    private double longitude;
    private String address;

    public EventLocation() {
    }

    public EventLocation(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @NonNull
    @Override
    public String toString() {
        return address != null && !address.isEmpty() ? address : latitude + ", " + longitude;
    }
} 