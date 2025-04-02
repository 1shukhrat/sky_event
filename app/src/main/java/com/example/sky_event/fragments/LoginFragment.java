package com.example.sky_event.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.sky_event.R;
import com.example.sky_event.databinding.FragmentLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginFragment extends Fragment {
    
    private static final String TAG = "LoginFragment";
    
    private FragmentLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        
        registerGoogleSignInLauncher();
    }
    
    private void registerGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        try {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            showLoading(false);
                            handleGoogleSignInError(e);
                        }
                    } else {
                        showLoading(false);
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(requireContext(), "Вход через Google отменен", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        checkCurrentUser();
        setupLoginButton();
        setupRegisterButton();
        setupGoogleSignInButton();
    }
    
    private void checkCurrentUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToProfile();
        }
    }
    
    private void setupLoginButton() {
        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();
            
            if (!isNetworkAvailable()) {
                Toast.makeText(requireContext(), "Нет соединения с интернетом", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (validateForm(email, password)) {
                loginUser(email, password);
            }
        });
    }
    
    private void setupRegisterButton() {
        binding.buttonRegister.setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigate(R.id.action_loginFragment_to_registerFragment);
        });
    }
    
    private void setupGoogleSignInButton() {
        binding.buttonGoogleSignIn.setOnClickListener(v -> {
            if (!isNetworkAvailable()) {
                Toast.makeText(requireContext(), "Нет соединения с интернетом", Toast.LENGTH_SHORT).show();
                return;
            }
            signInWithGoogle();
        });
    }
    
    private void signInWithGoogle() {
        showLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }
    
    private void handleGoogleSignInError(ApiException e) {
        Log.w(TAG, "Google sign in failed", e);
        String errorMessage;
        
        switch (e.getStatusCode()) {
            case 7:
                errorMessage = "Ошибка подключения к серверам Google";
                break;
            case 10:
                errorMessage = "Произошла ошибка в приложении Google Play";
                break;
            case 12501:
                errorMessage = "Вход отменен пользователем";
                break;
            case 12502:
                errorMessage = "Временная ошибка. Попробуйте еще раз";
                break;
            default:
                errorMessage = "Ошибка входа через Google: " + e.getStatusCode();
        }
        
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        try {
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            updateUserProfile(user);
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(requireContext(), "Вход выполнен успешно", Toast.LENGTH_SHORT).show();
                            }
                            navigateToProfile();
                        } else {
                            if (isAdded()) {
                                Log.w(TAG, "signInWithCredential:failure", task.getException());
                                String errorMessage = "Ошибка аутентификации";
                                if (task.getException() != null) {
                                    errorMessage += ": " + task.getException().getMessage();
                                }
                                if (getContext() != null) {
                                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            showLoading(false);
            Log.e(TAG, "Ошибка при аутентификации через Google", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(requireContext(), "Ошибка аутентификации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void updateUserProfile(FirebaseUser user) {
        if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("displayName", user.getDisplayName());
                        userData.put("email", user.getEmail());
                        
                        if (!documentSnapshot.exists()) {
                            userData.put("createdAt", System.currentTimeMillis());
                        }
                        
                        userData.put("lastLoginAt", System.currentTimeMillis());
                        userData.put("provider", "google");
                        
                        if (user.getPhotoUrl() != null) {
                            userData.put("photoUrl", user.getPhotoUrl().toString());
                        }
                        
                        firestore.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "User profile updated for " + user.getUid()))
                                .addOnFailureListener(e -> 
                                        Log.w(TAG, "Error updating user profile", e));
                    });
        }
    }
    
    private boolean validateForm(String email, String password) {
        boolean valid = true;
        
        if (TextUtils.isEmpty(email)) {
            binding.editTextEmail.setError("Введите email");
            valid = false;
        } else {
            binding.editTextEmail.setError(null);
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
        
        return valid;
    }
    
    private void loginUser(String email, String password) {
        showLoading(true);
        
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(requireContext(), "Вход выполнен успешно", Toast.LENGTH_SHORT).show();
                            }
                            navigateToProfile();
                        } else {
                            if (isAdded() && getContext() != null) {
                                String errorMessage = "Ошибка входа";
                                if (task.getException() != null) {
                                    errorMessage += ": " + task.getException().getMessage();
                                }
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            showLoading(false);
            Log.e(TAG, "Ошибка входа", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(requireContext(), "Ошибка входа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void navigateToProfile() {
        try {
            if (isAdded()) {
                Navigation.findNavController(requireView()).navigate(R.id.action_loginFragment_to_profileFragment);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка навигации", e);
        }
    }
    
    private void showLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.buttonLogin.setEnabled(!isLoading);
            binding.buttonRegister.setEnabled(!isLoading);
            binding.buttonGoogleSignIn.setEnabled(!isLoading);
            binding.editTextEmail.setEnabled(!isLoading);
            binding.editTextPassword.setEnabled(!isLoading);
        }
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 