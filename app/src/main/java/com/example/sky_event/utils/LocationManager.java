package com.example.sky_event.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationManager {
    private static LocationManager instance;
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> locationErrorLiveData = new MutableLiveData<>();
    private LocationCallback locationCallback;
    private boolean isRequestingLocationUpdates = false;

    private LocationManager(Context context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        setupLocationCallback();
    }

    public static synchronized LocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocationManager(context.getApplicationContext());
        }
        return instance;
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    locationErrorLiveData.setValue("Невозможно получить местоположение");
                    return;
                }
                
                for (Location location : locationResult.getLocations()) {
                    locationLiveData.setValue(location);
                    break;
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates(boolean hasPermission) {
        if (!hasPermission) {
            locationErrorLiveData.setValue("Нет разрешения на получение местоположения");
            return;
        }

        if (isRequestingLocationUpdates) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(30000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(100)
                .build();

        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
        
        isRequestingLocationUpdates = true;
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            isRequestingLocationUpdates = false;
        }
    }

    @SuppressLint("MissingPermission")
    public void getLastLocation(boolean hasPermission) {
        if (!hasPermission) {
            locationErrorLiveData.setValue("Нет разрешения на получение местоположения");
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        locationLiveData.setValue(location);
                    } else {
                        startLocationUpdates(true);
                    }
                })
                .addOnFailureListener(e -> 
                        locationErrorLiveData.setValue("Ошибка получения местоположения: " + e.getMessage()));
    }

    public LiveData<Location> getLocationLiveData() {
        return locationLiveData;
    }

    public LiveData<String> getLocationErrorLiveData() {
        return locationErrorLiveData;
    }
} 