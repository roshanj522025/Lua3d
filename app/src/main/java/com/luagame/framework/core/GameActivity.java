package com.luagame.framework.core;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.luagame.framework.renderer.GameGLSurfaceView;

import java.io.InputStream;

public class GameActivity extends AppCompatActivity {

    private static final int REQUEST_OPEN_LUA = 1001;
    private GameGLSurfaceView glSurfaceView;
    private GameEngine gameEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        // Fresh engine each time the Activity is created
        GameEngine.reset();
        gameEngine = GameEngine.getInstance();
        gameEngine.initialize(this);

        // Register the demo script BEFORE creating the GLSurfaceView.
        // GameRenderer.onSurfaceCreated will pick it up and run it on the GL thread.
        gameEngine.setPendingAssetScript("scripts/main.lua");

        // Now create the surface (this kicks off GL initialization)
        glSurfaceView = new GameGLSurfaceView(this, gameEngine);

        FrameLayout root = new FrameLayout(this);
        root.addView(glSurfaceView);

        Button openBtn = new Button(this);
        openBtn.setText("Open .lua");
        openBtn.setAlpha(0.75f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.END
        );
        lp.setMargins(0, 24, 24, 0);
        openBtn.setLayoutParams(lp);
        openBtn.setOnClickListener(v -> openFilePicker());
        root.addView(openBtn);

        setContentView(root);
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
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new Exception("Could not open stream");
            byte[] bytes = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(bytes);
            final String code = new String(bytes);
            // Always run script on GL thread
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

    @Override protected void onResume()  { super.onResume();  glSurfaceView.onResume();  gameEngine.onResume();  }
    @Override protected void onPause()   { super.onPause();   glSurfaceView.onPause();   gameEngine.onPause();   }
    @Override protected void onDestroy() { super.onDestroy(); gameEngine.onDestroy(); }
}
