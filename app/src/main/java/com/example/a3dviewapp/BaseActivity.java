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
import android.view.KeyEvent;
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

        // Internet check karvanu chalu karo
        registerNetworkMonitor();
    }

    // --- SYSTEM BACK BUTTON LOGIC (Entire App mate) ---
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Jyare button dabavyu (Down)
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                backPressedTime = System.currentTimeMillis();
                return true;
            }
            // Jyare button chhodyu (Up)
            else if (event.getAction() == KeyEvent.ACTION_UP) {
                long pressDuration = System.currentTimeMillis() - backPressedTime;

                if (pressDuration > 1000) {
                    // 1. LONG PRESS (1 second thi vadhu) -> Show Exit Dialog
                    showAppExitDialog();
                } else {
                    // 2. SINGLE CLICK -> Normal Back (Finish Activity)
                    finish();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // --- EXIT POPUP METHOD ---
    public void showAppExitDialog() {
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
            finishAffinity(); // Full App Exit
            System.exit(0);
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- NETWORK MONITOR LOGIC ---
    private void registerNetworkMonitor() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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

        internetDialog.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}