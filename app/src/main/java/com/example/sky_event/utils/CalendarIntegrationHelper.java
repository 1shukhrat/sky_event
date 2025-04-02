package com.example.sky_event.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;

import com.example.sky_event.models.event.Event;

import java.util.TimeZone;

public class CalendarIntegrationHelper {
    
    public static void addEventToCalendar(Context context, Event event) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getDate().getTime())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getDate().getTime() + 3600 * 1000)
                .putExtra(CalendarContract.Events.TITLE, event.getName())
                .putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription())
                .putExtra(CalendarContract.Events.EVENT_LOCATION, event.getLocation())
                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
        
        context.startActivity(intent);
    }
    
    public static Uri addEventToCalendarProgrammatically(Context context, Event event) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        
        values.put(CalendarContract.Events.DTSTART, event.getDate().getTime());
        values.put(CalendarContract.Events.DTEND, event.getDate().getTime() + 3600 * 1000);
        values.put(CalendarContract.Events.TITLE, event.getName());
        values.put(CalendarContract.Events.DESCRIPTION, event.getDescription());
        values.put(CalendarContract.Events.EVENT_LOCATION, event.getLocation());
        values.put(CalendarContract.Events.CALENDAR_ID, 1);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
        return uri;
    }
    
    public static void shareEvent(Context context, Event event) {
        String shareBody = String.format(
                "Событие: %s\nДата: %s\nМесто: %s\nОписание: %s",
                event.getName(),
                DateFormatter.formatDateForDisplay(event.getDate()),
                event.getLocation(),
                event.getDescription()
        );
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Приглашение на мероприятие: " + event.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        
        context.startActivity(Intent.createChooser(shareIntent, Constants.SHARE_CHOOSER_TITLE));
    }
} 