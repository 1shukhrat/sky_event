package com.example.sky_event.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.sky_event.R;
import com.example.sky_event.adapters.EventPreviewAdapter;
import com.example.sky_event.adapters.NotificationAdapter;
import com.example.sky_event.databinding.FragmentProfileBinding;
import com.example.sky_event.models.event.Event;
import com.example.sky_event.models.notification.Notification;
import com.example.sky_event.models.user.UserProfile;
import com.example.sky_event.repositories.NotificationRepository;
import com.example.sky_event.utils.DateUtils;
import com.example.sky_event.viewmodels.ProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ProfileFragment extends Fragment implements NotificationAdapter.NotificationClickListener {
    private static final int PICK_IMAGE_REQUEST = 1;
    
    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Uri imageUri;
    private EventPreviewAdapter eventsAdapter;
    private List<Event> userEvents = new ArrayList<>();
    private boolean dataLoaded = false;
    private String currentUserId;
    private NotificationAdapter notificationAdapter;
    
    private final String[] avatarColors = {
        "#FF5252", "#FF4081", "#E040FB", "#7C4DFF", 
        "#536DFE", "#448AFF", "#40C4FF", "#18FFFF", 
        "#64FFDA", "#69F0AE", "#B2FF59", "#EEFF41", 
        "#FFFF00", "#FFD740", "#FFAB40", "#FF6E40"
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        setupUserProfile();
        setupNotificationsRecyclerView();
        observeViewModel();
        setupListeners();
        
        viewModel.fetchUserProfile();
        viewModel.fetchUserNotifications();
        
        setupRecyclerView();
        checkCurrentUser();
        setupLogoutButton();
        setupUserAvatar();
    }
    
    private void setupUserProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            if (email != null && !email.trim().isEmpty()) {
                binding.textViewUserEmail.setText(email);
                char initial = email.charAt(0);
                binding.textViewAvatarInitial.setText(String.valueOf(initial).toUpperCase());
            }
        }
    }

    private void setupNotificationsRecyclerView() {
        notificationAdapter = new NotificationAdapter(new ArrayList<>(), this);
        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewNotifications.setAdapter(notificationAdapter);
    }

    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateUserProfileUI);
        viewModel.getNotifications().observe(getViewLifecycleOwner(), this::updateNotificationsUI);
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::updateLoadingState);
        viewModel.getNotificationsLoading().observe(getViewLifecycleOwner(), this::updateNotificationsLoadingState);
        viewModel.getError().observe(getViewLifecycleOwner(), this::handleError);
    }

    private void setupListeners() {
        binding.buttonRefreshNotifications.setOnClickListener(v -> viewModel.fetchUserNotifications());
    }

    private void updateUserProfileUI(UserProfile userProfile) {
        if (userProfile != null) {
            binding.textViewUserName.setText(userProfile.getDisplayName());
            if (userProfile.getCreationDate() != null) {
                String formattedDate = DateUtils.formatDate(userProfile.getCreationDate());
                binding.textViewCreationDate.setText(formattedDate);
            }
        }
    }

    private void updateNotificationsUI(List<Notification> notifications) {
        binding.notificationsProgressBar.setVisibility(View.GONE);
        
        if (notifications == null || notifications.isEmpty()) {
            binding.textViewNoNotifications.setVisibility(View.VISIBLE);
            binding.recyclerViewNotifications.setVisibility(View.GONE);
            return;
        }
        
        binding.textViewNoNotifications.setVisibility(View.GONE);
        binding.recyclerViewNotifications.setVisibility(View.VISIBLE);
        notificationAdapter.updateNotifications(notifications);
    }

    private void updateLoadingState(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.layoutUserInfo.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void updateNotificationsLoadingState(boolean isLoading) {
        binding.notificationsProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.textViewNoNotifications.setVisibility(View.GONE);
        
        if (isLoading) {
            binding.recyclerViewNotifications.setVisibility(View.GONE);
        }
    }

    private void handleError(String errorMessage) {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void logout() {
        firebaseAuth.signOut();
        try {
            if (isAdded() && getView() != null) {
                Navigation.findNavController(requireView()).navigate(R.id.action_profileFragment_to_loginFragment);
            }
        } catch (Exception e) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                Toast.makeText(getActivity(), "Ошибка перехода к экрану входа", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupRecyclerView() {
        eventsAdapter = new EventPreviewAdapter(userEvents, event -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getId());
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_profileFragment_to_eventDetailFragment, bundle);
        });
        
        // Удаляем или комментируем старый код, так как recyclerViewUserEvents больше не существует
        // binding.recyclerViewUserEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        // binding.recyclerViewUserEvents.setAdapter(eventsAdapter);
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadUserData(currentUser);
            loadUserEvents();
        } else {
            setupGuestView();
        }
    }
    
    private void setupGuestView() {
        binding.textViewUserName.setText("Гость");
        binding.textViewUserEmail.setText("Войдите, чтобы увидеть больше");
        setDefaultAvatar("Гость");
        binding.textViewNoEvents.setText("Доступно только авторизованным пользователям");
        binding.buttonLogout.setText("Войти");
        binding.buttonLogout.setOnClickListener(v -> {
            try {
                if (isAdded() && getView() != null) {
                    Navigation.findNavController(requireView()).navigate(R.id.action_profileFragment_to_loginFragment);
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Ошибка перехода, попробуйте еще раз", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadUserData(FirebaseUser user) {
        showLoading(true);
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        Map<String, Object> userData = documentSnapshot.getData();
                        String displayName = (String) userData.get("displayName");
                        if (!TextUtils.isEmpty(displayName)) {
                            binding.textViewUserName.setText(displayName);
                        } else if (!TextUtils.isEmpty(user.getDisplayName())) {
                            binding.textViewUserName.setText(user.getDisplayName());
                        } else {
                            binding.textViewUserName.setText("Пользователь");
                        }
                        
                        String photoUrl = (String) userData.get("photoUrl");
                        if (!TextUtils.isEmpty(photoUrl)) {
                            loadUserAvatar(photoUrl);
                        } else {
                            setDefaultAvatar(displayName);
                        }
                        
                        if (userData.containsKey("createdAt") && userData.get("createdAt") != null) {
                            Object createdAtObj = userData.get("createdAt");
                            long timestamp;
                            if (createdAtObj instanceof Long) {
                                timestamp = (Long) createdAtObj;
                            } else {
                                timestamp = System.currentTimeMillis();
                            }
                            Date creationDate = new Date(timestamp);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
                            binding.textViewCreationDate.setText(dateFormat.format(creationDate));
                        } else {
                            Date creationDate = new Date(System.currentTimeMillis());
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
                            binding.textViewCreationDate.setText(dateFormat.format(creationDate));
                            Map<String, Object> update = new HashMap<>();
                            update.put("createdAt", creationDate.getTime());
                            documentSnapshot.getReference().update(update);
                        }
          
                    } else {
                        if (!TextUtils.isEmpty(user.getDisplayName())) {
                            binding.textViewUserName.setText(user.getDisplayName());
                        } else {
                            binding.textViewUserName.setText("Пользователь");
                        }
                        setDefaultAvatar(user.getDisplayName());
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("displayName", user.getDisplayName());
                        userData.put("email", user.getEmail());
                        userData.put("createdAt", System.currentTimeMillis());
                        
                        firestore.collection("users").document(user.getUid())
                                .set(userData);
                    }
                    
                    binding.textViewUserEmail.setText(user.getEmail());
                    dataLoaded = true;
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    if (!TextUtils.isEmpty(user.getDisplayName())) {
                        binding.textViewUserName.setText(user.getDisplayName());
                    } else {
                        binding.textViewUserName.setText("Пользователь");
                    }
                    binding.textViewUserEmail.setText(user.getEmail());
                    setDefaultAvatar(user.getDisplayName());
                });
    }
    
    private void loadUserAvatar(String photoUrl) {
        Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(binding.imageViewUserAvatar);
    }
    
    private void setDefaultAvatar(String username) {
        Random random = new Random(username != null ? username.hashCode() : System.currentTimeMillis());
        String randomColor = avatarColors[random.nextInt(avatarColors.length)];
        
        binding.textViewAvatarInitial.setText(
                username != null && !username.isEmpty() 
                ? username.substring(0, 1).toUpperCase() 
                : "U");
        binding.textViewAvatarInitial.setVisibility(View.VISIBLE);
        binding.cardViewAvatarInitial.setCardBackgroundColor(android.graphics.Color.parseColor(randomColor));
        
        binding.imageViewUserAvatar.setImageResource(android.R.color.transparent);
    }
    
    private void setupWebsiteClick(String website) {
    }
    
    private void loadUserEvents() {
        if (currentUserId == null) return;
        // binding.progressBarEvents.setVisibility(View.VISIBLE);
        binding.textViewNoEvents.setVisibility(View.GONE);
        
        firestore.collection("events")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // binding.progressBarEvents.setVisibility(View.GONE);
                    userEvents.clear();
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        binding.textViewNoEvents.setVisibility(View.VISIBLE);
                        // binding.recyclerViewUserEvents.setVisibility(View.GONE);
                    } else {
                        binding.textViewNoEvents.setVisibility(View.GONE);
                        // binding.recyclerViewUserEvents.setVisibility(View.VISIBLE);
                        
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Event event = document.toObject(Event.class);
                            event.setId(document.getId());
                            userEvents.add(event);
                        }
                        
                        eventsAdapter.notifyDataSetChanged();
                        binding.textViewEventsCount.setText(String.format("Создано мероприятий: %d", userEvents.size()));
                    }
                })
                .addOnFailureListener(e -> {
                    // binding.progressBarEvents.setVisibility(View.GONE);
                    binding.textViewNoEvents.setVisibility(View.VISIBLE);
                    // binding.recyclerViewUserEvents.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка при загрузке событий: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupLogoutButton() {
        binding.buttonLogout.setOnClickListener(v -> logout());
    }
    
    private void setupUserAvatar() {
        binding.cardViewUserAvatar.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() != null) {
                showAvatarOptionsDialog();
            }
        });
    }
    
    private void showAvatarOptionsDialog() {
        String[] options = {"Выбрать из галереи", "Удалить фото", "Отмена"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Изменить фото профиля")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openGallery();
                            break;
                        case 1:
                            removeProfilePhoto();
                            break;
                        case 2:
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }
    
    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }
    
    private void removeProfilePhoto() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;
        
        showLoading(true);
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(null)
                .build();
                
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentReference userRef = firestore.collection("users").document(user.getUid());
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("photoUrl", null);
                        
                        userRef.update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    showLoading(false);
                                    setDefaultAvatar(binding.textViewUserName.getText().toString());
                                    Toast.makeText(requireContext(), "Фото профиля удалено", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == -1 && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadImage();
        }
    }
    
    private void uploadImage() {
        if (imageUri == null) return;
        
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;
        
        showLoading(true);
        StorageReference fileReference = storageReference.child("profile_images/" + user.getUid() + "/" + System.currentTimeMillis());
        
        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(uri)
                                .build();
                                
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        DocumentReference userRef = firestore.collection("users").document(user.getUid());
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("photoUrl", imageUrl);
                                        
                                        userRef.update(updates)
                                                .addOnSuccessListener(aVoid -> {
                                                    showLoading(false);
                                                    loadUserAvatar(imageUrl);
                                                    binding.textViewAvatarInitial.setVisibility(View.GONE);
                                                    Toast.makeText(requireContext(), "Фото профиля обновлено", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    showLoading(false);
                                                    Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
    public void onNotificationClick(Notification notification) {
        if (!notification.isRead()) {
            viewModel.markNotificationAsRead(notification.getId());
            notification.setRead(true);
            notificationAdapter.notifyDataSetChanged();
        }
        
        if (notification.getEventId() != null && !notification.getEventId().isEmpty()) {
            navigateToEventDetails(notification.getEventId());
        }
    }

    private void navigateToEventDetails(String eventId) {
        if (getParentFragmentManager() != null) {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_profileFragment_to_eventDetailFragment, args);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}