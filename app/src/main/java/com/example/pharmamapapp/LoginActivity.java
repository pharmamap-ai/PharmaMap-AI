package com.example.pharmamapapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pharmamapapp.appwrite.AppwriteManager;
import com.example.pharmamapapp.appwrite.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout nameLayout;
    private TextInputEditText nameInput, emailInput, passwordInput;
    private MaterialButton authButton, toggleButton;
    private ProgressBar progressBar;
    private TextView authSubtitle;
    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nameLayout = findViewById(R.id.name_layout);
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        authButton = findViewById(R.id.auth_button);
        toggleButton = findViewById(R.id.toggle_button);
        progressBar = findViewById(R.id.login_progress);
        authSubtitle = findViewById(R.id.auth_subtitle);

        authButton.setOnClickListener(v -> submit());
        toggleButton.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            updateMode();
        });

        checkExistingSession();
    }

    private void checkExistingSession() {
        setLoading(true);
        AppwriteManager.INSTANCE.getCurrentUser(new AppwriteManager.UserCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> goToDashboard());
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> setLoading(false));
            }
        });
    }

    private void updateMode() {
        if (isRegisterMode) {
            authSubtitle.setText(R.string.register_title);
            authButton.setText(R.string.register);
            toggleButton.setText(R.string.have_account_login);
            nameLayout.setVisibility(View.VISIBLE);
        } else {
            authSubtitle.setText(R.string.login_title);
            authButton.setText(R.string.login);
            toggleButton.setText(R.string.create_account);
            nameLayout.setVisibility(View.GONE);
        }
    }

    private void submit() {
        String email = getVal(emailInput);
        String password = getVal(passwordInput);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 8) {
            Toast.makeText(this, R.string.password_min_length, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (isRegisterMode) {
            String name = getVal(nameInput);
            if (name.isEmpty()) {
                setLoading(false);
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            AppwriteManager.INSTANCE.register(name, email, password, authCallback());
        } else {
            AppwriteManager.INSTANCE.login(email, password, authCallback());
        }
    }

    private AppwriteManager.AuthCallback authCallback() {
        return new AppwriteManager.AuthCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, R.string.auth_success, Toast.LENGTH_SHORT).show();
                    goToDashboard();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        };
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        authButton.setEnabled(!loading);
        toggleButton.setEnabled(!loading);
    }

    private String getVal(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
