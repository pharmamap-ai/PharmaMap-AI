package com.example.pharmamapapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pharmamapapp.adapter.MedicineAdapter;
import com.example.pharmamapapp.appwrite.AppwriteManager;
import com.example.pharmamapapp.model.Medicine;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicineListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextInputEditText searchInput;

    private List<Medicine> allMedicines = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicine_list);

        View header = findViewById(R.id.header);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.medicine_list_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            int headerPaddingTop = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.top;
            header.setPadding(header.getPaddingLeft(), headerPaddingTop, header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });

        recyclerView = findViewById(R.id.medicines_recycler);
        progressBar = findViewById(R.id.list_progress);
        emptyText = findViewById(R.id.empty_text);
        searchInput = findViewById(R.id.search_input);

        adapter = new MedicineAdapter(medicine -> {
            Intent intent = new Intent(this, AddMedicineActivity.class);
            intent.putExtra("medicine", medicine);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupFilterChips();
        setupBottomNav();

        String incomingQuery = getIntent().getStringExtra("search_query");
        if (incomingQuery != null && !incomingQuery.isEmpty()) {
            searchInput.setText(incomingQuery);
        }

        findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(this, AddMedicineActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_medicines);
        loadMedicines();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_medicines);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_medicines) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void setupFilterChips() {
        Chip chipAll = findViewById(R.id.chip_all);
        Chip chipLowStock = findViewById(R.id.chip_low_stock);
        Chip chipExpiring = findViewById(R.id.chip_expiring);
        Chip chipExpired = findViewById(R.id.chip_expired);

        chipAll.setOnCheckedChangeListener((b, ch) -> { if (ch) { currentFilter = "all"; applyFilters(); } });
        chipLowStock.setOnCheckedChangeListener((b, ch) -> { if (ch) { currentFilter = "low_stock"; applyFilters(); } });
        chipExpiring.setOnCheckedChangeListener((b, ch) -> { if (ch) { currentFilter = "expiring"; applyFilters(); } });
        chipExpired.setOnCheckedChangeListener((b, ch) -> { if (ch) { currentFilter = "expired"; applyFilters(); } });
    }

    private void loadMedicines() {
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        AppwriteManager.INSTANCE.getAllMedicines(new AppwriteManager.MedicineListCallback() {
            @Override
            public void onSuccess(List<Medicine> medicines) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    allMedicines = medicines;
                    applyFilters();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText(message);
                });
            }
        });
    }

    private void applyFilters() {
        String query = searchInput.getText() != null
                ? searchInput.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
        List<Medicine> filtered = new ArrayList<>();

        for (Medicine m : allMedicines) {
            boolean matchesSearch = query.isEmpty()
                    || m.getName().toLowerCase(Locale.getDefault()).contains(query)
                    || m.getCode().toLowerCase(Locale.getDefault()).contains(query)
                    || m.getShelf().toLowerCase(Locale.getDefault()).contains(query)
                    || m.getCategory().toLowerCase(Locale.getDefault()).contains(query);

            boolean matchesFilter = true;
            switch (currentFilter) {
                case "low_stock": matchesFilter = m.isLowStock(); break;
                case "expiring": matchesFilter = m.isExpiringSoon(); break;
                case "expired": matchesFilter = m.isExpired(); break;
            }

            if (matchesSearch && matchesFilter) filtered.add(m);
        }

        adapter.submitList(filtered);

        if (filtered.isEmpty()) {
            emptyText.setText(allMedicines.isEmpty() ? R.string.no_medicines : R.string.no_results);
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
