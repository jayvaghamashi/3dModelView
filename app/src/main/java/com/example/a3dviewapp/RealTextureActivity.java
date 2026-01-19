package com.example.a3dviewapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.the3deer.android_3d_model_engine.ModelFragment;
import org.the3deer.util.android.ContentUtils;

public class RealTextureActivity extends AppCompatActivity {

    private static final String TAG = "RealTextureActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_texture);

        // એન્જિન માટે જરૂરી સેટઅપ
        ContentUtils.setThreadActivity(this);
        ContentUtils.provideAssets(this);

        // ૧. Intent માંથી ID અને નામ મેળવો
        String productId = getIntent().getStringExtra("product_id");
        String productName = getIntent().getStringExtra("product_name");

        if (productName != null) {
            setTitle(productName);
        }

        // ૨. એસેટ્સ પાથ નક્કી કરો (દા.ત. models/man.glb)
        // જો તમારી ફાઈલ .glb હોય તો ".glb" અને .dae હોય તો ".dae" લખો
        String modelPath = "models/" + productId + ".glb";

        Log.d(TAG, "Loading model: " + modelPath);

        // ૩. મોડલ લોડ કરો
        load3DModel(modelPath);
    }

    private void load3DModel(String modelPath) {
        try {
            // URI બનાવતી વખતે 'android://' પ્રોટોકોલ વાપરવો જેથી "URI is not absolute" એરર ના આવે
            String uri = "android://com.example.a3dviewapp/assets/" + modelPath;

            // ModelFragment નો ઉપયોગ કરો જે બધું જ આંતરિક રીતે હેન્ડલ કરશે
            // "0" એટલે ઓટો-ડિટેક્ટ ફાઈલ ટાઈપ
            ModelFragment modelFragment = ModelFragment.newInstance(uri, "0", false);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, modelFragment, "model_tag")
                    .commit();

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            Toast.makeText(this, "મોડલ લોડ કરવામાં ભૂલ આવી", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // મેમરી ક્લીનઅપ
        ContentUtils.setThreadActivity(null);
    }
}