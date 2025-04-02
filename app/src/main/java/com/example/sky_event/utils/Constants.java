package com.example.sky_event.utils;

public class Constants {
    public static final String OPEN_WEATHER_API_KEY = "339fbf23040d10ed9d2f8275180603b7";
    public static final String YANDEX_MAP_API_KEY = "52e257d6-cf2e-4706-91c0-5bdc5804f220";
    public static final String YANDEX_MAPS_API_KEY = YANDEX_MAP_API_KEY;
    
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    public static final int DEFAULT_ZOOM_LEVEL = 12;
    
    public static final String WEATHER_PREFERENCE = "weather_preference";
    public static final String PREF_LAST_LATITUDE = "last_latitude";
    public static final String PREF_LAST_LONGITUDE = "last_longitude";
    public static final String PREF_LAST_CITY = "last_city";
    
    public static final String NOTIFICATION_CHANNEL_ID = "weather_alerts";
    public static final String NOTIFICATION_CHANNEL_NAME = "Оповещения о погоде";
    public static final String NOTIFICATION_CHANNEL_DESC = "Уведомления о изменениях погодных условий";
    
    public static final long MIN_TIME_BETWEEN_UPDATES = 30 * 60 * 1000; // 30 минут в миллисекундах
    
    public static final String WIDGET_UPDATE_ACTION = "com.example.sky_event.WIDGET_UPDATE";
    public static final String EXTRA_WIDGET_LAT = "widget_lat";
    public static final String EXTRA_WIDGET_LON = "widget_lon";
    
    public static final String PREF_NAME = "sky_event_preferences";
    public static final String PREF_WIDGET_LOCATION = "widget_location";
    public static final String PREF_WIDGET_LAT = "widget_latitude";
    public static final String PREF_WIDGET_LON = "widget_longitude";
    
    public static final String PREF_LAST_SYNC_TIME = "last_sync_time";
    
    public static final String GOOGLE_CALENDAR_MIME_TYPE = "vnd.android.cursor.item/event";
    public static final String SHARE_CHOOSER_TITLE = "Поделиться событием";
} 