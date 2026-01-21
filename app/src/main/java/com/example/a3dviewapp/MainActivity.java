package com.example.a3dviewapp;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.a3dviewapp.adapter.ProductAdapter;
import com.example.a3dviewapp.model.ApiResponse;
import com.example.a3dviewapp.model.Product;
import com.example.a3dviewapp.network.ApiClient;
import com.google.android.material.chip.Chip;


import org.the3deer.android_3d_model_engine.ModelEngine;
import org.the3deer.android_3d_model_engine.ModelFragment;
import org.the3deer.android_3d_model_engine.ModelViewModel;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.services.collada.ColladaLoader;



import org.the3deer.util.android.ContentUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    // --- 3D Engine Variables ---
    private ModelViewModel viewModel;
    private ModelEngine modelEngine;
    private ModelFragment modelFragment;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isTextureApplied = false;

    // --- UI Variables ---
    private RecyclerView productsRecyclerView;
    private ProgressBar progressBar, modelProgressBar;
    private EditText searchBar;
    private View emptyState;
    private Chip chipTShirts, chipShirts, chipPants;

    // --- Data Variables ---
    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    // --- Dialogs ---
    private LoadContentDialog loadContentDialog = new LoadContentDialog(this);
    private ActivityResultLauncher<String> mGetContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize 3D Engine Essentials
        ContentUtils.setThreadActivity(this);
        ContentUtils.provideAssets(this);
        try {
            viewModel = new ViewModelProvider(this).get(ModelViewModel.class);
            modelEngine = ModelEngine.newInstance(this, savedInstanceState, null);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ModelEngine", e);
        }

        // 2. Initialize UI & Listeners
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();
        setupMenuLogic(); // Keep existing menu/dialog logic

        // 3. Load Default 3D Model (Mannequin)
        load3DModel("man.dae");

        // 4. Fetch Products
        fetchProducts("tshirts");
    }

    private void initializeViews() {
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        modelProgressBar = findViewById(R.id.modelProgressBar); // Add this ID to XML
        searchBar = findViewById(R.id.searchBar);
        emptyState = findViewById(R.id.emptyState);
        chipTShirts = findViewById(R.id.chipTShirts);
        chipShirts = findViewById(R.id.chipShirts);
        chipPants = findViewById(R.id.chipPants);
    }

    // --- 3D MODEL LOGIC (Merged) ---

    private void load3DModel(String filename) {
        try {
            // Determine type: 2 for DAE (Collada)
            String uri = "android://org.the3deer.dddmodel2/assets/models/" + filename;
            Log.i(TAG, "Loading model: " + uri);

            // Replace fragment in the top container
            modelFragment = ModelFragment.newInstance(uri, "2", false);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.model_container, modelFragment, "model_tag")
                    .commit();

        } catch (Exception e) {
            Log.e(TAG, "Error loading model", e);
        }
    }

    private void applyTextureToModel(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        modelProgressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Applying Texture...", Toast.LENGTH_SHORT).show();

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        new Thread(() -> {
                            try {
                                Bitmap mappedBitmap = convertRobloxTemplateToTexture(resource);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                mappedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                byte[] textureData = stream.toByteArray();

                                runOnUiThread(() -> {
                                    updateModelTexture(textureData);
                                    modelProgressBar.setVisibility(View.GONE);
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Texture error", e);
                                runOnUiThread(() -> modelProgressBar.setVisibility(View.GONE));
                            }
                        }).start();
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    private void updateModelTexture(byte[] textureData) {
        // Retry logic to ensure model is ready
        applyTextureWithRetry(textureData, 0);
    }

    private void applyTextureWithRetry(byte[] textureData, int retryCount) {
        List<Object3DData> allObjects = getAllObjectsFromScene();

        if (allObjects != null && !allObjects.isEmpty()) {
            boolean applied = false;
            for (Object3DData object : allObjects) {
                if (applyTextureToObject(object, textureData)) applied = true;
            }
            if (applied) {
                Toast.makeText(MainActivity.this, "Texture Applied!", Toast.LENGTH_SHORT).show();
            } else if (retryCount < 10) {
                handler.postDelayed(() -> applyTextureWithRetry(textureData, retryCount + 1), 1000);
            }
        } else if (retryCount < 10) {
            handler.postDelayed(() -> applyTextureWithRetry(textureData, retryCount + 1), 1000);
        }
    }

    private List<Object3DData> getAllObjectsFromScene() {
        try {
            // Access ViewModel scoped to THIS activity
            if (viewModel != null && viewModel.getModelEngine().getValue() != null) {
                Scene scene = viewModel.getModelEngine().getValue().getBeanFactory().find(Scene.class);
                if (scene != null) return scene.getObjects();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing scene", e);
        }
        return new ArrayList<>();
    }

    private boolean applyTextureToObject(Object3DData object, byte[] textureData) {
        try {
            if (object == null) return false;
            Texture texture = new Texture();
            texture.setData(textureData);
            texture.setId((int) (System.currentTimeMillis() % Integer.MAX_VALUE));

            // Apply to elements (sub-meshes)
            if (object.getElements() != null) {
                for (org.the3deer.android_3d_model_engine.model.Element element : object.getElements()) {
                    if (element.getMaterial() == null) element.setMaterial(new Material("mat_" + object.getId()));
                    element.getMaterial().setColorTexture(texture);
                    element.getMaterial().setAlpha(1.0f);
                }
            }
            // Apply to main object
            if (object.getMaterial() == null) object.setMaterial(new Material("main_mat"));
            object.getMaterial().setColorTexture(texture);
            object.getMaterial().setAlpha(1.0f);

            object.setChanged(true);
            return true;
        } catch (Exception e) { return false; }
    }

    // --- Helper: Roblox Texture Mapping ---
    private Bitmap convertRobloxTemplateToTexture(Bitmap sourceBitmap) {
        Bitmap finalTexture = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalTexture);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        int i = 1;
        ColladaLoader.Side[] sides = ColladaLoader.Side.values();
        ColladaLoader.Part[] parts = {
                ColladaLoader.Part.BODY, ColladaLoader.Part.LEFT_ARM,
                ColladaLoader.Part.RIGHT_ARM, ColladaLoader.Part.LEFT_LEG,
                ColladaLoader.Part.RIGHT_LEG
        };

        for (ColladaLoader.Part part : parts) {
            for (ColladaLoader.Side side : sides) {
                Rect srcRect = ColladaLoader.getSourceRect(part, side, i);
                Rect dstRect = ColladaLoader.getDestRect(part, side, i);
                if (srcRect.width() > 0 && dstRect.width() > 0) {
                    drawPartWithPadding(canvas, sourceBitmap, srcRect, dstRect);
                }
            }
        }
        return finalTexture;
    }

    private void drawPartWithPadding(Canvas canvas, Bitmap source, Rect src, Rect dst) {
        try {
            if (src.left >= 0 && src.top >= 0 && src.right <= source.getWidth() && src.bottom <= source.getHeight()) {
                Bitmap piece = Bitmap.createBitmap(source, src.left, src.top, src.width(), src.height());
                canvas.drawBitmap(piece, null, new Rect(dst.left - 1, dst.top - 1, dst.right + 1, dst.bottom + 1), null);
                canvas.drawBitmap(piece, null, dst, null);
            }
        } catch (Exception e) {}
    }

    // --- PRODUCT BROWSING LOGIC ---

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        productsRecyclerView.setLayoutManager(layoutManager);

        adapter = new ProductAdapter(this, filteredProducts, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // Determine model type based on product metadata (simple logic here)
                String type = product.getType() != null ? product.getType().toLowerCase() : "man";
                if (type.contains("woman")) load3DModel("girl.dae");
                else if (type.contains("kid")) load3DModel("roblox.dae");
                else load3DModel("man.dae");

                // Apply texture
                applyTextureToModel(product.getActualImageUrl());
            }

            @Override public void onFavoriteClick(Product p, boolean fav) {}
            @Override public void onAddToCart(Product p) {}
        });
        productsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        chipTShirts.setOnClickListener(v -> fetchProducts("tshirts"));
        chipShirts.setOnClickListener(v -> fetchProducts("shirts"));
        chipPants.setOnClickListener(v -> fetchProducts("pants"));
    }

    private void fetchProducts(String category) {
        showLoading(true);
        ApiClient.ProductRequest request = new ApiClient.ProductRequest(category, "1", "28");
        ApiClient.getClient().create(ApiClient.ApiInterface.class).getProducts(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && "ok".equals(response.body().getResult())) {
                    allProducts = response.body().getData();
                    filterProducts(searchBar.getText().toString());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) { showLoading(false); }
        });
    }

    private void filterProducts(String query) {
        filteredProducts.clear();
        if (query.isEmpty()) filteredProducts.addAll(allProducts);
        else {
            for (Product p : allProducts) {
                if (p.getTitle().toLowerCase().contains(query.toLowerCase())) filteredProducts.add(p);
            }
        }
        adapter.updateData(filteredProducts);
        emptyState.setVisibility(filteredProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { filterProducts(s.toString()); }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        productsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    // --- Original Menu/Dialog Logic (Preserved) ---
    private void setupMenuLogic() {
        getSupportFragmentManager().setFragmentResultListener("app", this, (requestKey, bundle) -> {
            String action = bundle.getString("action");
            if ("load".equals(action)) showDialog();
            else if ("help".equals(action)) showHelpDialog();
            else if ("pick".equals(action)) loadContentDialog.start();
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { showDialog(); }
        });

        mGetContent = registerForActivityResult(loadContentDialog.getActivityContract(), uri -> {
            if(uri != null) loadContentDialog.load(uri);
        });
    }

    private void showDialog() {
        if (getSupportFragmentManager().findFragmentByTag("dialog") == null) {
            MainDialogFragment.newInstance(R.string.alert_dialog_main_title).show(getSupportFragmentManager(), "dialog");
        }
    }

    private void showHelpDialog() {
        HelpDialogFragment.newInstance(R.string.alert_dialog_help_title).show(getSupportFragmentManager(), "help");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContentUtils.setThreadActivity(null);
    }
}