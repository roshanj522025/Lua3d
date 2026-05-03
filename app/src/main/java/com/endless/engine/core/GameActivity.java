package com.endless.engine.core;

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

    // On-screen log overlay
    private TextView logView;
    private ScrollView logScroll;
    private final List<String> logLines = new ArrayList<>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public static GameActivity instance;

    public static void log(String msg) {
        android.util.Log.i("Lua3D", msg);
        if (instance != null) instance.appendLog(msg);
    }

    private void appendLog(String msg) {
        uiHandler.post(() -> {
            logLines.add(msg);
            if (logLines.size() > 30) logLines.remove(0);
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

        FrameLayout root = new FrameLayout(this);

        // GL Surface
        gameEngine = GameEngine.getInstance();
        gameEngine.initialize(this);
        gameEngine.queueAssetScript("scripts/main.lua");
        glSurfaceView = new GameGLSurfaceView(this, gameEngine);
        root.addView(glSurfaceView);

        // Log overlay — bottom half of screen
        logScroll = new ScrollView(this);
        logScroll.setBackgroundColor(Color.argb(180, 0, 0, 0));
        logView = new TextView(this);
        logView.setTextColor(Color.GREEN);
        logView.setTextSize(10f);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(8, 4, 8, 4);
        logView.setText("=== Lua3D starting ===\n");
        logScroll.addView(logView);

        FrameLayout.LayoutParams logParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 300, Gravity.BOTTOM);
        root.addView(logScroll, logParams);

        // Open .lua button
        Button openBtn = new Button(this);
        openBtn.setText("Open .lua");
        openBtn.setAlpha(0.85f);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.END);
        btnParams.setMargins(0, 24, 24, 0);
        openBtn.setLayoutParams(btnParams);
        openBtn.setOnClickListener(v -> openFilePicker());
        root.addView(openBtn);

        setContentView(root);
        log("onCreate done. GL surface created.");
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
        log("Loading .lua from URI: " + uri);
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new Exception("Could not open stream");
            byte[] bytes = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(bytes);
            final String code = new String(bytes);
            log("Script read: " + bytes.length + " bytes");
            glSurfaceView.queueEvent(() -> {
                gameEngine.queueStringScript(code);
                gameEngine.onGLReady();
            });
        } catch (Exception e) {
            log("ERROR loading script: " + e.getMessage());
        }
    }

    @Override protected void onResume()  { super.onResume();  glSurfaceView.onResume();  gameEngine.onResume();  }
    @Override protected void onPause()   { super.onPause();   glSurfaceView.onPause();   gameEngine.onPause();   }
    @Override protected void onDestroy() { super.onDestroy(); instance = null; gameEngine.onDestroy(); }
}
