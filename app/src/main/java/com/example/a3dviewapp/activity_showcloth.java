package com.example.a3dviewapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;
import java.util.List;

public class activity_showcloth extends BaseActivity {
    private String category;

    ImageView btnBack;
    private ConstraintLayout selectedContainer = null; // Home activity જેવું જ વેરીએબલ

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_showcloth);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.showactivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack =findViewById(R.id.btnclothBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(activity_showcloth.this,activity_home.class));
                finish();
            }
        });

        Intent intent = getIntent();
        category = intent.getStringExtra("CATEGORY");
        if (category == null) category = "tshirts";

        TextView tvTitle = findViewById(R.id.tvTitle);
        //tvTitle.setText(category);

        List<ConstraintLayout> containers = Arrays.asList(
                findViewById(R.id.containerTop),
                findViewById(R.id.containerPopular),
                findViewById(R.id.containerlatest)
        );

        // ટેક્સ્ટ સેટ કરો
        ((TextView) findViewById(R.id.tvTshirt)).setText(" Top"  );
        ((TextView) findViewById(R.id.tvShirt)).setText("Popular " );
        ((TextView) findViewById(R.id.tvPant)).setText("Latest " );

        for (int i = 0; i < containers.size(); i++) {
            final int index = i;
            final ConstraintLayout currentLayout = containers.get(i);

            currentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Home Activity મુજબની ઇફેક્ટ
                    handleContainerClick(currentLayout);

                    String subCategory;
                    switch (index) {
                        case 0: subCategory = "Top"; break;
                        case 1: subCategory = "Popular"; break;
                        case 2: subCategory = "Latest"; break;
                        default: subCategory = "";
                    }
                    openClothDetailsActivity(category, subCategory);
                }
            });
        }
    }

    private void handleContainerClick(ConstraintLayout container) {
        // ૧. જૂનું સિલેક્શન રીસેટ કરો (Alpha 1.0)
        if (selectedContainer != null) {
            selectedContainer.setSelected(false);
            selectedContainer.setAlpha(1.0f);
        }

        // ૨. નવું સિલેક્શન સેટ કરો (Alpha 0.8 - Home Activity મુજબ)
        container.setSelected(true);
        container.setAlpha(0.8f);
        selectedContainer = container;
    }

    private void openClothDetailsActivity(String category, String subCategory) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("CATEGORY", category);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // પાછા ફરો ત્યારે ઇફેક્ટ કાઢી નાખવા માટે
        if (selectedContainer != null) {
            selectedContainer.setAlpha(1.0f);
            selectedContainer.setSelected(false);
            selectedContainer = null;
        }
    }
}