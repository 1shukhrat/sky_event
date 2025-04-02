package com.example.sky_event.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sky_event.models.notification.Notification;
import com.example.sky_event.models.user.UserProfile;
import com.example.sky_event.repositories.NotificationRepository;
import com.example.sky_event.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FirebaseAuth firebaseAuth;

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> notificationsLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository();
        notificationRepository = new NotificationRepository();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public LiveData<List<Notification>> getNotifications() {
        return notifications;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getNotificationsLoading() {
        return notificationsLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void fetchUserProfile() {
        String userId = getCurrentUserId();
        if (userId == null) {
            error.setValue("Пользователь не авторизован");
            return;
        }

        isLoading.setValue(true);
        userRepository.getUserProfile(userId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                userProfile.setValue(profile);
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                error.setValue("Ошибка при загрузке профиля: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void fetchUserNotifications() {
        String userId = getCurrentUserId();
        if (userId == null) {
            error.setValue("Пользователь не авторизован");
            return;
        }

        notificationsLoading.setValue(true);
        notificationRepository.getUserNotifications(userId, new NotificationRepository.NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> notificationList) {
                notifications.setValue(notificationList);
                notificationsLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                error.setValue("Ошибка при загрузке уведомлений: " + e.getMessage());
                notificationsLoading.setValue(false);
            }
        });
    }

    public void markNotificationAsRead(String notificationId) {
        String userId = getCurrentUserId();
        if (userId == null || notificationId == null) {
            return;
        }

        notificationRepository.markNotificationAsRead(userId, notificationId, new NotificationRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                List<Notification> currentList = notifications.getValue();
                if (currentList != null) {
                    for (Notification notification : currentList) {
                        if (notification.getId().equals(notificationId)) {
                            notification.setRead(true);
                            break;
                        }
                    }
                    notifications.setValue(currentList);
                }
            }

            @Override
            public void onError(Exception e) {
                error.setValue("Ошибка при обновлении статуса уведомления: " + e.getMessage());
            }
        });
    }

    private String getCurrentUserId() {
        if (firebaseAuth.getCurrentUser() != null) {
            return firebaseAuth.getCurrentUser().getUid();
        }
        return null;
    }
} 