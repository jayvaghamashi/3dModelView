package com.example.a3dviewapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

// Engine Imports
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
    private ImageView btnGirl, btnMan, btnRounded, btnRoblox;
    private FloatingActionButton fabZoomIn, fabZoomOut;
    private Chip chipTShirts, chipShirts, chipPants;
    private RecyclerView productsRecyclerView;
    private ProgressBar progressBar;
    private EditText searchBar;

    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    private Target textureTarget;

    // Track category to decide mapping logic
    private String currentCategory = "tshirts";

    // --- MAPPING ENUMS ---
    public enum Part { BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }
    public enum Side { FRONT, BACK, LEFT, RIGHT, UP, DOWN }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modelViewModel = new ViewModelProvider(this).get(ModelViewModel.class);

        initializeUI();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();

        update3DView("models/man.dae");

        String categoryFromIntent = getIntent().getStringExtra("CATEGORY");
        if (categoryFromIntent != null) {
            currentCategory = categoryFromIntent;
        } else {
            currentCategory = "tshirts"; // default
        }
        fetchProducts(currentCategory);
        //fetchProducts("tshirts");
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
        if (modelViewModel != null) {
            modelViewModel.setModelEngine(null);
        }

        String uri = "android://com.example.a3dviewapp/assets/" + modelPath;
        ModelFragment fragment = ModelFragment.newInstance(uri, null, false);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.model_container, fragment)
                .commit();

        new Handler(Looper.getMainLooper()).postDelayed(() -> fixModelVisuals(), 1500);
    }

    private void fixModelVisuals() {
        ModelEngine engine = modelViewModel.getModelEngine().getValue();
        if (engine == null) return;

        Scene scene = engine.getBeanFactory().find(Scene.class);
        if (scene == null) return;

        scene.setLightProfile(Scene.LightProfile.PointOfView);

        if (!scene.getObjects().isEmpty()) {
            Object3DData model = scene.getObjects().get(0);
            model.setChanged(true);
        }
    }

    // -------------------------------------------------------------------------
    // TEXTURE APPLICATION
    // -------------------------------------------------------------------------

    private void applyTextureToModel(String textureUrl) {
        ModelEngine engine = modelViewModel.getModelEngine().getValue();
        if (engine == null) {
            Toast.makeText(this, "Please wait, model loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Applying " + currentCategory + "...", Toast.LENGTH_SHORT).show();

        textureTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap downloadedBitmap, Picasso.LoadedFrom from) {
                try {
                    Bitmap processedBitmap;

                    if (currentCategory.equals("tshirts")) {
                        // T-Shirts: Chest Decal Only
                        processedBitmap = processTShirtTexture(downloadedBitmap);
                    } else {
                        // Shirts & Pants: Template Logic
                        processedBitmap = processRobloxTemplate(downloadedBitmap);
                    }

                    Scene scene = engine.getBeanFactory().find(Scene.class);
                    if (scene != null && !scene.getObjects().isEmpty()) {
                        Object3DData model = scene.getObjects().get(0);

                        Texture newTexture = new Texture();
                        newTexture.setBitmap(processedBitmap);

                        if (model.getElements() != null) {
                            for (Element e : model.getElements()) {
                                if (e.getId().equalsIgnoreCase("geometry1")) {
                                    if (e.getMaterial() != null) {
                                        e.getMaterial().setColorTexture(newTexture);
                                    }
                                }
                            }
                        }

                        model.setChanged(true);
                        Toast.makeText(MainActivity.this, "Applied!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Toast.makeText(MainActivity.this, "Download Failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) { }
        };

        Picasso.get().load(textureUrl).into(textureTarget);
    }

    // -------------------------------------------------------------------------
    // LOGIC 1: T-SHIRTS (Single Image -> Chest Only)
    // -------------------------------------------------------------------------
    private Bitmap processTShirtTexture(Bitmap sourceBitmap) {
        Bitmap finalTexture = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalTexture);
        canvas.drawColor(Color.WHITE);

        // Chest Area
        Rect chestRect = new Rect(0, 72, 132, 204);

        if (sourceBitmap != null) {
            canvas.drawBitmap(sourceBitmap, null, chestRect, null);
        }

        return finalTexture;
    }

    // -------------------------------------------------------------------------
    // LOGIC 2: SHIRTS & PANTS (Fix Applied Here!)
    // -------------------------------------------------------------------------
    private Bitmap processRobloxTemplate(Bitmap sourceBitmap) {
        Bitmap finalTexture = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalTexture);
        canvas.drawColor(Color.WHITE);

        int scale = 1;
        Side[] sides = Side.values();
        Part[] parts;

        // [FIX] અહીં આપણે નક્કી કરીએ છીએ કે શું પહેરાવવું છે
        if (currentCategory.equals("pants")) {
            // જો PANTS હોય: તો માત્ર બોડી અને પગ (Arms નહીં)
            parts = new Part[]{Part.BODY, Part.LEFT_LEG, Part.RIGHT_LEG};
        } else {
            // જો SHIRTS હોય: તો માત્ર બોડી અને હાથ (Legs નહીં)
            // આનાથી શર્ટનું કપડું પગ પર કે બીજી જગ્યાએ નહીં જાય
            parts = new Part[]{Part.BODY, Part.LEFT_ARM, Part.RIGHT_ARM};
        }

        for (Part part : parts) {
            for (Side side : sides) {
                Rect srcRect = getSourceRect(part, side, scale);
                Rect dstRect = getDestRect(part, side, scale);

                // Check works to avoid crashes
                if (srcRect.width() > 0 && dstRect.width() > 0) {
                    drawPart(canvas, sourceBitmap, srcRect, dstRect);
                }
            }
        }
        return finalTexture;
    }

    private void drawPart(Canvas canvas, Bitmap source, Rect src, Rect dst) {
        try {
            if (src.left >= 0 && src.top >= 0 && src.right <= source.getWidth() && src.bottom <= source.getHeight()) {
                Bitmap piece = Bitmap.createBitmap(source, src.left, src.top, src.width(), src.height());
                canvas.drawBitmap(piece, null, dst, null);
            }
        } catch (Exception e) {}
    }

    // --- COORDINATES ---
    private Rect getSourceRect(Part part, Side side, int i) {
        if (part == Part.BODY) {
            if (side == Side.FRONT) return new Rect(i * 229, i * 72, i * 229 + (i * 132), i * 72 + (i * 132));
            if (side == Side.LEFT)  return new Rect(i * 361, i * 72, i * 361 + (i * 66), i * 72 + (i * 132));
            if (side == Side.BACK)  return new Rect(i * 427, i * 72, i * 427 + (i * 129), i * 72 + (i * 132));
            if (side == Side.RIGHT) return new Rect(i * 165, i * 72, i * 165 + (i * 64), i * 72 + (i * 132));
            if (side == Side.UP)    return new Rect(i * 229, i * 8,  i * 229 + (i * 32), i * 8 + (i * 64));
            if (side == Side.DOWN)  return new Rect(i * 229, i * 204, i * 229 + (i * 132), i * 204 + (i * 64));
        }
        if (part == Part.RIGHT_ARM || part == Part.RIGHT_LEG) {
            if (side == Side.FRONT) return new Rect(i * 215, i * 353, i * 215 + (i * 64), i * 353 + (i * 132));
            if (side == Side.LEFT)  return new Rect(i * 19,  i * 353, i * 19 + (i * 66),  i * 353 + (i * 132));
            if (side == Side.BACK)  return new Rect(i * 85,  i * 353, i * 85 + (i * 64),  i * 353 + (i * 132));
            if (side == Side.RIGHT) return new Rect(i * 149, i * 353, i * 149 + (i * 68), i * 353 + (i * 132));
            if (side == Side.UP)    return new Rect(i * 215, i * 289, i * 215 + (i * 68), i * 289 + (i * 64));
            if (side == Side.DOWN)  return new Rect(i * 215, i * 485, i * 215 + (i * 68), i * 485 + (i * 64));
        }
        if (part == Part.LEFT_ARM || part == Part.LEFT_LEG) {
            if (side == Side.FRONT) return new Rect(i * 307, i * 353, i * 307 + (i * 64), i * 353 + (i * 132));
            if (side == Side.LEFT)  return new Rect(i * 375, i * 353, i * 375 + (i * 66), i * 353 + (i * 132));
            if (side == Side.BACK)  return new Rect(i * 441, i * 353, i * 441 + (i * 64), i * 353 + (i * 132));
            if (side == Side.RIGHT) return new Rect(i * 505, i * 353, i * 505 + (i * 68), i * 353 + (i * 132));
            if (side == Side.UP)    return new Rect(i * 307, i * 289, i * 307 + (i * 68), i * 289 + (i * 64));
            if (side == Side.DOWN)  return new Rect(i * 307, i * 485, i * 307 + (i * 68), i * 485 + (i * 64));
        }
        return new Rect(0, 0, 0, 0);
    }

    private Rect getDestRect(Part part, Side side, int i) {
        if (part == Part.BODY) {
            if (side == Side.FRONT) return new Rect(0, i * 72, i * 132, i * 72 + (i * 132));
            if (side == Side.LEFT)  return new Rect(i * 132, i * 72, i * 132 + (i * 66), i * 72 + (i * 132));
            if (side == Side.BACK)  return new Rect(i * 198, i * 72, i * 198 + (i * 129), i * 72 + (i * 132));
            if (side == Side.RIGHT) return new Rect(i * 327, i * 72, i * 327 + (i * 64), i * 72 + (i * 132));
            if (side == Side.UP)    return new Rect(0, i * 8, i * 132, i * 8 + (i * 64));
            if (side == Side.DOWN)  return new Rect(0, i * 204, i * 132, i * 204 + (i * 64));
        }
        if (part == Part.LEFT_ARM) {
            if (side == Side.FRONT) return new Rect(i * 564, i * 64, i * 564 + (i * 64), i * 64 + (i * 112));
            if (side == Side.LEFT)  return new Rect(i * 628, i * 64, i * 628 + (i * 66), i * 64 + (i * 112));
            if (side == Side.BACK)  return new Rect(i * 694, i * 64, i * 694 + (i * 64), i * 64 + (i * 112));
            if (side == Side.RIGHT) return new Rect(i * 496, i * 64, i * 496 + (i * 68), i * 64 + (i * 112));
            if (side == Side.UP)    return new Rect(i * 496, 0, i * 496 + (i * 68), i * 64);
            if (side == Side.DOWN)  return new Rect(i * 692, i * 215, i * 692 + (i * 68), i * 215 + (i * 64));
        }
        if (part == Part.RIGHT_ARM) {
            if (side == Side.FRONT) return new Rect(i * 828, i * 64, i * 828 + (i * 64), i * 64 + (i * 112));
            if (side == Side.LEFT)  return new Rect(i * 892, i * 64, i * 892 + (i * 66), i * 64 + (i * 112));
            if (side == Side.BACK)  return new Rect(i * 958, i * 64, i * 958 + (i * 64), i * 64 + (i * 112));
            if (side == Side.RIGHT) return new Rect(i * 760, i * 64, i * 760 + (i * 68), i * 64 + (i * 112));
            if (side == Side.UP)    return new Rect(i * 760, 0, i * 760 + (i * 68), i * 64);
            if (side == Side.DOWN)  return new Rect(i * 956, i * 215, i * 956 + (i * 68), i * 215 + (i * 64));
        }
        if (part == Part.LEFT_LEG) {
            if (side == Side.FRONT) return new Rect(i * 564, i * 348, i * 564 + (i * 64), i * 348 + (i * 112));
            if (side == Side.LEFT)  return new Rect(i * 628, i * 348, i * 628 + (i * 66), i * 348 + (i * 112));
            if (side == Side.BACK)  return new Rect(i * 694, i * 348, i * 694 + (i * 64), i * 348 + (i * 112));
            if (side == Side.RIGHT) return new Rect(i * 496, i * 348, i * 496 + (i * 68), i * 348 + (i * 112));
            if (side == Side.UP)    return new Rect(i * 496, i * 284, i * 496 + (i * 68), i * 284 + (i * 64));
            if (side == Side.DOWN)  return new Rect(i * 692, i * 499, i * 692 + (i * 68), i * 499 + (i * 64));
        }
        if (part == Part.RIGHT_LEG) {
            if (side == Side.FRONT) return new Rect(i * 828, i * 348, i * 828 + (i * 64), i * 348 + (i * 112));
            if (side == Side.LEFT)  return new Rect(i * 892, i * 348, i * 892 + (i * 66), i * 348 + (i * 112));
            if (side == Side.BACK)  return new Rect(i * 958, i * 348, i * 958 + (i * 64), i * 348 + (i * 112));
            if (side == Side.RIGHT) return new Rect(i * 760, i * 348, i * 760 + (i * 68), i * 348 + (i * 112));
            if (side == Side.UP)    return new Rect(i * 760, i * 284, i * 760 + (i * 68), i * 284 + (i * 64));
            if (side == Side.DOWN)  return new Rect(i * 956, i * 499, i * 956 + (i * 68), i * 499 + (i * 64));
        }
        return new Rect(0, 0, 0, 0);
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

        chipTShirts.setOnClickListener(v -> fetchProducts(currentCategory));
        chipShirts.setOnClickListener(v -> fetchProducts(currentCategory));
        chipPants.setOnClickListener(v -> fetchProducts(currentCategory));
    }

    private void setupRecyclerView() {
        productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(this, filteredProducts, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                applyTextureToModel(product.getActualImageUrl());
            }


        });
        productsRecyclerView.setAdapter(adapter);
    }

    private void fetchProducts(String category) {
        // [IMPORTANT] કેટેગરી અપડેટ કરવી જરૂરી છે જેથી આપણે નક્કી કરી શકીએ કે કઈ લોજિક વાપરવી
        this.currentCategory = category;

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