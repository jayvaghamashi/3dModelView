package com.example.a3dviewapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;
import java.util.List;

public class activity_showcloth extends AppCompatActivity {
    private String category; // Class level variable

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



        // Intent માંથી ડેટા મેળવો
        Intent intent = getIntent();
           category = intent.getStringExtra("CATEGORY");
        if (category == null) {
            category = "tshirts";
        }
        int backgroundRes = intent.getIntExtra("BACKGROUND_RES", R.drawable.t_shirtpink);

        // Title સેટ કરો
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(category);

        // ડિકલેર કરેલા વેરીએબલ
        List<ConstraintLayout> containers = Arrays.asList(
                findViewById(R.id.containerTop),
                findViewById(R.id.containerPopular),
                findViewById(R.id.containerlatest)
        );

        // ટેક્સ્ટ માં category ઉમેરો
        TextView tvTop = findViewById(R.id.tvTshirt);
        TextView tvPopular = findViewById(R.id.tvShirt);
        TextView tvLatest = findViewById(R.id.tvPant);

        tvTop.setText(category + " Top");
        tvPopular.setText("Popular " + category);
        tvLatest.setText("Latest " + category);

        // દરેક કન્ટેઈનર માટે ક્લિક લિસનર
        for (int i = 0; i < containers.size(); i++) {
            final int index = i;
            containers.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // અહીં તમે sub-category પસંદ કરેલ બાદ next activity માં જઈ શકો છો
                    String subCategory;
                    switch (index) {
                        case 0:
                            subCategory = "Top";
                            break;
                        case 1:
                            subCategory = "Popular";
                            break;
                        case 2:
                            subCategory = "Latest";
                            break;
                        default:
                            subCategory = "";
                    }

                    openClothDetailsActivity(category, subCategory);
                }
            });
        }

        // Back button functionality (optional)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void openClothDetailsActivity(String category, String subCategory) {
        // Cloth details activity માટે
         Intent intent = new Intent(this, MainActivity.class);
         intent.putExtra("CATEGORY", category);
        // intent.putExtra("SUB_CATEGORY", subCategory);
         startActivity(intent);

        // For now show toast
        Toast.makeText(
                this,
                "Selected: " + category + " - " + subCategory,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}