package com.example.sky_event.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.sky_event.R;
import com.example.sky_event.SkyEventApplication;
import com.example.sky_event.models.DataLoadingStatus;
import com.example.sky_event.utils.AuthManager;
import com.example.sky_event.utils.NetworkUtils;
import com.example.sky_event.utils.PermissionManager;
import com.example.sky_event.viewmodels.PreloadViewModel;

import java.util.List;

public class SplashActivity extends AppCompatActivity {
    
    private static final String TAG = "SplashActivity";
    private static final long MIN_SPLASH_DISPLAY_DURATION = 2200;
    private static final long MAX_SPLASH_DISPLAY_DURATION = 5000;
    
    private FrameLayout logoContainer;
    private ImageView logoImage;
    private ImageView logoOverlay;
    private TextView appNameText;
    private TextView appSlogan;
    private View topCircle;
    private View bottomCircle;
    private TextView loadingText;
    
    private PreloadViewModel preloadViewModel;
    private AuthManager authManager;
    private long startTime;
    private boolean isDataLoaded = false;
    private boolean isPermissionsChecked = false;
    private boolean shouldRequestPermissions = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        startTime = System.currentTimeMillis();
        authManager = AuthManager.getInstance();
        
        initViews();
        startAnimations();
        checkNetworkAndPermissions();
    }
    
    private void initViews() {
        logoContainer = findViewById(R.id.logo_container);
        logoImage = findViewById(R.id.logo_image);
        logoOverlay = findViewById(R.id.logo_overlay);
        appNameText = findViewById(R.id.app_name_text);
        appSlogan = findViewById(R.id.app_slogan);
        topCircle = findViewById(R.id.top_circle);
        bottomCircle = findViewById(R.id.bottom_circle);
        loadingText = findViewById(R.id.loading_text);
        
        appNameText.setAlpha(0f);
        appSlogan.setAlpha(0f);
        loadingText.setAlpha(0f);
    }
    
    private void checkNetworkAndPermissions() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNetworkErrorDialog();
            return;
        }
        
        if (!PermissionManager.hasAllRequiredPermissions(this)) {
            shouldRequestPermissions = true;
            List<String> missingPermissions = PermissionManager.getMissingPermissions(this);
            
            if (missingPermissions.isEmpty()) {
                isPermissionsChecked = true;
                setupViewModelAndStartLoading();
            } else {
                PermissionManager.requestAllPermissions(this);
            }
        } else {
            isPermissionsChecked = true;
            setupViewModelAndStartLoading();
        }
    }
    
    private void setupViewModelAndStartLoading() {
        setupViewModel();
        startDataLoading();
        
        handler.postDelayed(() -> {
            if (isDataLoaded) {
                navigateToMainActivity();
            } else {
                Log.d(TAG, "Минимальное время показа splash истекло, ожидаем загрузку данных");
            }
        }, MIN_SPLASH_DISPLAY_DURATION);
        
        handler.postDelayed(() -> {
            navigateToMainActivity();
        }, MAX_SPLASH_DISPLAY_DURATION);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionManager.ALL_PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Log.d(TAG, "Все разрешения получены");
                isPermissionsChecked = true;
                setupViewModelAndStartLoading();
            } else {
                Log.d(TAG, "Не все разрешения получены");
                
                if (PermissionManager.shouldShowRationale(this, permissions)) {
                    showPermissionRationaleDialog();
                } else {
                    Toast.makeText(this, "Для полноценной работы приложения необходимы разрешения", Toast.LENGTH_LONG).show();
                    isPermissionsChecked = true;
                    setupViewModelAndStartLoading();
                }
            }
        }
    }
    
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Необходимы разрешения")
                .setMessage("Для полноценной работы приложения необходимы разрешения на доступ к местоположению, календарю и хранилищу.")
                .setPositiveButton("Запросить снова", (dialog, which) -> {
                    PermissionManager.requestAllPermissions(this);
                })
                .setNegativeButton("Продолжить без разрешений", (dialog, which) -> {
                    isPermissionsChecked = true;
                    setupViewModelAndStartLoading();
                })
                .setCancelable(false)
                .show();
    }
    
    private void showNetworkErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Нет подключения к интернету")
                .setMessage("Для работы приложения необходимо подключение к интернету.")
                .setPositiveButton("Повторить", (dialog, which) -> {
                    checkNetworkAndPermissions();
                })
                .setNegativeButton("Продолжить в офлайн режиме", (dialog, which) -> {
                    setupViewModelAndStartLoading();
                })
                .setCancelable(false)
                .show();
    }
    
    private void setupViewModel() {
        preloadViewModel = new ViewModelProvider(this).get(PreloadViewModel.class);
        
        preloadViewModel.getLoadingStatus().observe(this, status -> {
            if (status != null) {
                updateLoadingStatus(status);
                
                if (status.isComplete()) {
                    isDataLoaded = true;
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    
                    if (elapsedTime >= MIN_SPLASH_DISPLAY_DURATION) {
                        navigateToMainActivity();
                    }
                }
            }
        });
    }
    
    private void updateLoadingStatus(DataLoadingStatus status) {
        if (loadingText != null) {
            if (status.getMessage() != null) {
                loadingText.setText(status.getMessage());
                
                if (loadingText.getAlpha() == 0f) {
                    ObjectAnimator.ofFloat(loadingText, "alpha", 0f, 1f)
                            .setDuration(500)
                            .start();
                }
            }
        }
    }
    
    private void startDataLoading() {
        if (authManager.isUserAuthenticated()) {
            updateLoadingStatus(DataLoadingStatus.loading("Пользователь авторизован: " + authManager.getUserDisplayName(), 10));
        } else {
            authManager.setGuestMode(true);
            updateLoadingStatus(DataLoadingStatus.loading("Режим гостя", 10));
        }
        
        preloadViewModel.preloadAppData();
    }
    
    private void startAnimations() {
        animateBackgroundElements();
        animateLogo();
        animateTexts();
    }
    
    private void animateBackgroundElements() {
        ObjectAnimator topCircleAnimX = ObjectAnimator.ofFloat(topCircle, "translationX", 0, -60f);
        ObjectAnimator topCircleAnimY = ObjectAnimator.ofFloat(topCircle, "translationY", 0, 60f);
        
        ObjectAnimator bottomCircleAnimX = ObjectAnimator.ofFloat(bottomCircle, "translationX", 0, 60f);
        ObjectAnimator bottomCircleAnimY = ObjectAnimator.ofFloat(bottomCircle, "translationY", 0, -60f);
        
        AnimatorSet backgroundAnimSet = new AnimatorSet();
        backgroundAnimSet.playTogether(topCircleAnimX, topCircleAnimY, bottomCircleAnimX, bottomCircleAnimY);
        backgroundAnimSet.setDuration(1500);
        backgroundAnimSet.start();
    }
    
    private void animateLogo() {
        Animation scaleAndRotate = AnimationUtils.loadAnimation(this, R.anim.scale_and_rotate);
        logoContainer.startAnimation(scaleAndRotate);
        
        ObjectAnimator overlayRotation = ObjectAnimator.ofFloat(logoOverlay, "rotation", 0f, -360f);
        overlayRotation.setDuration(2000);
        overlayRotation.start();
        
        ObjectAnimator elevation = ObjectAnimator.ofFloat(logoImage, "translationZ", 0f, 15f);
        elevation.setDuration(1000);
        elevation.start();
    }
    
    private void animateTexts() {
        ObjectAnimator appNameFadeIn = ObjectAnimator.ofFloat(appNameText, "alpha", 0f, 1f);
        ObjectAnimator appNameTranslate = ObjectAnimator.ofFloat(appNameText, "translationY", 50f, 0f);
        
        ObjectAnimator appSloganFadeIn = ObjectAnimator.ofFloat(appSlogan, "alpha", 0f, 1f);
        ObjectAnimator appSloganTranslate = ObjectAnimator.ofFloat(appSlogan, "translationY", 30f, 0f);
        
        AnimatorSet textAnimSet = new AnimatorSet();
        textAnimSet.playTogether(appNameFadeIn, appNameTranslate);
        textAnimSet.setDuration(800);
        textAnimSet.setStartDelay(700);
        
        AnimatorSet sloganAnimSet = new AnimatorSet();
        sloganAnimSet.playTogether(appSloganFadeIn, appSloganTranslate);
        sloganAnimSet.setDuration(800);
        sloganAnimSet.setStartDelay(900);
        
        textAnimSet.start();
        sloganAnimSet.start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
    
    private void navigateToMainActivity() {
        if (isFinishing() || isDestroyed()) return;
        
        Log.d(TAG, "Переход к MainActivity");
        
        SkyEventApplication.getInstance().setPreloadCompleted(true);
        
        Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(mainIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
        
        handler.removeCallbacksAndMessages(null);
    }
} 