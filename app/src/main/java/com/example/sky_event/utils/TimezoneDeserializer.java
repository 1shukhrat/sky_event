package com.example.sky_event.utils;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.*;

public class TimezoneDeserializer implements JsonDeserializer<ZoneId> {
    @Override
    public ZoneId deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        int timestamp = json.getAsInt();
        return ZoneOffset.ofTotalSeconds(timestamp);
    }
}