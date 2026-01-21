package com.example.a3dviewapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;
import java.util.List;

public class activity_home extends AppCompatActivity {
    private ConstraintLayout selectedContainer = null;
    private String selectedItemName = "";
    private int selectedBackgroundRes = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeactivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        List<ConstraintLayout> containers = Arrays.asList(
                findViewById(R.id.containerTShirts),
                findViewById(R.id.containerShirt),
                findViewById(R.id.containerpant),
                findViewById(R.id.containerSetting)
        );

        for (ConstraintLayout container : containers) {
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleContainerClick((ConstraintLayout) v);
                }
            });
        }

        // લોંગ પ્રેસ લિસનર
        findViewById(R.id.containerTShirts).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity_home.this, "T-shirts: Choose from various T-shirt styles", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        findViewById(R.id.containerShirt).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity_home.this, "Shirts: Formal and casual shirts", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        findViewById(R.id.containerpant).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity_home.this, "Pants: Different types of pants", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        findViewById(R.id.containerSetting).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity_home.this, "Settings: App settings and preferences", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void handleContainerClick(ConstraintLayout container) {
        // પહેલાનું સિલેક્ટેડ હોય તો તેને ડિસિલેક્ટ કરો
        if (selectedContainer != null) {
            selectedContainer.setSelected(false);
        }

        // નવું સિલેક્ટ કરો
        container.setSelected(true);
        selectedContainer = container;

        // સિલેક્ટેડ આઇટમની માહિતી સેવ કરો
        int containerId = container.getId();

        if (containerId == R.id.containerTShirts) {
            selectedItemName = "T-shirts";
            selectedBackgroundRes = R.drawable.t_shirtpink;
            openShowActivity("T-shirts");
        } else if (containerId == R.id.containerShirt) {
            selectedItemName = "Shirts";
            selectedBackgroundRes = R.drawable.shirt;
            openShowActivity("Shirts");
        } else if (containerId == R.id.containerpant) {
            selectedItemName = "Pants";
            selectedBackgroundRes = R.drawable.pant;
            openShowActivity("Pants");
        } else if (containerId == R.id.containerSetting) {
            selectedItemName = "Settings";
            selectedBackgroundRes = R.drawable.setting;
            openSettingsActivity();
        }

        // હાઈલાઇટ effect આપો
        highlightSelectedContainer(container);
    }

    private void highlightSelectedContainer(ConstraintLayout container) {
        // Dark overlay effect
        container.setAlpha(0.8f);

        // પછીના સિલેક્શન માટે reset
        if (selectedContainer != null) {
            selectedContainer.setAlpha(1.0f);
        }
    }

    private void openShowActivity(String category) {
        Intent intent = new Intent(activity_home.this, activity_showcloth.class);
        intent.putExtra("CATEGORY", category);
        intent.putExtra("BACKGROUND_RES", selectedBackgroundRes);
        startActivity(intent);

        // Transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openSettingsActivity() {
        // Settings activity માટે અલગ activity
        Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, SettingsActivity.class);
        // startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Activity પાછા આવે ત્યારે selection reset
        if (selectedContainer != null) {
            selectedContainer.setSelected(false);
            selectedContainer = null;
        }
    }
}