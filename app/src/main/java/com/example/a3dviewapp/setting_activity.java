package com.example.a3dviewapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class setting_activity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Main shape container click listener
        findViewById(R.id.containershape).setOnClickListener(v -> showModelListDialog());

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void showModelListDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_model_list);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Options initialize karo
        LinearLayout optBlocky = dialog.findViewById(R.id.optionBlocky);
        LinearLayout optRounded = dialog.findViewById(R.id.optionRounded);
        LinearLayout optMan = dialog.findViewById(R.id.optionMan);
        LinearLayout optGirl = dialog.findViewById(R.id.optionGirl);

        // Click listeners for each model
        optBlocky.setOnClickListener(v -> saveAndExit(dialog, "models/roblox.dae"));
        optRounded.setOnClickListener(v -> saveAndExit(dialog, "models/rounded.dae"));
        optMan.setOnClickListener(v -> saveAndExit(dialog, "models/man.dae"));
        optGirl.setOnClickListener(v -> saveAndExit(dialog, "models/girl.dae"));

        dialog.show();
    }

    private void saveAndExit(Dialog dialog, String modelPath) {
        // SharedPrefs ma save karo
        getSharedPreferences("ModelPrefs", MODE_PRIVATE)
                .edit()
                .putString("default_model", modelPath)
                .apply();

        dialog.dismiss();
        Toast.makeText(this, "Default Model Updated!", Toast.LENGTH_SHORT).show();

        // Sidhu MainActivity par redirect karo
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}