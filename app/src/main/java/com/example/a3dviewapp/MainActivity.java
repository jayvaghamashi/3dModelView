package com.example.a3dviewapp;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
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

import org.the3deer.android_3d_model_engine.ModelEngine;
import org.the3deer.android_3d_model_engine.ModelFragment;
import org.the3deer.android_3d_model_engine.ModelViewModel;
import org.the3deer.android_3d_model_engine.camera.CameraController;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Texture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

    private ModelViewModel modelViewModel;
    private ImageView btnGirl, btnMan, btnRounded, btnRoblox, btnback;
    private FloatingActionButton fabZoomIn, fabZoomOut;
    private Chip chipTShirts, chipShirts, chipPants;
    private RecyclerView productsRecyclerView;
    private ProgressBar progressBar;
    private ProductAdapter adapter;

    private String imageUrl = "";
    private String currentCategory = "tshirts";
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    private Target textureTarget;
    private Target downloadTarget;

    private boolean doubleBackToExitPressedOnce = false;

    public enum Part { BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }
    public enum Side { FRONT, BACK, LEFT, RIGHT, UP, DOWN }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View mainLayout = findViewById(R.id.acticity_main);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        modelViewModel = new ViewModelProvider(this).get(ModelViewModel.class);

        initializeUI();
        setupRecyclerView();
        setupClickListeners();

        ImageButton btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                startDownloadSequence();
            } else {
                Toast.makeText(this, "Please select a cloth design first!", Toast.LENGTH_SHORT).show();
            }
        });

        String defaultModel = getSharedPreferences("ModelPrefs", MODE_PRIVATE)
                .getString("default_model", "models/man.dae");
        update3DView(defaultModel);

        String categoryFromIntent = getIntent().getStringExtra("CATEGORY");
        currentCategory = (categoryFromIntent != null) ? categoryFromIntent : "tshirts";
        fetchProducts(currentCategory);

        btnback = findViewById(R.id.btnBack);
        btnback.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    showExitDialog();
                    return;
                }
                doubleBackToExitPressedOnce = true;
                Toast.makeText(MainActivity.this, "Press again to exit", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
            }
        });
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
    }

    private void setupRecyclerView() {
        productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(this, filteredProducts, product -> {
            imageUrl = product.getActualImageUrl();
            applyTextureToModel(imageUrl);
        });
        productsRecyclerView.setAdapter(adapter);
    }

    private void startDownloadSequence() {
        Toast.makeText(this, "Downloading for Share...", Toast.LENGTH_SHORT).show();
        downloadTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                saveImageToGallery(bitmap);
                Intent intent = new Intent(MainActivity.this, activity_share.class);
                intent.putExtra("IMAGE_URL", imageUrl);
                startActivity(intent);
                downloadTarget = null;
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Toast.makeText(MainActivity.this, "Failed to download", Toast.LENGTH_SHORT).show();
                downloadTarget = null;
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        };
        Picasso.get().load(imageUrl).into(downloadTarget);
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String filename = "Design_" + System.currentTimeMillis() + ".png";
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/3DViewApp");
                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = getContentResolver().openOutputStream(imageUri);
            } else {
                File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File image = new File(imagesDir, filename);
                fos = new FileOutputStream(image);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyTextureToModel(String textureUrl) {
        ModelEngine engine = modelViewModel.getModelEngine().getValue();
        if (engine == null) return;

        textureTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap downloadedBitmap, Picasso.LoadedFrom from) {
                Bitmap processed = currentCategory.equals("tshirts") ?
                        processTShirtTexture(downloadedBitmap) : processRobloxTemplate(downloadedBitmap);

                Scene scene = engine.getBeanFactory().find(Scene.class);
                if (scene != null && !scene.getObjects().isEmpty()) {
                    Object3DData model = scene.getObjects().get(0);
                    Texture newTexture = new Texture();
                    newTexture.setBitmap(processed);

                    if (model.getElements() != null) {
                        for (Element e : model.getElements()) {
                            if (e.getId().equalsIgnoreCase("geometry1")) {
                                if (e.getMaterial() != null) e.getMaterial().setColorTexture(newTexture);
                            }
                        }
                    }
                    model.setChanged(true);
                }
                textureTarget = null;
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) { textureTarget = null; }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        };
        Picasso.get().load(textureUrl).into(textureTarget);
    }

    private Bitmap processTShirtTexture(Bitmap source) {
        Bitmap finalTexture = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalTexture);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(source, null, new Rect(0, 72, 132, 204), null);
        return finalTexture;
    }

    private Bitmap processRobloxTemplate(Bitmap source) {
        Bitmap finalTexture = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalTexture);
        canvas.drawColor(Color.WHITE);

        int scale = 1;
        Side[] sides = Side.values();
        Part[] parts;

        if (currentCategory.equals("pants")) {
            parts = new Part[]{Part.BODY, Part.LEFT_LEG, Part.RIGHT_LEG};
        } else {
            parts = new Part[]{Part.BODY, Part.LEFT_ARM, Part.RIGHT_ARM};
        }

        for (Part part : parts) {
            for (Side side : sides) {
                Rect src = getSourceRect(part, side, scale);
                Rect dst = getDestRect(part, side, scale);
                if (src.width() > 0 && dst.width() > 0) {
                    drawPart(canvas, source, src, dst);
                }
            }
        }
        return finalTexture;
    }

    private void drawPart(Canvas canvas, Bitmap source, Rect src, Rect dst) {
        try {
            Bitmap piece = Bitmap.createBitmap(source, src.left, src.top, src.width(), src.height());
            canvas.drawBitmap(piece, null, dst, null);
        } catch (Exception e) {}
    }

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

    private void update3DView(String modelPath) {
        if (modelViewModel != null) modelViewModel.setModelEngine(null);
        String uri = "android://com.example.a3dviewapp/assets/" + modelPath;
        ModelFragment fragment = ModelFragment.newInstance(uri, null, false);
        getSupportFragmentManager().beginTransaction().replace(R.id.model_container, fragment).commit();
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
            public void onFailure(Call<ApiResponse> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void showExitDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_exit);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.findViewById(R.id.btnYes).setOnClickListener(v -> finishAffinity());
        dialog.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 3D View ને સુરક્ષિત રીતે Pause કરવા માટે
        android.opengl.GLSurfaceView glView = findGLSurfaceView();
        if (glView != null) {
            glView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 3D View ને સુરક્ષિત રીતે Resume કરવા માટે
        android.opengl.GLSurfaceView glView = findGLSurfaceView();
        if (glView != null) {
            glView.onResume();
        }
    }

    // આ મેથડ તમારા Fragment માંથી GLSurfaceView શોધી આપશે
    private android.opengl.GLSurfaceView findGLSurfaceView() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.model_container);
        if (fragment != null && fragment.getView() instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) fragment.getView();
            for (int i = 0; i < group.getChildCount(); i++) {
                if (group.getChildAt(i) instanceof android.opengl.GLSurfaceView) {
                    return (android.opengl.GLSurfaceView) group.getChildAt(i);
                }
            }
        }
        return null;
    }
    }
