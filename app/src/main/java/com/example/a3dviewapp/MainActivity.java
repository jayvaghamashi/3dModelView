package com.example.a3dviewapp;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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
import com.squareup.picasso.Picasso; // Make sure you have this import
import com.squareup.picasso.Target;  // Make sure you have this import

// Imports from your Engine Package
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

    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    // [IMPORTANT] Hold a reference to the Target so it doesn't get garbage collected
    private Target textureTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This connects to the same ViewModel used in ModelFragment
        modelViewModel = new ViewModelProvider(this).get(ModelViewModel.class);

        initializeUI();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();

        // Default Load
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
    }

    private void update3DView(String modelPath) {
        // NOTE: Ensure your asset path matches exactly.
        // Usually "models/man.dae" -> "assets/models/man.dae"
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
                applyTextureToModel(product.getActualImageUrl());
            }

            @Override public void onFavoriteClick(Product p, boolean fav) {}
            @Override public void onAddToCart(Product p) {}
        });
        productsRecyclerView.setAdapter(adapter);
    }

    private void applyTextureToModel(String textureUrl) {
        // 1. Get the engine. (This relies on Step 1 being fixed in ModelFragment!)
        ModelEngine engine = modelViewModel.getModelEngine().getValue();

        if (engine == null) {
            Toast.makeText(this, "Engine not ready yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Downloading Texture...", Toast.LENGTH_SHORT).show();

        // 2. Create a Picasso Target to download the image as a Bitmap
        textureTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // The bitmap is downloaded. Now apply it to the 3D scene.
                Scene scene = engine.getBeanFactory().find(Scene.class);
                if (scene != null && !scene.getObjects().isEmpty()) {
                    Object3DData model = scene.getObjects().get(0);

                    // Create the Texture object with the BITMAP
                    Texture newTexture = new Texture();
                    newTexture.setBitmap(bitmap); // <--- Use setBitmap, not setFile
                    newTexture.setFile(textureUrl); // Optional: store URL for reference

                    // Apply to Main Material
                    if (model.getMaterial() != null) {
                        model.getMaterial().setColorTexture(newTexture);
                    }

                    // Apply to Sub-Elements (Arms, Legs, etc.)
                    if (model.getElements() != null) {
                        for (Element e : model.getElements()) {
                            if (e.getMaterial() != null) {
                                e.getMaterial().setColorTexture(newTexture);
                            }
                        }
                    }

                    // Tell the engine the model has changed so it redraws
                    model.setChanged(true);
                    Toast.makeText(MainActivity.this, "Texture Applied!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Toast.makeText(MainActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // Optional: You can show a spinner here if you want
            }
        };

        // 3. Trigger the download
        Picasso.get().load(textureUrl).into(textureTarget);
    }

    private void setupClickListeners() {
        btnGirl.setOnClickListener(v -> update3DView("models/girl.dae"));
        btnMan.setOnClickListener(v -> update3DView("models/man.dae"));
        btnRounded.setOnClickListener(v -> update3DView("models/rounded.dae"));
        btnRoblox.setOnClickListener(v -> update3DView("models/roblox.dae"));

        fabZoomIn.setOnClickListener(v -> {
            ModelEngine e = modelViewModel.getModelEngine().getValue();
            if (e != null) e.getBeanFactory().find(CameraController.class).move(0, 0, -10f);
        });

        fabZoomOut.setOnClickListener(v -> {
            ModelEngine e = modelViewModel.getModelEngine().getValue();
            if (e != null) e.getBeanFactory().find(CameraController.class).move(0, 0, 10f);
        });

        chipTShirts.setOnClickListener(v -> fetchProducts("tshirts"));
        chipShirts.setOnClickListener(v -> fetchProducts("shirts"));
        chipPants.setOnClickListener(v -> fetchProducts("pants"));
    }

    private void fetchProducts(String category) {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.ProductRequest request = new ApiClient.ProductRequest(category, "1", "28");
        ApiClient.getClient().create(ApiClient.ApiInterface.class).getProducts(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allProducts = response.body().getData();
                    filteredProducts.clear();
                    filteredProducts.addAll(allProducts);
                    adapter.updateData(filteredProducts);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filteredProducts.clear();
                for (Product p : allProducts) {
                    if (p.getTitle().toLowerCase().contains(s.toString().toLowerCase()))
                        filteredProducts.add(p);
                }
                adapter.updateData(filteredProducts);
            }
        });
    }
}