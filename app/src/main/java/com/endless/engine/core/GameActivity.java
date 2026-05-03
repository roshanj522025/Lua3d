package com.endless.engine.core;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.endless.engine.renderer.GameGLSurfaceView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    private static final int REQUEST_OPEN_LUA = 1001;

    private GameGLSurfaceView glSurfaceView;
    private GameEngine gameEngine;

    // Log overlay
    private TextView  logView;
    private ScrollView logScroll;
    private final List<String> logLines = new ArrayList<>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // Toolbar buttons
    private Button openBtn;
    private Button reloadBtn;
    private Button logToggleBtn;
    private boolean logVisible = true;

    // Last loaded script (for reload)
    private String lastScriptCode   = null;
    private String lastScriptAsset  = null; // non-null = asset path

    public static GameActivity instance;

    public static void log(String msg) {
        android.util.Log.i("Endless", msg);
        if (instance != null) instance.appendLog(msg);
    }

    private void appendLog(String msg) {
        uiHandler.post(() -> {
            logLines.add(msg);
            if (logLines.size() > 40) logLines.remove(0);
            StringBuilder sb = new StringBuilder();
            for (String l : logLines) sb.append(l).append("\n");
            logView.setText(sb.toString());
            logScroll.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        // ── Root layout ───────────────────────────────────────────────────────
        FrameLayout root = new FrameLayout(this);

        // ── GL Surface ────────────────────────────────────────────────────────
        gameEngine = GameEngine.getInstance();
        gameEngine.initialize(this);
        glSurfaceView = new GameGLSurfaceView(this, gameEngine);
        root.addView(glSurfaceView);

        // ── Top toolbar ───────────────────────────────────────────────────────
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(Color.argb(200, 10, 10, 20));
        toolbar.setPadding(12, 8, 12, 8);

        openBtn = makeToolbarButton("📂 Open .lua");
        openBtn.setOnClickListener(v -> openFilePicker());

        reloadBtn = makeToolbarButton("🔄 Reload");
        reloadBtn.setEnabled(false);
        reloadBtn.setAlpha(0.4f);
        reloadBtn.setOnClickListener(v -> reloadScript());

        logToggleBtn = makeToolbarButton("📋 Log");
        logToggleBtn.setOnClickListener(v -> toggleLog());

        Button demoBtn = makeToolbarButton("▶ Demo");
        demoBtn.setOnClickListener(v -> loadDemo());

        toolbar.addView(openBtn);
        toolbar.addView(makeSpacerView());
        toolbar.addView(reloadBtn);
        toolbar.addView(makeSpacerView());
        toolbar.addView(logToggleBtn);
        toolbar.addView(makeSpacerView());
        toolbar.addView(demoBtn);

        FrameLayout.LayoutParams toolbarParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP);
        root.addView(toolbar, toolbarParams);

        // ── Log overlay ───────────────────────────────────────────────────────
        logScroll = new ScrollView(this);
        logScroll.setBackgroundColor(Color.argb(190, 0, 0, 0));
        logView = new TextView(this);
        logView.setTextColor(Color.GREEN);
        logView.setTextSize(10f);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(8, 4, 8, 4);
        logView.setText("=== Endless Engine starting ===\n");
        logScroll.addView(logView);

        FrameLayout.LayoutParams logParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 220, Gravity.BOTTOM);
        root.addView(logScroll, logParams);

        setContentView(root);

        // ── Show launcher dialog ──────────────────────────────────────────────
        showLaunchDialog();
    }

    // ── Launcher dialog ───────────────────────────────────────────────────────

    private void showLaunchDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Endless Engine")
            .setMessage("Load a Lua script from your device, or run the built-in demo.")
            .setPositiveButton("📂 Open .lua file", (d, w) -> openFilePicker())
            .setNegativeButton("▶ Run demo", (d, w) -> loadDemo())
            .setCancelable(false)
            .show();
    }

    // ── Script loading ────────────────────────────────────────────────────────

    private void loadDemo() {
        lastScriptAsset = "scripts/main.lua";
        lastScriptCode  = null;
        enableReload();
        glSurfaceView.queueEvent(() -> {
            gameEngine.queueAssetScript("scripts/main.lua");
            gameEngine.onGLReady();
        });
        log("[App] Loaded demo: scripts/main.lua");
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
            new String[]{"text/plain", "application/octet-stream", "text/x-lua"});
        startActivityForResult(intent, REQUEST_OPEN_LUA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_LUA && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            loadLuaFromUri(data.getData());
        }
    }

    private void loadLuaFromUri(Uri uri) {
        log("[App] Reading: " + uri.getLastPathSegment());
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new Exception("Stream null");
            byte[] bytes = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(bytes);
            final String code = new String(bytes);
            lastScriptCode  = code;
            lastScriptAsset = null;
            enableReload();
            log("[App] Script " + bytes.length + " bytes — executing...");
            glSurfaceView.queueEvent(() -> {
                gameEngine.queueStringScript(code);
                gameEngine.onGLReady();
            });
        } catch (Exception e) {
            log("[App] ERROR: " + e.getMessage());
            showError("Failed to load script", e.getMessage());
        }
    }

    private void reloadScript() {
        if (lastScriptAsset != null) {
            log("[App] Reloading: " + lastScriptAsset);
            glSurfaceView.queueEvent(() -> {
                gameEngine.queueAssetScript(lastScriptAsset);
                gameEngine.onGLReady();
            });
        } else if (lastScriptCode != null) {
            log("[App] Reloading last script...");
            final String code = lastScriptCode;
            glSurfaceView.queueEvent(() -> {
                gameEngine.queueStringScript(code);
                gameEngine.onGLReady();
            });
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void toggleLog() {
        logVisible = !logVisible;
        logScroll.setVisibility(logVisible
            ? android.view.View.VISIBLE
            : android.view.View.GONE);
        logToggleBtn.setText(logVisible ? "📋 Log" : "📋 Log ○");
    }

    private void enableReload() {
        uiHandler.post(() -> {
            reloadBtn.setEnabled(true);
            reloadBtn.setAlpha(0.9f);
        });
    }

    private Button makeToolbarButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11f);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.argb(180, 40, 40, 80));
        btn.setPadding(20, 8, 20, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(4, 0, 4, 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private android.view.View makeSpacerView() {
        android.view.View spacer = new android.view.View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
        spacer.setLayoutParams(lp);
        return spacer;
    }

    private void showError(String title, String msg) {
        uiHandler.post(() ->
            new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show()
        );
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        gameEngine.onResume();
    }

    @Override protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        gameEngine.onPause();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        instance = null;
        gameEngine.onDestroy();
    }
}
