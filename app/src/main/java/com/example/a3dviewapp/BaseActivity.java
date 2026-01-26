package com.example.a3dviewapp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private Dialog internetDialog;
    private long backPressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Internet check start
        registerNetworkMonitor();
    }

    // --- SYSTEM BACK BUTTON LOGIC ---
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                backPressedTime = System.currentTimeMillis();
                return true;
            }
            else if (event.getAction() == KeyEvent.ACTION_UP) {
                long pressDuration = System.currentTimeMillis() - backPressedTime;

                if (pressDuration > 1000) {
                    showAppExitDialog();
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // --- EXIT POPUP METHOD ---
    public void showAppExitDialog() {
        if (isFinishing() || isDestroyed()) return;

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_exit);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);

        Button btnYes = dialog.findViewById(R.id.btnYes);
        Button btnNo = dialog.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
            System.exit(0);
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        try {
            dialog.show();
        } catch (WindowManager.BadTokenException e) {
            Log.e("BaseActivity", "Error showing exit dialog: " + e.getMessage());
        }
    }

    // --- NETWORK MONITOR LOGIC ---
    private void registerNetworkMonitor() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (internetDialog != null && internetDialog.isShowing()) {
                        internetDialog.dismiss();
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> showNoInternetDialog());
            }
        });
    }

    private void showNoInternetDialog() {
        // --- SAFETY CHECK: Activity run thai rahi che ke nahi? ---
        if (isFinishing() || isDestroyed()) return;

        if (internetDialog != null && internetDialog.isShowing()) return;

        internetDialog = new Dialog(this);
        internetDialog.setContentView(R.layout.dialog_no_internet);
        internetDialog.setCancelable(false);
        if (internetDialog.getWindow() != null) {
            internetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnTryAgain = internetDialog.findViewById(R.id.btnTryAgain);
        btnTryAgain.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                internetDialog.dismiss();
            } else {
                Toast.makeText(this, "Still no internet...", Toast.LENGTH_SHORT).show();
            }
        });

        // --- SAFETY CHECK: BadTokenException rokva mate ---
        try {
            internetDialog.show();
        } catch (WindowManager.BadTokenException e) {
            Log.e("BaseActivity", "Window token is invalid. Skipping dialog show.");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onDestroy() {
        // Activity destroy thai tyare dialog dismiss karvo jaruri che
        if (internetDialog != null && internetDialog.isShowing()) {
            internetDialog.dismiss();
        }
        super.onDestroy();
    }
}