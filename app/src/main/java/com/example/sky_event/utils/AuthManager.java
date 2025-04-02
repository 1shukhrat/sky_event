package com.example.sky_event.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static AuthManager instance;
    
    private final FirebaseAuth firebaseAuth;
    private boolean isGuestMode = false;
    
    private AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
    }
    
    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }
    
    public boolean isUserAuthenticated() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        return currentUser != null && currentUser.isEmailVerified();
    }
    
    public boolean isGuestMode() {
        return isGuestMode;
    }
    
    public void setGuestMode(boolean guestMode) {
        isGuestMode = guestMode;
    }
    
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    @NonNull
    public String getUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : "";
    }
    
    @NonNull
    public String getUserDisplayName() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        }
        return "Пользователь";
    }
    
    @NonNull
    public String getUserEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            return user.getEmail();
        }
        return "";
    }
    
    public void signOut(Context context) {
        try {
            firebaseAuth.signOut();
            Log.d(TAG, "Пользователь вышел из системы");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при выходе из системы", e);
        }
    }
    
    public interface AuthStateListener {
        void onAuthStateChanged(boolean isAuthenticated);
    }
    
    public void addAuthStateListener(AuthStateListener listener) {
        firebaseAuth.addAuthStateListener(auth -> {
            boolean isAuthenticated = auth.getCurrentUser() != null;
            listener.onAuthStateChanged(isAuthenticated);
        });
    }
} 