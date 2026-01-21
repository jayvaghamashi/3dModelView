package com.example.a3dviewapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // ViewModel માટે જરૂરી

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.the3deer.android_3d_model_engine.ModelEngine;
import org.the3deer.android_3d_model_engine.ModelFragment;
import org.the3deer.android_3d_model_engine.ModelViewModel;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.services.collada.ColladaLoader;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.bean.BeanFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RealTextureActivity extends AppCompatActivity {

    private static final String TAG = "RealTextureActivity";
    private Button btnChangeModel;
    private ModelFragment modelFragment;
    private String currentTextureUrl;
    private Handler handler = new Handler();
    private boolean isTextureApplied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_texture);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ContentUtils.setThreadActivity(this);
        ContentUtils.provideAssets(this);

        btnChangeModel = findViewById(R.id.btnChangeModel);

        String productId = getIntent().getStringExtra("product_id");
        String productName = getIntent().getStringExtra("product_name");
        String productType = getIntent().getStringExtra("product_type");
        currentTextureUrl = getIntent().getStringExtra("product_image");

        if (productName != null) {
            setTitle(productName);
        }

        String initialModel = determineInitialModel(productId, productType);
        load3DModel(initialModel);

        btnChangeModel.setOnClickListener(v -> showModelSelectionMenu());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void showModelSelectionMenu() {
        PopupMenu popup = new PopupMenu(RealTextureActivity.this, btnChangeModel);
        popup.getMenu().add(0, 1, 1, "Man (Male)");
        popup.getMenu().add(0, 2, 2, "Woman (Girl)");
        popup.getMenu().add(0, 3, 3, "Kid (Roblox)");
        popup.getMenu().add(0, 4, 4, "Abstract (Rounded)");

        popup.setOnMenuItemClickListener(item -> {
            isTextureApplied = false;
            switch (item.getItemId()) {
                case 1: load3DModel("man.dae"); return true;
                case 2: load3DModel("girl.dae"); return true;
                case 3: load3DModel("roblox.dae"); return true;
                case 4: load3DModel("rounded.dae"); return true;
                default: return false;
            }
        });
        popup.show();
    }

    private String determineInitialModel(String productId, String productType) {
        if (assetExists("models/" + productId + ".dae")) return productId + ".dae";
        if (productType != null) {
            String type = productType.toLowerCase();
            if (type.contains("woman") || type.contains("girl")) return "girl.dae";
            if (type.contains("kid") || type.contains("roblox")) return "roblox.dae";
        }
        return "man.dae";
    }

    private void load3DModel(String filename) {
        try {
            String modelPath = "models/" + filename;
            String typeId = "3";
            if (filename.toLowerCase().endsWith(".dae")) typeId = "2";
            else if (filename.toLowerCase().endsWith(".obj")) typeId = "0";

            Log.d(TAG, "Loading model: " + modelPath + " Type: " + typeId);
            Toast.makeText(this, "Loading: " + filename, Toast.LENGTH_SHORT).show();

            String uri = "android://com.example.a3dviewapp/assets/" + modelPath;

            Fragment existingFragment = getSupportFragmentManager().findFragmentByTag("model_tag");
            if (existingFragment != null) {
                getSupportFragmentManager().beginTransaction().remove(existingFragment).commit();
            }

            modelFragment = ModelFragment.newInstance(uri, typeId, false);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, modelFragment, "model_tag")
                    .commit();

            if (currentTextureUrl != null && !currentTextureUrl.isEmpty()) {
                handler.postDelayed(() -> {
                    if (!isTextureApplied) {
                        applyTextureToModel(currentTextureUrl);
                    }
                }, 4000);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + e.getMessage(), e);
        }
    }

    private void applyTextureToModel(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        Log.d(TAG, "Starting texture application from URL: " + imageUrl);
        Toast.makeText(this, "Applying Texture...", Toast.LENGTH_SHORT).show();

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            Bitmap mappedBitmap = convertRobloxTemplateToTexture(resource);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            mappedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            byte[] textureData = stream.toByteArray();

                            updateModelTexture(textureData);
                        } catch (Exception e) {
                            Log.e(TAG, "Texture processing failed", e);
                        }
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
    }

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
                // 1px Border (Seam Fix)
                Rect dilated = new Rect(dst.left - 1, dst.top - 1, dst.right + 1, dst.bottom + 1);
                canvas.drawBitmap(piece, null, dilated, null);
                // Original
                canvas.drawBitmap(piece, null, dst, null);
            }
        } catch (Exception e) { }
    }

    private void updateModelTexture(byte[] textureData) {
        handler.postDelayed(() -> applyTextureWithRetry(textureData, 0), 1000);
    }

    private void applyTextureWithRetry(byte[] textureData, int retryCount) {
        try {
            Log.d(TAG, "Texture application attempt: " + retryCount);

            // FIX: Using ViewModelProvider to safely get Objects
            List<Object3DData> allObjects = getAllObjectsFromScene();

            if (allObjects != null && !allObjects.isEmpty()) {
                boolean textureApplied = false;
                int appliedCount = 0;

                Log.d(TAG, "Found objects: " + allObjects.size());

                for (Object3DData object : allObjects) {
                    if (object != null && applyTextureToObject(object, textureData)) {
                        textureApplied = true;
                        appliedCount++;
                    }
                }

                if (textureApplied) {
                    isTextureApplied = true;
                    Toast.makeText(RealTextureActivity.this, "Texture Applied to " + appliedCount + " parts!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Texture applied successfully!");
                } else if (retryCount < 10) {
                    handler.postDelayed(() -> applyTextureWithRetry(textureData, retryCount + 1), 1000);
                }
            } else if (retryCount < 10) {
                Log.d(TAG, "No objects found, retrying... " + retryCount);
                handler.postDelayed(() -> applyTextureWithRetry(textureData, retryCount + 1), 1000);
            } else {
                Toast.makeText(this, "Failed to find 3D Objects", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying texture", e);
        }
    }

    private List<Object3DData> getAllObjectsFromScene() {
        try {
            // Activity સ્કોપમાં રહેલા ViewModel ને એક્સેસ કરો
            ModelViewModel modelViewModel = new ViewModelProvider(this).get(ModelViewModel.class);

            if (modelViewModel.getModelEngine() != null && modelViewModel.getModelEngine().getValue() != null) {
                ModelEngine engine = modelViewModel.getModelEngine().getValue();

                if (engine.getBeanFactory() != null) {
                    Scene scene = engine.getBeanFactory().find(Scene.class);
                    if (scene != null) {
                        return scene.getObjects();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing ViewModel: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private boolean applyTextureToObject(Object3DData object, byte[] textureData) {
        try {
            if (object == null) return false;

            Texture texture = new Texture();
            texture.setData(textureData);

            // Fix: Pass ID as INT (Current Time as Unique ID)
            texture.setId((int) System.currentTimeMillis());

            boolean applied = false;

            // 1. Elements (Parts) પર ટેક્સચર લગાવો
            if (object.getElements() != null && !object.getElements().isEmpty()) {
                Log.d(TAG, "Applying to " + object.getElements().size() + " elements");
                for (org.the3deer.android_3d_model_engine.model.Element element : object.getElements()) {
                    if (element.getMaterial() == null) {
                        element.setMaterial(new Material("elem_mat_" + object.getId()));
                    }
                    element.getMaterial().setColorTexture(texture);
                    element.getMaterial().setAlpha(1.0f);
                    applied = true;
                }
            }

            // 2. Main Object પર પણ લગાવો
            Material material = object.getMaterial();
            if (material == null) {
                material = new Material("main_mat");
                object.setMaterial(material);
            }
            material.setColorTexture(texture);
            material.setAlpha(1.0f);

            object.setChanged(true);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error inside applyTextureToObject", e);
            return false;
        }
    }

    private boolean assetExists(String path) {
        try {
            String[] parts = path.split("/");
            String filename = parts[parts.length - 1];
            String folder = path.substring(0, path.length() - filename.length() - 1);
            String[] list = getAssets().list(folder);
            if (list != null) {
                return Arrays.asList(list).contains(filename);
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContentUtils.setThreadActivity(null);
        handler.removeCallbacksAndMessages(null);
    }
}