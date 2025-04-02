package com.example.sky_event.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionManager {
    private static final String TAG = "PermissionManager";
    
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 102;
    public static final int CALENDAR_PERMISSION_REQUEST_CODE = 103;
    public static final int ALL_PERMISSIONS_REQUEST_CODE = 100;
    
    public static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    public static final String[] STORAGE_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    public static final String[] CALENDAR_PERMISSIONS = {
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
    };
    
    public static boolean hasLocationPermissions(@NonNull Context context) {
        return hasPermissions(context, LOCATION_PERMISSIONS);
    }
    
    public static boolean hasStoragePermissions(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return hasPermissions(context, STORAGE_PERMISSIONS);
    }
    
    public static boolean hasCalendarPermissions(@NonNull Context context) {
        return hasPermissions(context, CALENDAR_PERMISSIONS);
    }
    
    public static boolean hasAllRequiredPermissions(@NonNull Context context) {
        return hasLocationPermissions(context) && 
               hasStoragePermissions(context) && 
               hasCalendarPermissions(context);
    }
    
    public static boolean hasPermissions(@NonNull Context context, @NonNull String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    public static List<String> getMissingPermissions(@NonNull Context context) {
        List<String> missingPermissions = new ArrayList<>();
        
        if (!hasLocationPermissions(context)) {
            missingPermissions.addAll(Arrays.asList(LOCATION_PERMISSIONS));
        }
        
        if (!hasStoragePermissions(context) && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            missingPermissions.addAll(Arrays.asList(STORAGE_PERMISSIONS));
        }
        
        if (!hasCalendarPermissions(context)) {
            missingPermissions.addAll(Arrays.asList(CALENDAR_PERMISSIONS));
        }
        
        return missingPermissions;
    }
    
    public static void requestAllPermissions(@NonNull Activity activity) {
        List<String> missingPermissions = getMissingPermissions(activity);
        
        if (!missingPermissions.isEmpty()) {
            Log.d(TAG, "Запрашиваем разрешения: " + Arrays.toString(missingPermissions.toArray()));
            ActivityCompat.requestPermissions(activity, 
                    missingPermissions.toArray(new String[0]), 
                    ALL_PERMISSIONS_REQUEST_CODE);
        }
    }
    
    public static boolean shouldShowRationale(@NonNull Activity activity, @NonNull String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }
} 