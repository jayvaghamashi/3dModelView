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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class activity_seondquestion extends AppCompatActivity {
    private FloatingActionButton btnNext;
    private ConstraintLayout selectedContainer = null;
    private String selectedType = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seondquestion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.secondactivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        btnNext = findViewById(R.id.btnNext);

        List<ConstraintLayout> containers = Arrays.asList(
                findViewById(R.id.containerTShirts),
                findViewById(R.id.containerShirt),
                findViewById(R.id.containerpant),
                findViewById(R.id.containerSetting)
        );

        final Map<Integer, String> textViewIds = new HashMap<>();
        textViewIds.put(R.id.containerTShirts, "Blocky");
        textViewIds.put(R.id.containerShirt, "Rounded");
        textViewIds.put(R.id.containerpant, "Man");
        textViewIds.put(R.id.containerSetting, "Girl");

        for (final ConstraintLayout container : containers) {
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (ConstraintLayout c : containers) {
                        c.setSelected(false);
                    }

                    container.setSelected(true);
                    selectedContainer = container;
                    selectedType = textViewIds.get(container.getId());

                    btnNext.setEnabled(true);
                    btnNext.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));

                   Toast.makeText(activity_seondquestion.this, selectedType + " selected", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedContainer != null) {
                    Intent intent = new Intent(activity_seondquestion.this, activity_thirdquestion.class);
                    intent.putExtra("SELECTED_TYPE", selectedType);
                    intent.putExtra("SELECTED_IMAGE", getSelectedImageResource(selectedContainer.getId()));

                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    Toast.makeText(activity_seondquestion.this, "Please select an option first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private int getSelectedImageResource(int containerId) {
        if (containerId == R.id.containerTShirts) {
            return R.drawable.women2;
        } else if (containerId == R.id.containerShirt) {
            return R.drawable.women1;
        } else if (containerId == R.id.containerpant) {
            return R.drawable.women3;
        } else if (containerId == R.id.containerSetting) {
            return R.drawable.women1;
        } else {
            return R.drawable.women1;
        }
    }
}