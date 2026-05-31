package com.example.pharmamapapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pharmamapapp.appwrite.AppwriteManager;
import com.example.pharmamapapp.appwrite.UserProfile;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.pharmamapapp.util.ImageLoader;

import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName, profileEmail, profileVerified, profilePhone;
    private TextView pharmacyNameText, pharmacyAddressText, pharmacyPhoneText, pharmacyLicenseText;
    private TextView profileAvatar;
    private ImageView profilePhoto;
    private MaterialButton logoutButton;

    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadProfile();
                    loadPharmacyInfo();
                    loadProfilePhoto();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        View header = findViewById(R.id.header);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            int headerPaddingTop = (int) (20 * getResources().getDisplayMetrics().density) + systemBars.top;
            header.setPadding(header.getPaddingLeft(), headerPaddingTop, header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });

        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        profileVerified = findViewById(R.id.profile_verified);
        profilePhone = findViewById(R.id.profile_phone);
        profileAvatar = findViewById(R.id.profile_avatar);
        profilePhoto = findViewById(R.id.profile_photo);
        pharmacyNameText = findViewById(R.id.pharmacy_name_text);
        pharmacyAddressText = findViewById(R.id.pharmacy_address_text);
        pharmacyPhoneText = findViewById(R.id.pharmacy_phone_text);
        pharmacyLicenseText = findViewById(R.id.pharmacy_license_text);
        logoutButton = findViewById(R.id.logout_button);

        logoutButton.setOnClickListener(v -> confirmLogout());

        findViewById(R.id.btn_edit_profile).setOnClickListener(v ->
                editProfileLauncher.launch(new Intent(this, EditProfileActivity.class)));

        setupBottomNav();
        loadProfile();
        loadPharmacyInfo();
        loadProfilePhoto();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_profile);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_medicines) {
                startActivity(new Intent(this, MedicineListActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void loadProfile() {
        AppwriteManager.INSTANCE.getCurrentUser(new AppwriteManager.UserCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    profileName.setText(profile.getName().isEmpty() ? getString(R.string.no_name) : profile.getName());
                    profileEmail.setText(profile.getEmail());
                    profileVerified.setText(profile.getEmailVerified()
                            ? R.string.email_verified
                            : R.string.email_not_verified);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                });
            }
        });
    }

    private void loadPharmacyInfo() {
        AppwriteManager.INSTANCE.getPrefs(new AppwriteManager.PrefsCallback() {
            @Override
            public void onSuccess(Map<String, Object> prefs) {
                runOnUiThread(() -> {
                    setTextOrDefault(profilePhone, getStr(prefs, "phone"));
                    setTextOrDefault(pharmacyNameText, getStr(prefs, "pharmacy_name"));
                    setTextOrDefault(pharmacyAddressText, getStr(prefs, "pharmacy_address"));
                    setTextOrDefault(pharmacyPhoneText, getStr(prefs, "pharmacy_phone"));
                    setTextOrDefault(pharmacyLicenseText, getStr(prefs, "pharmacy_license"));
                });
            }

            @Override
            public void onError(String message) { }
        });
    }

    private void loadProfilePhoto() {
        AppwriteManager.INSTANCE.getPrefs(new AppwriteManager.PrefsCallback() {
            @Override
            public void onSuccess(Map<String, Object> prefs) {
                runOnUiThread(() -> {
                    String photoId = getStr(prefs, "photo_file_id");
                    ImageLoader.load(photoId, profilePhoto, profileAvatar);
                });
            }

            @Override
            public void onError(String message) { }
        });
    }

    private void setTextOrDefault(TextView tv, String value) {
        if (value != null && !value.isEmpty()) {
            tv.setText(value);
            tv.setAlpha(1f);
        } else {
            tv.setText(R.string.not_set);
            tv.setAlpha(0.5f);
        }
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout, (d, w) -> doLogout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doLogout() {
        logoutButton.setEnabled(false);
        AppwriteManager.INSTANCE.logout(new AppwriteManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, R.string.logout_success, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    logoutButton.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
