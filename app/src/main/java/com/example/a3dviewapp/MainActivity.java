package com.example.a3dviewapp;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a3dviewapp.adapter.ProductAdapter;
import com.example.a3dviewapp.model.ApiResponse;
import com.example.a3dviewapp.model.Product;
import com.example.a3dviewapp.network.ApiClient;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.the3deer.android_3d_model_engine.ModelEngine;
import org.the3deer.android_3d_model_engine.ModelFragment;
import org.the3deer.android_3d_model_engine.ModelViewModel;
import org.the3deer.android_3d_model_engine.camera.CameraController;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Texture;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ModelViewModel modelViewModel;
    private Button btnGirl, btnMan, btnRounded, btnRoblox;
    private FloatingActionButton fabZoomIn, fabZoomOut;
    private Chip chipTShirts, chipShirts, chipPants;
    private RecyclerView productsRecyclerView;
    private ProgressBar progressBar;
    private EditText searchBar;
    private View emptyState;

    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modelViewModel = new ViewModelProvider(this).get(ModelViewModel.class);

        initializeUI();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();

        // Default Load - .dae files
        update3DView("models/man.dae");
        fetchProducts("tshirts");
    }

    private void initializeUI() {
        btnGirl = findViewById(R.id.btnGirl);
        btnMan = findViewById(R.id.btnMan);
        btnRounded = findViewById(R.id.btnRounded);
        btnRoblox = findViewById(R.id.btnRoblox);
        fabZoomIn = findViewById(R.id.fabZoomIn);
        fabZoomOut = findViewById(R.id.fabZoomOut);
        chipTShirts = findViewById(R.id.chipTShirts);
        chipShirts = findViewById(R.id.chipShirts);
        chipPants = findViewById(R.id.chipPants);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        searchBar = findViewById(R.id.searchBar);
        emptyState = findViewById(R.id.emptyState);
    }

    private void update3DView(String modelPath) {
        String uri = "android://com.example.a3dviewapp/assets/" + modelPath;
        ModelFragment fragment = ModelFragment.newInstance(uri, null, false);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.model_container, fragment)
                .commit();
    }

    private void setupRecyclerView() {
        productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(this, filteredProducts, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // APPLY TEXTURE
                // Use the URL from the files array if available
                String url = product.getActualImageUrl();
                applyTextureToModel(url);
               // applyTextureToModel(product.getActualImageUrl());
            }
            @Override public void onFavoriteClick(Product p, boolean fav) {}
            @Override public void onAddToCart(Product p) {}
        });
        productsRecyclerView.setAdapter(adapter);
    }

    private void applyTextureToModel(String textureUrl) {
        ModelEngine engine = modelViewModel.getModelEngine().getValue();
        if (engine != null) {
            // Access the scene through the bean factory
            Scene scene = engine.getBeanFactory().find(Scene.class);

            if (scene != null && scene.getObjects() != null && !scene.getObjects().isEmpty()) {
                // Get the character model (index 0)
                Object3DData character = scene.getObjects().get(0);

                // 1. Create a new Texture object for the product
                Texture newTexture = new Texture();
                newTexture.setFile(textureUrl); // The engine will load this URL in the background

                // 2. Update the main material of the object
                if (character.getMaterial() != null) {
                    character.getMaterial().setColorTexture(newTexture);
                }

                // 3. IMPORTANT: Update all sub-elements (DAE models have separate body parts)
                // This ensures the texture applies to the Torso/Shirt mesh specifically
                List<Element> elements = character.getElements();
                if (elements != null) {
                    for (Element element : elements) {
                        if (element.getMaterial() != null) {
                            element.getMaterial().setColorTexture(newTexture);
                        }
                    }
                }

                // 4. Force the engine to re-render the model with the new texture
                character.setChanged(true);

                Toast.makeText(this, "Texture Loading...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Model not ready yet", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupClickListeners() {
        if (btnGirl != null) btnGirl.setOnClickListener(v -> update3DView("models/girl.dae"));
        if (btnMan != null) btnMan.setOnClickListener(v -> update3DView("models/man.dae"));
        if (btnRounded != null) btnRounded.setOnClickListener(v -> update3DView("models/rounded.dae"));
        if (btnRoblox != null) btnRoblox.setOnClickListener(v -> update3DView("models/roblox.dae"));

        fabZoomIn.setOnClickListener(v -> {
            ModelEngine e = modelViewModel.getModelEngine().getValue();
            if (e != null) e.getBeanFactory().find(CameraController.class).move(0, 0, -10f);
        });

        fabZoomOut.setOnClickListener(v -> {
            ModelEngine e = modelViewModel.getModelEngine().getValue();
            if (e != null) e.getBeanFactory().find(CameraController.class).move(0, 0, 10f);
        });

        if (chipTShirts != null) chipTShirts.setOnClickListener(v -> fetchProducts("tshirts"));
        if (chipShirts != null) chipShirts.setOnClickListener(v -> fetchProducts("shirts"));
        if (chipPants != null) chipPants.setOnClickListener(v -> fetchProducts("pants"));
    }

    private void fetchProducts(String category) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        ApiClient.ProductRequest request = new ApiClient.ProductRequest(category, "1", "28");
        ApiClient.getClient().create(ApiClient.ApiInterface.class).getProducts(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && "ok".equals(response.body().getResult())) {
                    allProducts = response.body().getData();
                    filteredProducts.clear();
                    filteredProducts.addAll(allProducts);
                    adapter.updateData(filteredProducts);
                }
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupSearch() {
        if (searchBar != null) {
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { filterProducts(s.toString()); }
            });
        }
    }

    private void filterProducts(String query) {
        filteredProducts.clear();
        String q = query.toLowerCase().trim();
        for (Product p : allProducts) {
            if (p.getTitle() != null && p.getTitle().toLowerCase().contains(q)) filteredProducts.add(p);
        }
        adapter.updateData(filteredProducts);
    }
}