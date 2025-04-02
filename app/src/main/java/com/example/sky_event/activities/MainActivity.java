package com.example.sky_event.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.sky_event.R;
import com.example.sky_event.SkyEventApplication;
import com.example.sky_event.databinding.ActivityMainBinding;
import com.example.sky_event.utils.NotificationPermissionUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher = 
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    if (NotificationPermissionUtils.shouldShowRequestPermissionRationale(this)) {
                        showNotificationPermissionRationale();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupNavigation();
        handleIntent(getIntent());
        checkNotificationPermission();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        Uri data = intent.getData();
        
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String path = data.getPath();
            if (path != null && path.startsWith("/event/")) {
                String eventId = path.substring(7);
                navigateToEventDetails(eventId);
            }
        } else if (intent.hasExtra("eventId") && intent.getBooleanExtra("notification", false)) {
            String eventId = intent.getStringExtra("eventId");
            navigateToEventDetails(eventId);
        }
    }

    private void navigateToEventDetails(String eventId) {
        if (eventId != null && !eventId.isEmpty()) {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            if (navController != null) {
                navController.navigate(R.id.eventDetailsFragment, args);
            }
        }
    }

    private void setupNavigation() {
        BottomNavigationView navView = binding.bottomNavigation;
        
        navController = NavHostFragment.findNavController(
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment));

        NavigationUI.setupWithNavController(navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            
            if (destinationId == R.id.weatherFragment || destinationId == R.id.eventsFragment ||
                    destinationId == R.id.profileFragment) {
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            } else {
                binding.bottomNavigation.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    private void checkNotificationPermission() {
        if (!NotificationPermissionUtils.hasNotificationPermission(this)) {
            if (NotificationPermissionUtils.shouldShowRequestPermissionRationale(this)) {
                showNotificationPermissionRationale();
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
    
    private void showNotificationPermissionRationale() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Разрешение на уведомления")
                .setMessage("Для получения важных оповещений о погоде и событиях необходимо разрешение на отправку уведомлений")
                .setPositiveButton("Разрешить", (dialog, which) -> 
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NotificationPermissionUtils.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((SkyEventApplication) getApplication()).terminateApplication();
    }
}