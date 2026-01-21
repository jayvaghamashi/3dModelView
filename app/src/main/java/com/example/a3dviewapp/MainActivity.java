package com.example.a3dviewapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a3dviewapp.adapter.ProductAdapter;
import com.example.a3dviewapp.model.ApiResponse;
import com.example.a3dviewapp.model.Product;
import com.example.a3dviewapp.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private CardView tshirtsCard, shirtsCard, pantsCard;
    private RecyclerView productsRecyclerView;
    private ProgressBar progressBar;
    private EditText searchBar;
    private TextView productsTitle, viewAll;
    private View emptyState;

    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private String currentCategory = "tshirts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();

        // Load initial data (T-Shirts by default)
        fetchProducts("tshirts");
    }

    private void initializeViews() {
        try {
            tshirtsCard = findViewById(R.id.tshirtsCard);
            shirtsCard = findViewById(R.id.shirtsCard);
            pantsCard = findViewById(R.id.pantsCard);
            productsRecyclerView = findViewById(R.id.productsRecyclerView);
            progressBar = findViewById(R.id.progressBar);
            searchBar = findViewById(R.id.searchBar);
            productsTitle = findViewById(R.id.productsTitle);
            viewAll = findViewById(R.id.viewAll);
            emptyState = findViewById(R.id.emptyState);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        try {
            GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
            productsRecyclerView.setLayoutManager(layoutManager);

            adapter = new ProductAdapter(this, filteredProducts, new ProductAdapter.OnProductClickListener() {
                @Override
                public void onProductClick(Product product) {
                    try {
                        Intent intent = new Intent(MainActivity.this, RealTextureActivity.class);

                        // Pass basic info
                        intent.putExtra("product_id", product.getId());
                        intent.putExtra("product_name", product.getTitle());
                        intent.putExtra("product_type", product.getType());

                        // Pass the actual texture URL using the helper method in Product.java
                        intent.putExtra("product_image", product.getActualImageUrl());

                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error opening 3D view: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFavoriteClick(Product product, boolean isFavorite) {
                    Toast.makeText(MainActivity.this, isFavorite ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAddToCart(Product product) {
                    Toast.makeText(MainActivity.this, "Added to cart: " + product.getTitle(), Toast.LENGTH_SHORT).show();
                }
            });

            productsRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Toast.makeText(this, "Error setting up RecyclerView: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        // T-Shirts Card Click
        tshirtsCard.setOnClickListener(v -> {
            productsTitle.setText("T-Shirts");
            currentCategory = "tshirts";
            fetchProducts("tshirts");
            searchBar.setText("");
        });

        // Shirts Card Click
        shirtsCard.setOnClickListener(v -> {
            productsTitle.setText("Shirts");
            currentCategory = "shirts";
            fetchProducts("shirts");
            searchBar.setText("");
        });

        // Pants Card Click
        pantsCard.setOnClickListener(v -> {
            productsTitle.setText("Pants");
            currentCategory = "pants";
            fetchProducts("pants");
            searchBar.setText("");
        });

        // View All Click
        viewAll.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProductsActivity.class);
            intent.putExtra("category", currentCategory);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterProducts(s.toString());
            }
        });
    }

    private void fetchProducts(String category) {
        showLoading(true);

        try {
            ApiClient.ProductRequest request = new ApiClient.ProductRequest(category, "1", "28");
            ApiClient.ApiInterface apiInterface = ApiClient.getClient().create(ApiClient.ApiInterface.class);
            Call<ApiResponse> call = apiInterface.getProducts(request);

            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if ("ok".equals(apiResponse.getResult())) {
                            allProducts = apiResponse.getData();
                            filteredProducts.clear();
                            filteredProducts.addAll(allProducts);
                            adapter.updateData(filteredProducts);
                            updateEmptyState();
                        } else {
                            Toast.makeText(MainActivity.this, "API Error: " + apiResponse.toString(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void filterProducts(String query) {
        filteredProducts.clear();
        if (query.isEmpty()) {
            filteredProducts.addAll(allProducts);
        } else {
            String searchQuery = query.toLowerCase().trim();
            for (Product product : allProducts) {
                if (product.getTitle().toLowerCase().contains(searchQuery) ||
                        (product.getId() != null && product.getId().toLowerCase().contains(searchQuery))) {
                    filteredProducts.add(product);
                }
            }
        }
        adapter.updateData(filteredProducts);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredProducts.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            productsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            productsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        productsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) emptyState.setVisibility(View.GONE);
    }
}