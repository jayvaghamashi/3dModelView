package com.example.a3dviewapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.picasso.Picasso;

public class activity_share extends BaseActivity {

    private ImageView imgPreview;
    private ImageButton btnBack, btnHome, btnFacebook, btnWhatsapp, btnInstagram, btnMore;
    private String imageUrl = ""; // Variable declare karyu jethi error na ave

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_share);

        // XML ma root layout ni ID "main" hovi joiye
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 1. Initialize UI
        imgPreview = findViewById(R.id.imgPreview);
        btnBack = findViewById(R.id.btnBack);
        btnHome = findViewById(R.id.btnHome);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnWhatsapp = findViewById(R.id.btnWhatsapp);
        btnInstagram = findViewById(R.id.btnInstagram);
        btnMore = findViewById(R.id.btnMore);

        // 2. Get Data from Intent
        imageUrl = getIntent().getStringExtra("IMAGE_URL");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl).into(imgPreview);
        }

        // 3. Click Listeners
        btnBack.setOnClickListener(v -> finish());

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, activity_home.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Social Media Logic
        btnFacebook.setOnClickListener(v -> openLink("https://www.facebook.com/sharer/sharer.php?u=" + imageUrl));

        btnWhatsapp.setOnClickListener(v -> {
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, "Check this out: " + imageUrl);
            try {
                startActivity(whatsappIntent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });

        btnInstagram.setOnClickListener(v -> {
            Intent instaIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
            if (instaIntent != null) {
                startActivity(instaIntent);
            } else {
                openLink("https://www.instagram.com");
            }
        });

        btnMore.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Cloth Design: " + imageUrl);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
    }

    private void openLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}