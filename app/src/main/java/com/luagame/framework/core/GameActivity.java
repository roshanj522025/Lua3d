package com.luagame.framework.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.luagame.framework.renderer.GameGLSurfaceView;
import com.luagame.framework.scripting.LuaEngine;

import java.io.InputStream;

/**
 * Main entry Activity for the Lua Game Framework.
 * Shows a floating "Open Lua Script" button so the user can pick any .lua
 * file from their device.  Falls back to the bundled demo if they dismiss.
 */
public class GameActivity extends Activity {

    private static final int REQUEST_OPEN_LUA = 1001;

    private GameGLSurfaceView glSurfaceView;
    private GameEngine gameEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen, keep screen on
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        // Initialize core engine
        gameEngine = GameEngine.getInstance();
        gameEngine.initialize(this);

        // Create OpenGL surface as the base layer
        glSurfaceView = new GameGLSurfaceView(this, gameEngine);

        // Overlay a small "Open Script" button in the top-right corner
        FrameLayout root = new FrameLayout(this);
        root.addView(glSurfaceView);

        Button openBtn = new Button(this);
        openBtn.setText("📂 Open .lua");
        openBtn.setAlpha(0.85f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.END
        );
        lp.setMargins(0, 32, 32, 0);
        openBtn.setLayoutParams(lp);
        openBtn.setOnClickListener(v -> openFilePicker());
        root.addView(openBtn);

        setContentView(root);

        // Ask user to pick a script, or load the bundled demo
        showLaunchDialog();
    }

    /** Dialog on first launch: open a file or run the bundled demo */
    private void showLaunchDialog() {
        new AlertDialog.Builder(this)
            .setTitle("LuaGameFramework")
            .setMessage("Open a .lua script from your device, or run the bundled solar-system demo.")
            .setPositiveButton("Open .lua file", (d, w) -> openFilePicker())
            .setNegativeButton("Run demo", (d, w) -> loadBundledDemo())
            .setCancelable(false)
            .show();
    }

    /** Launch the system file picker filtered to .lua / plain-text files */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Accept plain text (covers .lua) and any file as fallback
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "text/plain",
            "application/octet-stream",
            "text/x-lua"
        });
        startActivityForResult(intent, REQUEST_OPEN_LUA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_LUA) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                loadLuaFromUri(data.getData());
            } else {
                // User cancelled the picker — fall back to bundled demo
                loadBundledDemo();
            }
        }
    }

    /** Read the picked URI and execute its contents as Lua */
    private void loadLuaFromUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new Exception("Could not open stream");
            byte[] bytes = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(bytes);
            String code = new String(bytes);
            // Execute on the GL thread so OpenGL calls are safe
            glSurfaceView.queueEvent(() ->
                gameEngine.getLuaEngine().executeString(code)
            );
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                .setTitle("Error loading script")
                .setMessage(e.getMessage())
                .setPositiveButton("OK", null)
                .show();
        }
    }

    private void loadBundledDemo() {
        glSurfaceView.queueEvent(() ->
            gameEngine.getLuaEngine().executeAssetScript("scripts/main.lua")
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        gameEngine.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        gameEngine.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameEngine.onDestroy();
    }
}
