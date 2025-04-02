package com.example.sky_event.models;

public class DataLoadingStatus {
    private boolean isComplete;
    private String message;
    private int progress;
    
    public DataLoadingStatus(boolean isComplete, String message, int progress) {
        this.isComplete = isComplete;
        this.message = message;
        this.progress = progress;
    }
    
    public boolean isComplete() {
        return isComplete;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public static DataLoadingStatus loading(String message, int progress) {
        return new DataLoadingStatus(false, message, progress);
    }
    
    public static DataLoadingStatus complete() {
        return new DataLoadingStatus(true, "Загрузка завершена", 100);
    }
    
    public static DataLoadingStatus complete(String message, int progress) {
        return new DataLoadingStatus(true, message, progress);
    }
    
    public static DataLoadingStatus error(String errorMessage) {
        return new DataLoadingStatus(false, "Ошибка: " + errorMessage, 0);
    }
} 