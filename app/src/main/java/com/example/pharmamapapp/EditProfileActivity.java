package com.example.pharmamapapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pharmamapapp.appwrite.AppwriteManager;
import com.example.pharmamapapp.util.ImageLoader;
import com.example.pharmamapapp.appwrite.UserProfile;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText inputName, inputPhone;
    private TextInputEditText inputPharmacyName, inputPharmacyAddress, inputPharmacyPhone, inputPharmacyLicense;
    private ImageView profileImage;
    private TextView profileInitials;
    private MaterialButton btnSave;

    private String currentName = "";
    private String currentPhotoFileId = "";
    private File tempPhotoFile;
    private Uri cameraUri;
    private boolean photoChanged = false;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) processPickedImage(uri);
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraUri != null) processPickedImage(cameraUri);
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        MaterialToolbar toolbar = findViewById(R.id.edit_profile_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_profile_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            toolbar.setPadding(toolbar.getPaddingLeft(), systemBars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            return insets;
        });
        toolbar.setNavigationOnClickListener(v -> finish());

        profileImage = findViewById(R.id.profile_image);
        profileInitials = findViewById(R.id.profile_initials);
        inputName = findViewById(R.id.input_name);
        inputPhone = findViewById(R.id.input_phone);
        inputPharmacyName = findViewById(R.id.input_pharmacy_name);
        inputPharmacyAddress = findViewById(R.id.input_pharmacy_address);
        inputPharmacyPhone = findViewById(R.id.input_pharmacy_phone);
        inputPharmacyLicense = findViewById(R.id.input_pharmacy_license);
        btnSave = findViewById(R.id.btn_save);

        tempPhotoFile = new File(getCacheDir(), "profile_temp.jpg");

        findViewById(R.id.btn_change_photo).setOnClickListener(v -> showImageSourceDialog());
        profileImage.setOnClickListener(v -> showImageSourceDialog());

        btnSave.setOnClickListener(v -> saveProfile());
        loadCurrentData();
    }

    private void showImageSourceDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_image_source)
                .setItems(new String[]{getString(R.string.from_camera), getString(R.string.from_gallery)}, (d, which) -> {
                    if (which == 0) checkCameraPermission();
                    else pickImageLauncher.launch("image/*");
                })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempPhotoFile);
        takePictureLauncher.launch(cameraUri);
    }

    private void processPickedImage(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) return;
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();

            int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int x = (bitmap.getWidth() - size) / 2;
            int y = (bitmap.getHeight() - size) / 2;
            Bitmap cropped = Bitmap.createBitmap(bitmap, x, y, size, size);
            Bitmap scaled = Bitmap.createScaledBitmap(cropped, 400, 400, true);

            FileOutputStream out = new FileOutputStream(tempPhotoFile);
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();

            profileImage.setImageBitmap(scaled);
            profileImage.setVisibility(ImageView.VISIBLE);
            profileInitials.setVisibility(TextView.GONE);
            photoChanged = true;
        } catch (Exception e) {
            Toast.makeText(this, R.string.photo_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCurrentData() {
        AppwriteManager.INSTANCE.getCurrentUser(new AppwriteManager.UserCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    currentName = profile.getName();
                    inputName.setText(profile.getName());
                    updateInitials(profile.getName());
                });
            }

            @Override
            public void onError(String message) { }
        });

        AppwriteManager.INSTANCE.getPrefs(new AppwriteManager.PrefsCallback() {
            @Override
            public void onSuccess(Map<String, Object> prefs) {
                runOnUiThread(() -> {
                    inputPhone.setText(getStr(prefs, "phone"));
                    inputPharmacyName.setText(getStr(prefs, "pharmacy_name"));
                    inputPharmacyAddress.setText(getStr(prefs, "pharmacy_address"));
                    inputPharmacyPhone.setText(getStr(prefs, "pharmacy_phone"));
                    inputPharmacyLicense.setText(getStr(prefs, "pharmacy_license"));

                    currentPhotoFileId = getStr(prefs, "photo_file_id");
                    ImageLoader.load(currentPhotoFileId, profileImage, profileInitials);
                });
            }

            @Override
            public void onError(String message) { }
        });
    }

    private void saveProfile() {
        String name = getVal(inputName);
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.name_required_profile, Toast.LENGTH_SHORT).show();
            return;
        }
        btnSave.setEnabled(false);

        boolean nameChanged = !name.equals(currentName);

        if (nameChanged) {
            AppwriteManager.INSTANCE.updateName(name, new AppwriteManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    handlePhotoUpload();
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            handlePhotoUpload();
        }
    }

    private void handlePhotoUpload() {
        if (photoChanged && tempPhotoFile.exists()) {
            if (!currentPhotoFileId.isEmpty()) {
                AppwriteManager.INSTANCE.deleteFile(currentPhotoFileId, new AppwriteManager.SimpleCallback() {
                    @Override public void onSuccess() { uploadNewPhoto(); }
                    @Override public void onError(String message) { uploadNewPhoto(); }
                });
            } else {
                uploadNewPhoto();
            }
        } else {
            savePrefsWithPhotoId(currentPhotoFileId);
        }
    }

    private void uploadNewPhoto() {
        android.util.Log.d("EditProfile", "Uploading photo, file size: " + tempPhotoFile.length());
        AppwriteManager.INSTANCE.uploadFile(tempPhotoFile, new AppwriteManager.FileUploadCallback() {
            @Override
            public void onSuccess(String fileId) {
                android.util.Log.d("EditProfile", "Upload success, fileId: " + fileId);
                savePrefsWithPhotoId(fileId);
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("EditProfile", "Upload error: " + message);
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this, "خطأ في رفع الصورة: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void savePrefsWithPhotoId(String photoFileId) {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("phone", getVal(inputPhone));
        prefs.put("pharmacy_name", getVal(inputPharmacyName));
        prefs.put("pharmacy_address", getVal(inputPharmacyAddress));
        prefs.put("pharmacy_phone", getVal(inputPharmacyPhone));
        prefs.put("pharmacy_license", getVal(inputPharmacyLicense));
        prefs.put("photo_file_id", photoFileId != null ? photoFileId : "");

        AppwriteManager.INSTANCE.updatePrefs(prefs, new AppwriteManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateInitials(String name) {
        if (name == null || name.isEmpty()) {
            profileInitials.setText("؟");
            return;
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) initials.append(parts[i].charAt(0));
        }
        profileInitials.setText(initials.toString());
    }

    private String getVal(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
