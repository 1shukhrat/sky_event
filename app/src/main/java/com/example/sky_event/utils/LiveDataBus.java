package com.example.sky_event.utils;

import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.Map;

public class LiveDataBus {

    private static LiveDataBus instance;
    private final Map<String, MutableLiveData<Object>> bus;

    private LiveDataBus() {
        bus = new HashMap<>();
    }

    public static LiveDataBus getInstance() {
        if (instance == null) {
            synchronized (LiveDataBus.class) {
                if (instance == null) {
                    instance = new LiveDataBus();
                }
            }
        }
        return instance;
    }

    public MutableLiveData<Object> with(String key) {
        if (!bus.containsKey(key)) {
            bus.put(key, new MutableLiveData<>());
        }
        return bus.get(key);
    }
} 