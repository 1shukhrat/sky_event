package com.example.sky_event.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class DateSuggestion implements Parcelable {
    private Date date;
    private double temperature;
    private double windSpeed;
    private String weatherDescription;
    private boolean isRainy;
    private double score;
    private int humidity;
    private double feelsLike;
    private double weatherScore;
    private double timeScore;

    public DateSuggestion() {
    }

    protected DateSuggestion(Parcel in) {
        long dateTime = in.readLong();
        date = dateTime != 0 ? new Date(dateTime) : null;
        temperature = in.readDouble();
        windSpeed = in.readDouble();
        weatherDescription = in.readString();
        isRainy = in.readByte() != 0;
        score = in.readDouble();
        humidity = in.readInt();
        feelsLike = in.readDouble();
        weatherScore = in.readDouble();
        timeScore = in.readDouble();
    }

    public static final Creator<DateSuggestion> CREATOR = new Creator<DateSuggestion>() {
        @Override
        public DateSuggestion createFromParcel(Parcel in) {
            return new DateSuggestion(in);
        }

        @Override
        public DateSuggestion[] newArray(int size) {
            return new DateSuggestion[size];
        }
    };

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public void setWeatherDescription(String weatherDescription) {
        this.weatherDescription = weatherDescription;
    }

    public boolean isRainy() {
        return isRainy;
    }

    public void setRainy(boolean rainy) {
        isRainy = rainy;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
    
    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(double feelsLike) {
        this.feelsLike = feelsLike;
    }

    public double getWeatherScore() {
        return weatherScore;
    }

    public void setWeatherScore(double weatherScore) {
        this.weatherScore = weatherScore;
    }

    public double getTimeScore() {
        return timeScore;
    }

    public void setTimeScore(double timeScore) {
        this.timeScore = timeScore;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(date != null ? date.getTime() : 0);
        dest.writeDouble(temperature);
        dest.writeDouble(windSpeed);
        dest.writeString(weatherDescription);
        dest.writeByte((byte) (isRainy ? 1 : 0));
        dest.writeDouble(score);
        dest.writeInt(humidity);
        dest.writeDouble(feelsLike);
        dest.writeDouble(weatherScore);
        dest.writeDouble(timeScore);
    }
} 