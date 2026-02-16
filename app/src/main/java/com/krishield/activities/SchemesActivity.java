package com.krishield.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.krishield.R;
import com.krishield.adapters.SchemesAdapter;
import com.krishield.models.Scheme;
import com.krishield.repositories.SchemesRepository;

import java.util.List;

public class SchemesActivity extends AppCompatActivity {

    private RecyclerView rvSchemes;
    private SwipeRefreshLayout swipeRefresh;
    private SchemesAdapter adapter;
    private SchemesRepository repository;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schemes);

        initializeViews();
        setupRecyclerView();
        loadSchemes(false);
    }

    private void initializeViews() {
        rvSchemes = findViewById(R.id.rv_schemes);
        swipeRefresh = findViewById(R.id.swipe_refresh_layout);
        btnBack = findViewById(R.id.btn_back);
        repository = new SchemesRepository(this);

        btnBack.setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(() -> loadSchemes(true));
    }

    private void setupRecyclerView() {
        adapter = new SchemesAdapter(this);
        rvSchemes.setLayoutManager(new LinearLayoutManager(this));
        rvSchemes.setAdapter(adapter);
    }

    private void loadSchemes(boolean forceRefresh) {
        swipeRefresh.setRefreshing(true);
        repository.getSchemes(forceRefresh, new SchemesRepository.SchemesCallback() {
            @Override
            public void onSuccess(List<Scheme> schemes) {
                runOnUiThread(() -> {
                    adapter.setSchemes(schemes);
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(SchemesActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
