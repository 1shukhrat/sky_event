package com.example.sky_event.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateFormatter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", new Locale("ru"));
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("ru"));
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));
    private static final SimpleDateFormat SHORT_TIME_FORMAT = new SimpleDateFormat("HH:mm", new Locale("ru"));
    private static final String[] WEEKDAYS_RU = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
    private static final String[] WEEKDAYS_SHORT_RU = {"Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};

    public static String formatDateForDisplay(Date date) {
        if (date == null) return "";
        return DATE_FORMAT.format(date);
    }
    
    public static String formatTimeForDisplay(Date date) {
        if (date == null) return "";
        return TIME_FORMAT.format(date);
    }
    
    public static String formatDateTimeForDisplay(Date date) {
        if (date == null) return "";
        return DATE_TIME_FORMAT.format(date);
    }
    
    public static String formatShortDate(Date date) {
        if (date == null) return "";
        return SHORT_DATE_FORMAT.format(date);
    }
    
    public static String formatShortTime(Date date) {
        if (date == null) return "";
        return SHORT_TIME_FORMAT.format(date);
    }
    
    public static String formatWeekday(Date date) {
        if (date == null) return "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        return WEEKDAYS_RU[dayOfWeek];
    }
    
    public static String formatShortWeekday(Date date) {
        if (date == null) return "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        return WEEKDAYS_SHORT_RU[dayOfWeek];
    }
    
    public static boolean isToday(Date date) {
        if (date == null) return false;
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(date);
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR);
    }
    
    public static boolean isTomorrow(Date date) {
        if (date == null) return false;
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(date);
        calendar1.add(Calendar.DAY_OF_YEAR, 1);
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR);
    }
    
    public static String getRelativeDateString(Date date) {
        if (date == null) return "";
        
        if (isToday(date)) {
            return "Сегодня, " + formatTimeForDisplay(date);
        } else if (isTomorrow(date)) {
            return "Завтра, " + formatTimeForDisplay(date);
        } else {
            return formatDateTimeForDisplay(date);
        }
    }
    
    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
} 