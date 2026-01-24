package org.the3deer.android_3d_model_engine;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.the3deer.android_3d_model_engine.services.SceneLoader;
import org.the3deer.android_3d_model_engine.view.GLFragment;
import org.the3deer.android_3d_model_engine.view.GLSurfaceView;

public class ModelFragment extends Fragment {

    private static final String TAG = ModelFragment.class.getSimpleName();

    private ModelViewModel viewModel;
    private final Handler handler;

    protected ModelEngine modelEngine;
    private boolean immersiveMode;

    // 🔥 IMPORTANT FLAG
    private boolean backEnabled = false;

    public ModelFragment() {
        super(R.layout.fragment_model);
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static ModelFragment newInstance(String uri, String type, boolean demo) {
        ModelFragment frag = new ModelFragment();
        Bundle args = new Bundle();
        args.putString("uri", uri);
        args.putString("type", type);
        args.putBoolean("demo", demo);
        frag.setArguments(args);
        return frag;
    }

    // 🔥 Activity control method
    public void setBackEnabled(boolean enabled) {
        this.backEnabled = enabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Loading ModelFragment...");
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ModelViewModel.class);

        modelEngine = viewModel.getModelEngine().getValue();
        if (modelEngine == null) {
            modelEngine = ModelEngine.newInstance(requireActivity(), savedInstanceState, getArguments());
        }

        viewModel.setModelEngine(modelEngine);

        modelEngine.getBeanFactory().addOrReplace("extras", getArguments());
        modelEngine.getBeanFactory().addOrReplace("surface", new GLSurfaceView(requireActivity()));
        modelEngine.getBeanFactory().addOrReplace("fragment_gl", new GLFragment());
        modelEngine.getBeanFactory().addOrReplace("scene_0.loader", new SceneLoader());

        modelEngine.init();
        modelEngine.refresh();

        modelEngine.getPreferenceFragment().onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.i(TAG, "Attaching GL fragment...");
        if (modelEngine.getGLFragment() != null) {

            getChildFragmentManager().setFragmentResultListener(
                    "immersive", this,
                    (requestKey, result) -> toggleImmersive()
            );

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.gl_container, (Fragment) modelEngine.getGLFragment(), "surface")
                    .setReorderingAllowed(true)
                    .commit();

            createSettings();
        }
    }

    // 🔥 FIXED BACK HANDLING
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        // 👉 Fragment back disabled → Activity handle kare
                        if (!backEnabled) {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                            setEnabled(true);
                            return;
                        }

                        // 👉 Fragment back enabled
                        Log.v(TAG, "ModelFragment handleOnBackPressed");
                        Bundle result = new Bundle();
                        result.putString("action", "back");
                        getParentFragmentManager().setFragmentResult("app", result);
                    }
                }
        );
    }

    private void createSettings() {
        if (modelEngine.getPreferenceFragment() == null) return;

        FloatingActionButton fab = getView().findViewById(R.id.button_settings);
        fab.setOnClickListener(view -> {

            Fragment settings = getChildFragmentManager().findFragmentByTag("settings");
            if (settings != null) {
                getChildFragmentManager()
                        .beginTransaction()
                        .remove(settings)
                        .commit();
            } else {
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings, modelEngine.getPreferenceFragment(), "settings")
                        .commit();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        modelEngine.getPreferenceFragment().onSaveInstanceState(outState);
    }

    private void toggleImmersive() {
        immersiveMode = !immersiveMode;
        if (immersiveMode) hideSystemUI();
        else showSystemUI();

        Toast.makeText(getContext(),
                "Fullscreen " + immersiveMode,
                Toast.LENGTH_SHORT).show();
    }

    void hideSystemUI() {
        if (!immersiveMode) return;

        View decorView = requireActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    void showSystemUI() {
        handler.removeCallbacksAndMessages(null);
        View decorView = requireActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
}
