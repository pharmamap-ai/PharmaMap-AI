package com.example.pharmamapapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
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
import com.example.pharmamapapp.model.Medicine;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddMedicineActivity extends AppCompatActivity {

    private TextInputEditText inputName, inputCode, inputPrice, inputQuantity;
    private TextInputEditText inputShelf, inputMinStock, inputExpiry;
    private TextInputEditText inputManufacturer, inputNotes;
    private AutoCompleteTextView inputCategory;
    private MaterialButton btnSave, btnDelete;
    private ImageView medicineImage;
    private View imagePlaceholder;

    private Medicine editingMedicine = null;
    private File tempImageFile;
    private Uri cameraUri;
    private boolean imageChanged = false;
    private String currentImageId = "";

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) processImage(uri);
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraUri != null) processImage(cameraUri);
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
        setContentView(R.layout.activity_add_medicine);

        MaterialToolbar toolbar = findViewById(R.id.add_medicine_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_medicine_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            toolbar.setPadding(toolbar.getPaddingLeft(), systemBars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            return insets;
        });
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setupCategoryDropdown();
        setupDatePicker();

        tempImageFile = new File(getCacheDir(), "medicine_temp.jpg");

        findViewById(R.id.card_medicine_image).setOnClickListener(v -> showImageSourceDialog());

        if (getIntent().hasExtra("medicine")) {
            editingMedicine = (Medicine) getIntent().getSerializableExtra("medicine");
            if (editingMedicine != null) {
                toolbar.setTitle(R.string.edit_medicine_title);
                populateFields(editingMedicine);
                btnDelete.setVisibility(View.VISIBLE);
            }
        }

        btnSave.setOnClickListener(v -> save());
        btnDelete.setOnClickListener(v -> confirmDelete());
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
        cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempImageFile);
        takePictureLauncher.launch(cameraUri);
    }

    private void processImage(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) return;
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();

            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                    Math.min(bitmap.getWidth(), 800),
                    (int) ((float) Math.min(bitmap.getWidth(), 800) / bitmap.getWidth() * bitmap.getHeight()),
                    true);

            FileOutputStream out = new FileOutputStream(tempImageFile);
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();

            medicineImage.setImageBitmap(scaled);
            medicineImage.setVisibility(View.VISIBLE);
            imagePlaceholder.setVisibility(View.GONE);
            imageChanged = true;
        } catch (Exception e) {
            Toast.makeText(this, R.string.photo_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void bindViews() {
        inputName = findViewById(R.id.input_name);
        inputCode = findViewById(R.id.input_code);
        inputPrice = findViewById(R.id.input_price);
        inputQuantity = findViewById(R.id.input_quantity);
        inputShelf = findViewById(R.id.input_shelf);
        inputMinStock = findViewById(R.id.input_min_stock);
        inputExpiry = findViewById(R.id.input_expiry);
        inputManufacturer = findViewById(R.id.input_manufacturer);
        inputNotes = findViewById(R.id.input_notes);
        inputCategory = findViewById(R.id.input_category);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        medicineImage = findViewById(R.id.medicine_image);
        imagePlaceholder = findViewById(R.id.image_placeholder);
    }

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.medicine_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);
        inputCategory.setAdapter(adapter);
    }

    private void setupDatePicker() {
        inputExpiry.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.pick_date))
                .build();
        picker.show(getSupportFragmentManager(), "EXPIRY_DATE");
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selection);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            inputExpiry.setText(sdf.format(cal.getTime()));
        });
    }

    private void populateFields(Medicine m) {
        inputName.setText(m.getName());
        inputCode.setText(m.getCode());
        inputPrice.setText(m.getPrice() > 0 ? String.valueOf(m.getPrice()) : "");
        inputQuantity.setText(String.valueOf(m.getQuantity()));
        inputShelf.setText(m.getShelf());
        inputMinStock.setText(String.valueOf(m.getMinStock()));
        inputExpiry.setText(m.getExpiryDate());
        inputManufacturer.setText(m.getManufacturer());
        inputNotes.setText(m.getNotes());
        if (!m.getCategory().isEmpty()) {
            inputCategory.setText(m.getCategory(), false);
        }

        currentImageId = m.getImageId();
        ImageLoader.load(currentImageId, medicineImage, imagePlaceholder);
    }

    private void save() {
        String name = getVal(inputName);
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        if (imageChanged && tempImageFile.exists()) {
            if (!currentImageId.isEmpty()) {
                AppwriteManager.INSTANCE.deleteFile(currentImageId, new AppwriteManager.SimpleCallback() {
                    @Override public void onSuccess() { uploadImageAndSave(name); }
                    @Override public void onError(String msg) { uploadImageAndSave(name); }
                });
            } else {
                uploadImageAndSave(name);
            }
        } else {
            saveMedicine(name, currentImageId);
        }
    }

    private void uploadImageAndSave(String name) {
        AppwriteManager.INSTANCE.uploadFile(tempImageFile, new AppwriteManager.FileUploadCallback() {
            @Override
            public void onSuccess(String fileId) {
                saveMedicine(name, fileId);
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(AddMedicineActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void saveMedicine(String name, String imageId) {
        double price = parseDouble(getVal(inputPrice));
        int quantity = parseInt(getVal(inputQuantity));
        int minStock = parseInt(getVal(inputMinStock));
        if (minStock <= 0) minStock = 5;

        Medicine medicine = new Medicine(
                editingMedicine != null ? editingMedicine.getId() : "",
                name,
                getVal(inputCode),
                price,
                quantity,
                getVal(inputShelf),
                getVal(inputCategory),
                getVal(inputExpiry),
                getVal(inputManufacturer),
                "",
                minStock,
                getVal(inputNotes),
                imageId != null ? imageId : "",
                "", ""
        );

        if (editingMedicine != null) {
            AppwriteManager.INSTANCE.updateMedicine(medicine, medicineCallback());
        } else {
            AppwriteManager.INSTANCE.addMedicine(medicine, medicineCallback());
        }
    }

    private AppwriteManager.MedicineCallback medicineCallback() {
        return new AppwriteManager.MedicineCallback() {
            @Override
            public void onSuccess(Medicine medicine) {
                runOnUiThread(() -> {
                    Toast.makeText(AddMedicineActivity.this, R.string.saved_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(AddMedicineActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        };
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteMedicine())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteMedicine() {
        if (editingMedicine == null) return;
        btnDelete.setEnabled(false);
        btnSave.setEnabled(false);

        if (!currentImageId.isEmpty()) {
            AppwriteManager.INSTANCE.deleteFile(currentImageId, new AppwriteManager.SimpleCallback() {
                @Override public void onSuccess() { deleteDoc(); }
                @Override public void onError(String msg) { deleteDoc(); }
            });
        } else {
            deleteDoc();
        }
    }

    private void deleteDoc() {
        AppwriteManager.INSTANCE.deleteMedicine(editingMedicine.getId(), new AppwriteManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(AddMedicineActivity.this, R.string.deleted_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnDelete.setEnabled(true);
                    btnSave.setEnabled(true);
                    Toast.makeText(AddMedicineActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String getVal(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String getVal(AutoCompleteTextView tv) {
        return tv.getText() != null ? tv.getText().toString().trim() : "";
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
