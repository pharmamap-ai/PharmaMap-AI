package com.example.pharmamapapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pharmamapapp.appwrite.AppwriteManager;
import com.example.pharmamapapp.appwrite.UserProfile;
import com.example.pharmamapapp.model.Medicine;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private TextView welcomeText, statTotal, statLowStock, statExpiring, statValue, statExpiredCount;
    private View cardExpired;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        View header = findViewById(R.id.header);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            int headerPaddingTop = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.top;
            header.setPadding(header.getPaddingLeft(), headerPaddingTop, header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });

        welcomeText = findViewById(R.id.welcome_text);
        statTotal = findViewById(R.id.stat_total_count);
        statLowStock = findViewById(R.id.stat_low_stock_count);
        statExpiring = findViewById(R.id.stat_expiring_count);
        statValue = findViewById(R.id.stat_value);
        statExpiredCount = findViewById(R.id.stat_expired_count);
        cardExpired = findViewById(R.id.card_expired);
        progressBar = findViewById(R.id.dashboard_progress);

        findViewById(R.id.btn_add_medicine).setOnClickListener(v ->
                startActivity(new Intent(this, AddMedicineActivity.class)));
        findViewById(R.id.btn_browse).setOnClickListener(v ->
                startActivity(new Intent(this, MedicineListActivity.class)));

        setupBottomNav();
        loadUserName();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_home);
        loadStats();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_medicines) {
                startActivity(new Intent(this, MedicineListActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void loadUserName() {
        AppwriteManager.INSTANCE.getCurrentUser(new AppwriteManager.UserCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    String name = profile.getName().isEmpty() ? profile.getEmail() : profile.getName();
                    welcomeText.setText(getString(R.string.welcome_user, name));
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
                    finish();
                });
            }
        });
    }

    private void loadStats() {
        progressBar.setVisibility(View.VISIBLE);
        AppwriteManager.INSTANCE.getAllMedicines(new AppwriteManager.MedicineListCallback() {
            @Override
            public void onSuccess(List<Medicine> medicines) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStats(medicines);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statTotal.setText("--");
                    statLowStock.setText("--");
                    statExpiring.setText("--");
                    statValue.setText("--");
                });
            }
        });
    }

    private void updateStats(List<Medicine> medicines) {
        int total = medicines.size();
        int lowStock = 0;
        int expiringSoon = 0;
        int expired = 0;
        double totalValue = 0;

        for (Medicine m : medicines) {
            if (m.isLowStock()) lowStock++;
            if (m.isExpiringSoon()) expiringSoon++;
            if (m.isExpired()) expired++;
            totalValue += m.getPrice() * m.getQuantity();
        }

        statTotal.setText(String.valueOf(total));
        statLowStock.setText(String.valueOf(lowStock));
        statExpiring.setText(String.valueOf(expiringSoon));
        statValue.setText(getString(R.string.currency_format, String.format(Locale.getDefault(), "%.0f", totalValue)));

        if (expired > 0) {
            cardExpired.setVisibility(View.VISIBLE);
            statExpiredCount.setText(expired + " " + getString(R.string.stat_expired));
        } else {
            cardExpired.setVisibility(View.GONE);
        }
    }
}
