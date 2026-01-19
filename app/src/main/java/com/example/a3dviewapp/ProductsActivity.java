package com.example.a3dviewapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a3dviewapp.adapter.ProductAdapter;
import com.example.a3dviewapp.model.ApiResponse;
import com.example.a3dviewapp.model.Product;
import com.example.a3dviewapp.network.ApiClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ChipGroup filterChipGroup;
    private Chip chipAll, chipTShirts, chipShirts, chipPants;
    private RecyclerView productsRecyclerView;
    private View progressBar, emptyState;

    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private String selectedCategory = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupChipListeners();

        // Get category from intent
        String category = getIntent().getStringExtra("category");
        if (category != null && !category.equals("all")) {
            selectedCategory = category;
            selectChip(category);
        }

        // Load initial data
        loadProducts(selectedCategory);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipTShirts = findViewById(R.id.chipTShirts);
        chipShirts = findViewById(R.id.chipShirts);
        chipPants = findViewById(R.id.chipPants);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        productsRecyclerView.setLayoutManager(layoutManager);

        adapter = new ProductAdapter(this, filteredProducts, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // Open Simple3DViewerActivity (corrected name)
                Intent intent = new Intent(ProductsActivity.this,RealTextureActivity.class);
                intent.putExtra("product_name", product.getTitle());
                intent.putExtra("product_image", product.getImageUrl());
                intent.putExtra("product_id", product.getId());
                startActivity(intent);
            }

            @Override
            public void onFavoriteClick(Product product, boolean isFavorite) {
                Toast.makeText(ProductsActivity.this,
                        isFavorite ? "Added to favorites" : "Removed from favorites",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddToCart(Product product) {
                Toast.makeText(ProductsActivity.this,
                        "Added to cart: " + product.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        productsRecyclerView.setAdapter(adapter);
    }

    private void setupChipListeners() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                chipAll.setChecked(true);
                selectedCategory = "all";
            } else {
                int checkedId = checkedIds.get(0);

                if (checkedId == R.id.chipAll) {
                    selectedCategory = "all";
                } else if (checkedId == R.id.chipTShirts) {
                    selectedCategory = "tshirts";
                } else if (checkedId == R.id.chipShirts) {
                    selectedCategory = "shirts";
                } else if (checkedId == R.id.chipPants) {
                    selectedCategory = "pants";
                }

                loadProducts(selectedCategory);
            }
        });
    }

    private void selectChip(String category) {
        switch (category) {
            case "all":
                chipAll.setChecked(true);
                break;
            case "tshirts":
                chipTShirts.setChecked(true);
                break;
            case "shirts":
                chipShirts.setChecked(true);
                break;
            case "pants":
                chipPants.setChecked(true);
                break;
        }
    }

    private void loadProducts(String category) {
        if (category.equals("all")) {
            // Load all products from all categories
            loadAllProducts();
        } else {
            // Load specific category
            fetchCategoryProducts(category);
        }
    }

    private void fetchCategoryProducts(String category) {
        showLoading(true);

        ApiClient.ProductRequest request = new ApiClient.ProductRequest(
                category,
                "1",
                "50"
        );

        ApiClient.ApiInterface apiInterface = ApiClient.getClient().create(ApiClient.ApiInterface.class);
        Call<ApiResponse> call = apiInterface.getProducts(request);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.getResult().equals("ok")) {
                        filteredProducts = apiResponse.getData();
                        adapter.updateData(filteredProducts);
                        updateTitle(category);
                        updateEmptyState();
                    } else {
                        Toast.makeText(ProductsActivity.this, "API Error", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ProductsActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ProductsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllProducts() {
        showLoading(true);

        // We'll load tshirts first as default
        fetchCategoryProducts("tshirts");
    }

    private void updateTitle(String category) {
        String title = "All Products";

        switch (category) {
            case "tshirts":
                title = "T-Shirts";
                break;
            case "shirts":
                title = "Shirts";
                break;
            case "pants":
                title = "Pants";
                break;
        }

        toolbar.setTitle(title);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        productsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (emptyState != null) {
            if (filteredProducts.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                productsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                productsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
}