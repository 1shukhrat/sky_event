package com.example.sky_event.models.event;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Event implements Parcelable {
    private String id;
    private String name;
    private String description;
    private Date date;
    private String location;
    private double latitude;
    private double longitude;
    private WeatherCondition weatherCondition;
    private String userId;
    private String category;
    private int participantsCount;
    
    @ServerTimestamp
    private Timestamp createdAt;
    
    @ServerTimestamp
    private Timestamp updatedAt;

    public Event() {
        this.id = "";
        this.weatherCondition = new WeatherCondition();
        this.category = "Другое";
        this.participantsCount = 0;
    }

    public Event(String name, String description, Date date, String location, 
                double latitude, double longitude, WeatherCondition weatherCondition, String userId) {
        this.name = name;
        this.description = description;
        this.date = date;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.weatherCondition = weatherCondition != null ? weatherCondition : new WeatherCondition();
        this.userId = userId;
        this.category = "Другое";
        this.participantsCount = 0;
    }
    
    protected Event(Parcel in) {
        try {
            id = in.readString();
            name = in.readString();
            description = in.readString();
            long dateTime = in.readLong();
            date = dateTime != 0 ? new Date(dateTime) : null;
            location = in.readString();
            latitude = in.readDouble();
            longitude = in.readDouble();
            
            android.util.Log.d("Event", "Before reading WeatherCondition from Parcel");
            ClassLoader classLoader = WeatherCondition.class.getClassLoader();
            weatherCondition = in.readParcelable(classLoader);
            
            if (weatherCondition == null) {
                android.util.Log.w("Event", "WeatherCondition was null after reading from Parcel, creating new instance");
                weatherCondition = new WeatherCondition();
            } else {
                android.util.Log.d("Event", "WeatherCondition successfully read from Parcel: " + 
                        "minTemp=" + weatherCondition.getMinTemperature() + 
                        ", maxTemp=" + weatherCondition.getMaxTemperature() + 
                        ", maxWind=" + weatherCondition.getMaxWindSpeed() + 
                        ", noRain=" + weatherCondition.isNoRain());
            }
            
            userId = in.readString();
            category = in.readString();
            participantsCount = in.readInt();
            
        } catch (Exception e) {
            android.util.Log.e("Event", "Error reading from Parcel", e);
            id = "";
            name = "";
            description = "";
            date = new Date();
            location = "";
            latitude = 0;
            longitude = 0;
            weatherCondition = new WeatherCondition();
            userId = "";
            category = "Другое";
            participantsCount = 0;
        }
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Object dateObj) {
        if (dateObj instanceof Date) {
            this.date = (Date) dateObj;
        } else if (dateObj instanceof Timestamp) {
            this.date = ((Timestamp) dateObj).toDate();
        } else if (dateObj instanceof Long) {
            this.date = new Date((Long) dateObj);
        } else if (dateObj instanceof String) {
            try {
                long timestamp = Long.parseLong((String) dateObj);
                this.date = new Date(timestamp);
            } catch (NumberFormatException e) {
                this.date = new Date();
            }
        } else {
            this.date = new Date();
        }
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public WeatherCondition getWeatherCondition() {
        if (weatherCondition == null) {
            weatherCondition = new WeatherCondition();
        }
        return weatherCondition;
    }

    public void setWeatherCondition(Object weatherConditionObj) {
        if (weatherConditionObj instanceof WeatherCondition) {
            this.weatherCondition = (WeatherCondition) weatherConditionObj;
        } else if (weatherConditionObj instanceof String) {
            this.weatherCondition = new WeatherCondition();
        } else if (weatherConditionObj instanceof Map) {
            try {
                Map<String, Object> map = (Map<String, Object>) weatherConditionObj;
                WeatherCondition condition = new WeatherCondition();
                
                if (map.containsKey("minTemperature")) {
                    Object val = map.get("minTemperature");
                    if (val instanceof Number) {
                        condition.setMinTemperature(((Number) val).doubleValue());
                    }
                }
                
                if (map.containsKey("maxTemperature")) {
                    Object val = map.get("maxTemperature");
                    if (val instanceof Number) {
                        condition.setMaxTemperature(((Number) val).doubleValue());
                    }
                }
                
                if (map.containsKey("maxWindSpeed")) {
                    Object val = map.get("maxWindSpeed");
                    if (val instanceof Number) {
                        condition.setMaxWindSpeed(((Number) val).doubleValue());
                    }
                }
                
                if (map.containsKey("noRain")) {
                    Object val = map.get("noRain");
                    if (val instanceof Boolean) {
                        condition.setNoRain((Boolean) val);
                    }
                }
                
                if (map.containsKey("allowedConditions")) {
                    Object val = map.get("allowedConditions");
                    if (val instanceof List) {
                        condition.setAllowedConditions((List<String>) val);
                    }
                }
                
                this.weatherCondition = condition;
            } catch (Exception e) {
                this.weatherCondition = new WeatherCondition();
            }
        } else {
            this.weatherCondition = new WeatherCondition();
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getParticipantsCount() {
        return participantsCount;
    }
    
    public void setParticipantsCount(int participantsCount) {
        this.participantsCount = participantsCount;
    }

    @PropertyName("createdAt")
    public Date getCreatedAt() {
        return createdAt != null ? createdAt.toDate() : null;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Object createdAtObj) {
        if (createdAtObj instanceof Timestamp) {
            this.createdAt = (Timestamp) createdAtObj;
        } else if (createdAtObj instanceof Date) {
            this.createdAt = new Timestamp((Date) createdAtObj);
        } else if (createdAtObj instanceof Long) {
            this.createdAt = new Timestamp(new Date((Long) createdAtObj));
        }
    }

    @PropertyName("updatedAt")
    public Date getUpdatedAt() {
        return updatedAt != null ? updatedAt.toDate() : null;
    }

    @PropertyName("updatedAt")
    public void setUpdatedAt(Object updatedAtObj) {
        if (updatedAtObj instanceof Timestamp) {
            this.updatedAt = (Timestamp) updatedAtObj;
        } else if (updatedAtObj instanceof Date) {
            this.updatedAt = new Timestamp((Date) updatedAtObj);
        } else if (updatedAtObj instanceof Long) {
            this.updatedAt = new Timestamp(new Date((Long) updatedAtObj));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        try {
            dest.writeString(id);
            dest.writeString(name);
            dest.writeString(description);
            dest.writeLong(date != null ? date.getTime() : 0);
            dest.writeString(location);
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
            
            if (weatherCondition == null) {
                android.util.Log.w("Event", "WeatherCondition is null in writeToParcel, creating new instance");
                weatherCondition = new WeatherCondition();
            }
            
            android.util.Log.d("Event", "Writing WeatherCondition to Parcel: " + 
                    "minTemp=" + weatherCondition.getMinTemperature() + 
                    ", maxTemp=" + weatherCondition.getMaxTemperature() + 
                    ", maxWind=" + weatherCondition.getMaxWindSpeed() + 
                    ", noRain=" + weatherCondition.isNoRain());
            
            dest.writeParcelable(weatherCondition, flags);
            dest.writeString(userId);
            dest.writeString(category);
            dest.writeInt(participantsCount);
        } catch (Exception e) {
            android.util.Log.e("Event", "Error writing to Parcel", e);
        }
    }
} 