package com.example.a3dviewapp;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class activity_share extends AppCompatActivity {

    private ImageView imgPreview;
    private ImageButton btnBack, btnHome, btnFacebook, btnWhatsapp, btnInstagram, btnMore, btnDownload;
    private String imageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_share);

        // 1. UI Initialization
        imgPreview = findViewById(R.id.imgPreview);
        btnBack = findViewById(R.id.btnBack);
        btnHome = findViewById(R.id.btnHome);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnWhatsapp = findViewById(R.id.btnWhatsapp);
        btnInstagram = findViewById(R.id.btnInstagram);
        btnMore = findViewById(R.id.btnMore);
        btnDownload = findViewById(R.id.btnDownload); // Assuming you have a download button in XML

        // 2. Load Image from Intent
        imageUrl = getIntent().getStringExtra("IMAGE_URL");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl).into(imgPreview);
        }

        // 3. Button Click Listeners
        // In activity_share.java
        btnBack.setOnClickListener(v -> {
             startActivity(new Intent(activity_share.this,MainActivity.class));

        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, activity_home.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Share buttons (Share actual file)
        btnWhatsapp.setOnClickListener(v -> processImageAction("SHARE", "com.whatsapp"));
        btnFacebook.setOnClickListener(v -> processImageAction("SHARE", "com.facebook.katana"));
        btnInstagram.setOnClickListener(v -> processImageAction("SHARE", "com.instagram.android"));
        btnMore.setOnClickListener(v -> processImageAction("SHARE", null));

        // Download button (Save to Gallery)
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> processImageAction("SAVE", null));
        }

        // Apply Window Insets
        View mainView = findViewById(R.id.activity_share);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // સિસ્ટમ બેક બટન માટેનું લોજિક
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // અહીં તમે તમારો Intent પાસ કરી શકો છો
                    Intent intent = new Intent(activity_share.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // આ એક્ટિવિટી બંધ કરવા માટે
                }
            });
        }


    }

    /**
     * Helper to download image via Picasso and then either SHARE or SAVE
     */
    private void processImageAction(String action, String packageName) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Image URL not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show();

        Picasso.get().load(imageUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (action.equals("SHARE")) {
                    handleShare(bitmap, packageName);
                } else {
                    handleSave(bitmap);
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Toast.makeText(activity_share.this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        });
    }

    private void handleShare(Bitmap bitmap, String packageName) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "shared_design.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

            if (packageName != null) shareIntent.setPackage(packageName);
            startActivity(Intent.createChooser(shareIntent, "Share via"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSave(Bitmap bitmap) {
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
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
        }
    }


}