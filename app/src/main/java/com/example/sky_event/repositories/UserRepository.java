package com.example.sky_event.repositories;

import com.example.sky_event.models.user.UserProfile;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore firestore;
    private static final String USERS_COLLECTION = "users";

    public interface UserProfileCallback {
        void onSuccess(UserProfile userProfile);
        void onError(Exception e);
    }

    public interface TaskCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void getUserProfile(String userId, UserProfileCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                        if (userProfile != null) {
                            userProfile.setId(documentSnapshot.getId());
                            callback.onSuccess(userProfile);
                        } else {
                            callback.onError(new Exception("Ошибка при преобразовании данных профиля"));
                        }
                    } else {
                        createDefaultUserProfile(userId, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    private void createDefaultUserProfile(String userId, UserProfileCallback callback) {
        String email = "";
        String displayName = "Пользователь";
        
        UserProfile userProfile = new UserProfile();
        userProfile.setId(userId);
        userProfile.setEmail(email);
        userProfile.setDisplayName(displayName);
        userProfile.setCreationDate(new Date());
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userProfile.getId());
        userMap.put("email", userProfile.getEmail());
        userMap.put("displayName", userProfile.getDisplayName());
        userMap.put("creationDate", userProfile.getCreationDate());
        
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> callback.onSuccess(userProfile))
                .addOnFailureListener(e -> callback.onError(e));
    }

    public void updateUserProfile(UserProfile userProfile, TaskCallback callback) {
        if (userProfile == null || userProfile.getId() == null) {
            callback.onError(new Exception("Некорректные данные пользователя"));
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", userProfile.getDisplayName());
        
        if (userProfile.getPhotoUrl() != null) {
            updates.put("photoUrl", userProfile.getPhotoUrl());
        }
        
        firestore.collection(USERS_COLLECTION)
                .document(userProfile.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e));
    }
} 