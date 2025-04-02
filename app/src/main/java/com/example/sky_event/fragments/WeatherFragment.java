package com.example.sky_event.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sky_event.R;
import com.example.sky_event.adapters.HourlyWeatherAdapter;
import com.example.sky_event.adapters.MonthlyForecastAdapter;
import com.example.sky_event.databinding.FragmentWeatherBinding;
import com.example.sky_event.viewmodels.WeatherViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WeatherFragment extends Fragment {
    private FragmentWeatherBinding binding;
    private WeatherViewModel weatherViewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private HourlyWeatherAdapter hourlyAdapter;
    private MonthlyForecastAdapter monthlyAdapter;
    private String locationDetails = "";
    private Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    
                    if (fineLocationGranted != null && fineLocationGranted || 
                            coarseLocationGranted != null && coarseLocationGranted) {
                        checkLocationServices();
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.errorView.setVisibility(View.VISIBLE);
                        binding.errorTextView.setText(R.string.error_location_permission);
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWeatherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewModel();
        setupRecyclerViews();
        binding.swipeRefreshLayout.setOnRefreshListener(this::checkLocationPermissions);
        binding.retryButton.setOnClickListener(v -> checkLocationPermissions());
        
        checkLocationPermissions();
    }

    private void setupViewModel() {
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        observeViewModel();
    }

    private void setupRecyclerViews() {
        hourlyAdapter = new HourlyWeatherAdapter();
        binding.hourlyRecyclerView.setAdapter(hourlyAdapter);
        binding.hourlyRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        
        monthlyAdapter = new MonthlyForecastAdapter();
        binding.monthlyRecyclerView.setAdapter(monthlyAdapter);
        binding.monthlyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.monthlyRecyclerView.setNestedScrollingEnabled(false);
    }

    private void checkLocationPermissions() {
        binding.errorView.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setRefreshing(false);
        
        Log.d("WeatherFragment", "Проверка разрешений на местоположение");
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED 
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            
            showLocationPermissionExplanation();
        } else {
            checkLocationServices();
        }
    }

    private void showLocationPermissionExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.location_permission_title)
                .setMessage(R.string.location_permission_explanation)
                .setPositiveButton(R.string.ok, (dialog, which) -> 
                        requestLocationPermissions())
                .create()
                .show();
    }

    private void requestLocationPermissions() {
        locationPermissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void checkLocationServices() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            showLocationServicesDialog();
        } else {
            getCurrentLocation();
        }
    }

    private void showLocationServicesDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.location_services_title)
                .setMessage(R.string.location_services_message)
                .setPositiveButton(R.string.settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> 
                        handleError(getString(R.string.error_location_disabled)))
                .create()
                .show();
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED 
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        Log.d("WeatherFragment", "Получение текущего местоположения");
        
        if (!binding.swipeRefreshLayout.isRefreshing()) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        
        try {
            CancellationToken token = new CancellationToken() {
                @NonNull
                @Override
                public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                    return this;
                }

                @Override
                public boolean isCancellationRequested() {
                    return false;
                }
            };
            
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d("WeatherFragment", "Местоположение получено: " + location.getLatitude() + ", " + location.getLongitude());
                        if (isAdded() && binding != null) {
                            fetchWeatherAndAddress(location.getLatitude(), location.getLongitude());
                        }
                    } else {
                        Log.d("WeatherFragment", "Местоположение null, пробуем последнее известное местоположение");
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(lastLocation -> {
                                    if (lastLocation != null && isAdded() && binding != null) {
                                        Log.d("WeatherFragment", "Последнее местоположение: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                                        fetchWeatherAndAddress(lastLocation.getLatitude(), lastLocation.getLongitude());
                                    } else if (isAdded() && binding != null) {
                                        Log.e("WeatherFragment", "Местоположение недоступно");
                                        handleError(getString(R.string.error_location_unavailable));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("WeatherFragment", "Ошибка получения последнего местоположения", e);
                                    if (isAdded() && binding != null) {
                                        handleError(getString(R.string.error_location_failure));
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("WeatherFragment", "Ошибка получения текущего местоположения", e);
                    if (isAdded() && binding != null) {
                        handleError(getString(R.string.error_location_failure));
                    }
                });
        } catch (SecurityException e) {
            Log.e("WeatherFragment", "Проблема с разрешениями", e);
            if (isAdded() && binding != null) {
                handleError(getString(R.string.error_location_permission));
            }
        }
    }
    
    private void fetchWeatherAndAddress(double latitude, double longitude) {
        weatherViewModel.fetchWeather(latitude, longitude);
        
        backgroundExecutor.execute(() -> {
            String address = getAddressFromLocation(latitude, longitude);
            if (isAdded() && binding != null) {
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        locationDetails = address;
                        binding.cityTextView.setText(address);
                    }
                });
            }
        });
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality();
                String countryName = address.getCountryName();
                
                if (cityName != null && !cityName.isEmpty()) {
                    return countryName != null ? cityName + ", " + countryName : cityName;
                } else if (countryName != null) {
                    return countryName;
                }
            }
        } catch (IOException e) {
            Log.e("WeatherFragment", "Ошибка получения адреса", e);
        }
        
        return "";
    }

    private void handleError(String errorMessage) {
        if (binding == null) return;
        
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.errorView.setVisibility(View.VISIBLE);
        binding.errorTextView.setText(errorMessage);
        binding.contentLayout.setVisibility(View.GONE);
    }

    private void observeViewModel() {
        weatherViewModel.getCurrentWeather().observe(getViewLifecycleOwner(), currentWeather -> {
            if (currentWeather != null && binding != null) {
                updateCurrentWeatherUI(currentWeather);
                if (!weatherViewModel.isLoading().getValue()) {
                    showContent();
                }
            }
        });
        
        weatherViewModel.getHourlyForecast().observe(getViewLifecycleOwner(), hourlyForecast -> {
            if (hourlyForecast != null && !hourlyForecast.isEmpty() && binding != null) {
                hourlyAdapter.submitList(hourlyForecast);
            }
        });
        
        weatherViewModel.getMonthlyForecast().observe(getViewLifecycleOwner(), monthlyForecast -> {
            if (monthlyForecast != null && !monthlyForecast.isEmpty() && binding != null) {
                monthlyAdapter.submitList(monthlyForecast);
            }
        });
        
        weatherViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null) {
                if (!isLoading) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    
                    if (weatherViewModel.getCurrentWeather().getValue() != null) {
                        showContent();
                    }
                }
            }
        });
        
        weatherViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty() && binding != null) {
                handleError(error);
            }
        });
    }

    private void showContent() {
        if (binding != null) {
            binding.contentLayout.setVisibility(View.VISIBLE);
            binding.errorView.setVisibility(View.GONE);
        }
    }

    private void updateCurrentWeatherUI(com.example.sky_event.models.weather.CurrentWeather weather) {
        if (binding == null) return;
        
        if (!locationDetails.isEmpty()) {
            binding.cityTextView.setText(locationDetails);
        } else {
            binding.cityTextView.setText(weather.getCity());
        }
        
        binding.temperatureTextView.setText(String.format("%d°C", weather.getTemperature()));
        binding.descriptionTextView.setText(weather.getDescription());
        binding.feelsLikeTextView.setText(getString(R.string.feels_like, weather.getFeelsLike()));
        binding.humidityTextView.setText(getString(R.string.humidity, weather.getHumidity()));
        binding.pressureTextView.setText(getString(R.string.pressure, weather.getPressure()));
        binding.windSpeedTextView.setText(getString(R.string.wind_speed, weather.getWindSpeed()));
        
        int iconResId = getWeatherIconResource(weather.getIcon());
        binding.weatherIconImageView.setImageResource(iconResId);
    }

    private int getWeatherIconResource(String iconCode) {
        switch (iconCode) {
            case "01d": return R.drawable.ic_clear_day;
            case "01n": return R.drawable.ic_clear_night;
            case "02d": return R.drawable.ic_partly_cloudy_day;
            case "02n": return R.drawable.ic_partly_cloudy_night;
            case "03d":
            case "03n": return R.drawable.ic_cloudy;
            case "04d":
            case "04n": return R.drawable.ic_cloudy;
            case "09d":
            case "09n": return R.drawable.ic_rain;
            case "10d": return R.drawable.ic_rainy_day;
            case "10n": return R.drawable.ic_rainy_night;
            case "11d":
            case "11n": return R.drawable.ic_thunderstorm;
            case "13d":
            case "13n": return R.drawable.ic_snow;
            case "50d":
            case "50n": return R.drawable.ic_fog;
            default: return R.drawable.ic_clear_day;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 