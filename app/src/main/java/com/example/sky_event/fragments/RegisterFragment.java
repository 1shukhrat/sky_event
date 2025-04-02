package com.example.sky_event.fragments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.sky_event.R;
import com.example.sky_event.databinding.FragmentRegisterBinding;
import com.example.sky_event.models.user.UserPreference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {
    
    private FragmentRegisterBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRegisterButton();
        setupLoginButton();
    }
    
    private void setupRegisterButton() {
        binding.buttonRegister.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();
            String confirmPassword = binding.editTextConfirmPassword.getText().toString().trim();
            String username = binding.editTextUsername.getText().toString().trim();
            
            if (!isNetworkAvailable()) {
                Toast.makeText(requireContext(), "Нет соединения с интернетом", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (validateForm(email, password, confirmPassword, username)) {
                registerUser(email, password, username);
            }
        });
    }
    
    private void setupLoginButton() {
        binding.buttonBackToLogin.setOnClickListener(v -> {
            navigateToLogin();
        });
    }
    
    private boolean validateForm(String email, String password, String confirmPassword, String username) {
        boolean valid = true;
        
        if (TextUtils.isEmpty(email)) {
            binding.editTextEmail.setError("Введите email");
            valid = false;
        } else {
            binding.editTextEmail.setError(null);
        }
        
        if (TextUtils.isEmpty(username)) {
            binding.editTextUsername.setError("Введите имя пользователя");
            valid = false;
        } else {
            binding.editTextUsername.setError(null);
        }
        
        if (TextUtils.isEmpty(password)) {
            binding.editTextPassword.setError("Введите пароль");
            valid = false;
        } else if (password.length() < 6) {
            binding.editTextPassword.setError("Пароль должен содержать не менее 6 символов");
            valid = false;
        } else {
            binding.editTextPassword.setError(null);
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.editTextConfirmPassword.setError("Подтвердите пароль");
            valid = false;
        } else if (!password.equals(confirmPassword)) {
            binding.editTextConfirmPassword.setError("Пароли не совпадают");
            valid = false;
        } else {
            binding.editTextConfirmPassword.setError(null);
        }
        
        return valid;
    }
    
    private void registerUser(String email, String password, String username) {
        showLoading(true);
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            
                            // Установка отображаемого имени
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();
                                    
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("displayName", username);
                                        userMap.put("email", email);
                                        userMap.put("createdAt", System.currentTimeMillis());
                                        
                                        firestore.collection("users").document(user.getUid())
                                                .set(userMap)
                                                .addOnSuccessListener(aVoid -> {
                                                    UserPreference defaultPreferences = new UserPreference();
                                                    defaultPreferences.setUserId(user.getUid());
                                                    defaultPreferences.setDarkMode(false);
                                                    defaultPreferences.setNotificationsEnabled(true);
                                                    defaultPreferences.setLocationEnabled(true);
                                                    
                                                    firestore.collection("userPreferences").document(user.getUid())
                                                            .set(defaultPreferences)
                                                            .addOnSuccessListener(aVoid2 -> {
                                                                showLoading(false);
                                                                Toast.makeText(requireContext(), "Регистрация выполнена успешно", Toast.LENGTH_SHORT).show();
                                                                navigateToProfile();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                showLoading(false);
                                                                Toast.makeText(requireContext(), "Регистрация выполнена. Ошибка сохранения настроек.", Toast.LENGTH_SHORT).show();
                                                                navigateToProfile();
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    showLoading(false);
                                                    Toast.makeText(requireContext(), "Регистрация выполнена. Ошибка сохранения данных.", Toast.LENGTH_SHORT).show();
                                                    navigateToProfile();
                                                });
                                    });
                        } else {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Ошибка при создании пользователя", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(requireContext(), 
                                "Ошибка регистрации: " + task.getException().getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void navigateToProfile() {
        Navigation.findNavController(requireView()).navigate(R.id.action_registerFragment_to_profileFragment);
    }
    
    private void navigateToLogin() {
        Navigation.findNavController(requireView()).navigate(R.id.action_registerFragment_to_loginFragment);
    }
    
    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonRegister.setEnabled(!isLoading);
        binding.buttonBackToLogin.setEnabled(!isLoading);
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 