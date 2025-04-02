package com.example.sky_event.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("ru"));
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", new Locale("ru"));

    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMAT.format(date);
    }

    public static String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_TIME_FORMAT.format(date);
    }

    public static String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        return TIME_FORMAT.format(date);
    }

    public static String getRelativeTimeSpan(Date date) {
        if (date == null) {
            return "";
        }

        long currentTime = System.currentTimeMillis();
        long dateTime = date.getTime();
        long diffTime = currentTime - dateTime;

        if (diffTime < 0) {
            return formatDateTime(date);
        }

        long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffTime);
        long diffHours = TimeUnit.MILLISECONDS.toHours(diffTime);
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffTime);

        if (diffMinutes < 1) {
            return "только что";
        } else if (diffMinutes < 60) {
            return formatMinutes(diffMinutes) + " назад";
        } else if (diffHours < 24) {
            return formatHours(diffHours) + " назад";
        } else if (diffDays < 7) {
            return formatDays(diffDays) + " назад";
        } else {
            return formatDate(date);
        }
    }

    private static String formatMinutes(long minutes) {
        if (minutes % 10 == 1 && minutes % 100 != 11) {
            return minutes + " минуту";
        } else if ((minutes % 10 == 2 || minutes % 10 == 3 || minutes % 10 == 4) && 
                   (minutes % 100 < 10 || minutes % 100 > 20)) {
            return minutes + " минуты";
        } else {
            return minutes + " минут";
        }
    }

    private static String formatHours(long hours) {
        if (hours % 10 == 1 && hours % 100 != 11) {
            return hours + " час";
        } else if ((hours % 10 == 2 || hours % 10 == 3 || hours % 10 == 4) && 
                   (hours % 100 < 10 || hours % 100 > 20)) {
            return hours + " часа";
        } else {
            return hours + " часов";
        }
    }

    private static String formatDays(long days) {
        if (days == 1) {
            return "вчера";
        } else if (days == 2) {
            return "позавчера";
        } else if ((days % 10 == 1) && days % 100 != 11) {
            return days + " день";
        } else if ((days % 10 == 2 || days % 10 == 3 || days % 10 == 4) && 
                   (days % 100 < 10 || days % 100 > 20)) {
            return days + " дня";
        } else {
            return days + " дней";
        }
    }
} 