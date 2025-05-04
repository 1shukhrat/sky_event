package com.example.sky_event.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.sky_event.R;
import com.example.sky_event.databinding.FragmentMapBinding;
import com.example.sky_event.utils.Constants;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;


public class MapFragment extends Fragment implements InputListener, Session.SearchListener {
    private FragmentMapBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private Point selectedPoint;
    private Point userLocation;
    private PlacemarkMapObject userLocationMarker;
    private PlacemarkMapObject mainMarker;
    private SearchManager searchManager;
    private Session searchSession;
    private static final float DEFAULT_ZOOM = 15.0f;
    private static boolean mapInitialized = false;
    private MapObjectCollection mapObjects;
    private MapObjectCollection userLocationCollection;
    private MapObjectCollection attractionsCollection;
    private float currentZoom = DEFAULT_ZOOM;
    private static final float MIN_ZOOM = 3.0f;
    private static final float MAX_ZOOM = 20.0f;
    private static final float ZOOM_STEP = 1.0f;
    private boolean isMapReady = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!mapInitialized) {
            MapKitFactory.initialize(requireContext());
            SearchFactory.initialize(requireContext());
            mapInitialized = true;
        }
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE);
        
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    
                    if (fineLocationGranted != null && fineLocationGranted || 
                            coarseLocationGranted != null && coarseLocationGranted) {
                        getCurrentLocation();
                    } else {
                        Toast.makeText(requireContext(), 
                                "Для определения текущего местоположения необходимы разрешения", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        MapKitFactory.getInstance().onStart();
        binding.mapView.onStart();
        
        setupMapView();
        setupButton();
        setupMapControls();
        setupSearchBar();
        
        boolean hasEventCoordinates = false;
        
        if (getArguments() != null) {
            float lat = getArguments().getFloat("latitude", 0);
            float lon = getArguments().getFloat("longitude", 0);
            
            if (lat != 0 && lon != 0) {
                selectedPoint = new Point(lat, lon);
                moveCamera(selectedPoint, DEFAULT_ZOOM);
                updateLocationText(selectedPoint);
                addMarker(selectedPoint);
                hasEventCoordinates = true;
            }
        }
        
        if (!hasEventCoordinates) {
            checkLocationPermissions();
        }
    }

    private void setupMapView() {
        binding.mapView.getMap().addInputListener(this);
        mapObjects = binding.mapView.getMap().getMapObjects().addCollection();
        userLocationCollection = binding.mapView.getMap().getMapObjects().addCollection();
        attractionsCollection = binding.mapView.getMap().getMapObjects().addCollection();
        isMapReady = true;
    }

    private void setupButton() {
        binding.buttonSelectLocation.setOnClickListener(v -> {
            if (selectedPoint != null) {
                Bundle result = new Bundle();
                result.putDouble("latitude", selectedPoint.getLatitude());
                result.putDouble("longitude", selectedPoint.getLongitude());
                result.putString("address", binding.textViewSelectedLocation.getText().toString());
                
                getParentFragmentManager().setFragmentResult("locationSelection", result);
                Navigation.findNavController(requireView()).popBackStack();
            } else {
                Toast.makeText(requireContext(), "Выберите местоположение на карте", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupMapControls() {
        binding.buttonZoomIn.setOnClickListener(v -> zoomIn());
        binding.buttonZoomOut.setOnClickListener(v -> zoomOut());
        binding.buttonMyLocation.setOnClickListener(v -> getCurrentLocation());
        binding.fabFindMe.setOnClickListener(v -> animateToCurrentLocation());
    }
    
    private void setupSearchBar() {
        binding.editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchByQuery(binding.editTextSearch.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });
        
        binding.buttonSearch.setOnClickListener(v -> {
            searchByQuery(binding.editTextSearch.getText().toString());
            hideKeyboard();
        });
    }
    
    private void searchByQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Введите название места", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        SearchOptions options = new SearchOptions();
        Point center = binding.mapView.getMap().getCameraPosition().getTarget();
        searchSession = searchManager.submit(
                query,
                Geometry.fromPoint(center),
                options,
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull Response response) {
                        if (binding == null) return;
                        
                        binding.progressBar.setVisibility(View.GONE);
                        
                        if (response.getCollection().getChildren().isEmpty()) {
                            Toast.makeText(requireContext(), "Ничего не найдено", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        Point point = response.getCollection().getChildren().get(0).getObj().getGeometry().get(0).getPoint();
                        if (point != null) {
                            selectedPoint = point;
                            moveCamera(point, currentZoom);
                            updateLocationText(point);
                            addMarker(point);
                        }
                    }
                    
                    @Override
                    public void onSearchError(@NonNull Error error) {
                        if (binding == null) return;
                        
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Ошибка при поиске", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.editTextSearch.getWindowToken(), 0);
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED 
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            binding.progressBar.setVisibility(View.VISIBLE);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        binding.progressBar.setVisibility(View.GONE);

                        if (location != null) {
                            Point currentLocation = new Point(
                                    location.getLatitude(),
                                    location.getLongitude());

                            userLocation = currentLocation;
                            moveCamera(currentLocation, currentZoom);
                            addUserLocationMarker(currentLocation);

                            if (selectedPoint == null) {
                                selectedPoint = currentLocation;
                                updateLocationText(currentLocation);
                                addMarker(currentLocation);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Ошибка получения местоположения",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void moveCamera(Point point, float zoom) {
        currentZoom = zoom;
        binding.mapView.getMap().move(
                new CameraPosition(point, zoom, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0.3f),
                null);
    }

    private void addMarker(Point point) {
        if (!isMapReady) return;

        try {
            if (mapObjects != null) {
                mapObjects.clear();
                mainMarker = mapObjects.addPlacemark(point);
                mainMarker.setIcon(
                        ImageProvider.fromResource(
                                requireContext(), R.drawable.ic_location_marker));
                mainMarker.setDraggable(true);
            }

            if (userLocation != null) {
                addUserLocationMarker(userLocation);
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Ошибка добавления маркера",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocationText(Point point) {
        binding.progressBar.setVisibility(View.VISIBLE);

        SearchOptions options = new SearchOptions();
        searchSession = searchManager.submit(
                point,
                16,
                options,
                this);
    }

    private void zoomIn() {
        if (currentZoom < MAX_ZOOM) {
            currentZoom += ZOOM_STEP;
            moveCamera(binding.mapView.getMap().getCameraPosition().getTarget(), currentZoom);
        }
    }

    private void zoomOut() {
        if (currentZoom > MIN_ZOOM) {
            currentZoom -= ZOOM_STEP;
            moveCamera(binding.mapView.getMap().getCameraPosition().getTarget(), currentZoom);
        }
    }

    private void showMapTypeSelector() {
        binding.mapView.getMap().setMapType(MapType.MAP);
    }

    private void addAttractionMarker(Point point, String name) {
        if (!isMapReady || attractionsCollection == null) return;

        try {
            PlacemarkMapObject mark = attractionsCollection.addPlacemark(point);
            mark.setIcon(
                    ImageProvider.fromResource(
                            requireContext(), R.drawable.ic_attractions));

            mark.addTapListener((mapObject, point1) -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), name, Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        } catch (Exception e) {
        }
    }


    @Override
    public void onMapTap(@NonNull Map map, @NonNull Point point) {
        selectedPoint = point;
        moveCamera(point, currentZoom);
        updateLocationText(point);
        addMarker(point);
    }

    @Override
    public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
    }

    @Override
    public void onSearchResponse(@NonNull Response response) {
        if (binding == null) return;

        binding.progressBar.setVisibility(View.GONE);

        try {
            if (response.getCollection() != null && response.getCollection().getChildren().size() > 0) {
                String displayText = response.getCollection().getChildren().get(0).getObj().getName();
                if (displayText != null && !displayText.isEmpty()) {
                    binding.textViewSelectedLocation.setText(displayText);
                    return;
                }
            }

            binding.textViewSelectedLocation.setText(
                    String.format("%f, %f",
                            selectedPoint.getLatitude(),
                            selectedPoint.getLongitude()));
        } catch (Exception e) {
            if (binding != null) {
                binding.textViewSelectedLocation.setText(
                        String.format("%f, %f",
                                selectedPoint.getLatitude(),
                                selectedPoint.getLongitude()));
            }
        }
    }

    @Override
    public void onSearchError(@NonNull Error error) {
        if (binding == null) return;

        binding.progressBar.setVisibility(View.GONE);
        binding.textViewSelectedLocation.setText(
                String.format("%f, %f",
                        selectedPoint.getLatitude(),
                        selectedPoint.getLongitude()));
    }

    @Override
    public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        binding.mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onStop();
        }
        if (MapKitFactory.getInstance() != null) {
            MapKitFactory.getInstance().onStop();
        }
    }

    @Override
    public void onDestroyView() {
        isMapReady = false;
        userLocationMarker = null;
        mainMarker = null;

        try {
            if (mapObjects != null) {
                mapObjects.clear();
            }
            if (userLocationCollection != null) {
                userLocationCollection.clear();
            }
            if (attractionsCollection != null) {
                attractionsCollection.clear();
            }
        } catch (Exception e) {
        }

        super.onDestroyView();
        binding = null;
    }

    private void animateToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            binding.progressBar.setVisibility(View.VISIBLE);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        binding.progressBar.setVisibility(View.GONE);

                        if (location != null) {
                            Point currentLocation = new Point(
                                    location.getLatitude(),
                                    location.getLongitude());

                            userLocation = currentLocation;
                            moveCamera(currentLocation, 17.0f);
                            addUserLocationMarker(currentLocation);
                            Toast.makeText(requireContext(), "Ваше текущее местоположение", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Не удалось определить местоположение", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), 
                                "Ошибка получения местоположения", 
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            checkLocationPermissions();
        }
    }

    private void addUserLocationMarker(Point point) {
        if (!isMapReady || userLocationCollection == null) return;
        
        try {
            userLocationCollection.clear();
            userLocationMarker = userLocationCollection.addPlacemark(point);
            userLocationMarker.setIcon(
                    ImageProvider.fromResource(
                            requireContext(), R.drawable.ic_person));
            userLocationMarker.setZIndex(100);
        } catch (Exception e) {
        }
    }
} 