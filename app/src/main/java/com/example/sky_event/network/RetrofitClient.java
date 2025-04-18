package com.example.sky_event.network;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.sky_event.SkyEventApplication;
import com.example.sky_event.utils.TimezoneDeserializer;
import com.example.sky_event.utils.NetworkUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RetrofitClient {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final int CACHE_SIZE = 10 * 1024 * 1024; // 10 МБ
    private static final int CONNECT_TIMEOUT = 15; // секунды
    private static final int READ_TIMEOUT = 15; // секунды
    private static final int WRITE_TIMEOUT = 15; // секунды
    
    private static RetrofitClient instance;
    private final Retrofit retrofit;

    private RetrofitClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Cache cache = new Cache(SkyEventApplication.getInstance().getCacheDir(), CACHE_SIZE);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new RetryInterceptor(3))
                .addNetworkInterceptor(provideCacheInterceptor())
                .addInterceptor(provideOfflineCacheInterceptor())
                .cache(cache)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
                
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .registerTypeAdapter(ZoneId.class, new TimezoneDeserializer())
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
    
    private Interceptor provideCacheInterceptor() {
        return chain -> {
            Response originalResponse = chain.proceed(chain.request());
            
            if (NetworkUtils.isNetworkAvailable(SkyEventApplication.getInstance())) {
                int maxAge = 60; // 1 минута кэширования
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            } else {
                int maxStale = 60 * 60 * 24 * 7; // 1 неделя
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .build();
            }
        };
    }
    
    private Interceptor provideOfflineCacheInterceptor() {
        return chain -> {
            Request request = chain.request();
            
            if (!NetworkUtils.isNetworkAvailable(SkyEventApplication.getInstance())) {
                int maxStale = 60 * 60 * 24 * 7; // 1 неделя
                request = request.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .build();
            }
            
            return chain.proceed(request);
        };
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public WeatherApi getWeatherApi() {
        return retrofit.create(WeatherApi.class);
    }
    
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        
        public RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException exception = null;
            
            int tryCount = 0;
            while (tryCount < maxRetries) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        return response;
                    } else if (response.code() >= 500) {
                        response.close();
                        tryCount++;
                        try {
                            Thread.sleep(1000 * tryCount);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        return response;
                    }
                } catch (IOException e) {
                    exception = e;
                    tryCount++;
                    if (tryCount >= maxRetries) {
                        throw exception;
                    }
                    try {
                        Thread.sleep(1000 * tryCount);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            throw exception;
        }
    }
} 