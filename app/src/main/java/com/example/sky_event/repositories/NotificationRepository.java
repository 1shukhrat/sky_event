package com.example.sky_event.repositories;

import com.example.sky_event.models.notification.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRepository {

    private final FirebaseFirestore firestore;
    private static final String NOTIFICATIONS_COLLECTION = "notifications";

    public interface NotificationsCallback {
        void onSuccess(List<Notification> notifications);
        void onError(Exception e);
    }

    public interface TaskCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public NotificationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void getUserNotifications(String userId, NotificationsCallback callback) {
        firestore.collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Notification notification = document.toObject(Notification.class);
                        if (notification != null) {
                            notification.setId(document.getId());
                            notifications.add(notification);
                        }
                    }
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    public void markNotificationAsRead(String userId, String notificationId, TaskCallback callback) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("read", true);

        firestore.collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e));
    }

    public void addNotification(Notification notification, TaskCallback callback) {
        firestore.collection(NOTIFICATIONS_COLLECTION)
                .add(notification)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e));
    }

    public void deleteNotification(String notificationId, TaskCallback callback) {
        firestore.collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e));
    }

    public void deleteAllUserNotifications(String userId, TaskCallback callback) {
        firestore.collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                    int totalDocuments = documents.size();
                    int[] deletedCount = {0};
                    int[] errorCount = {0};

                    for (DocumentSnapshot document : documents) {
                        firestore.collection(NOTIFICATIONS_COLLECTION)
                                .document(document.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    deletedCount[0]++;
                                    checkCompletion(deletedCount[0], errorCount[0], totalDocuments, callback);
                                })
                                .addOnFailureListener(e -> {
                                    errorCount[0]++;
                                    checkCompletion(deletedCount[0], errorCount[0], totalDocuments, callback);
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    private void checkCompletion(int deletedCount, int errorCount, int totalDocuments, TaskCallback callback) {
        if (deletedCount + errorCount == totalDocuments) {
            if (errorCount > 0) {
                callback.onError(new Exception("Не удалось удалить " + errorCount + " уведомлений"));
            } else {
                callback.onSuccess();
            }
        }
    }
} 