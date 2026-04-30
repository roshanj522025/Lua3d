package com.luagame.framework.core;

import android.content.Context;
import android.util.Log;

import com.luagame.framework.input.InputManager;
import com.luagame.framework.renderer.Renderer;
import com.luagame.framework.scene.SceneManager;
import com.luagame.framework.scripting.LuaEngine;

public class GameEngine {

    private static final String TAG = "GameEngine";
    private static GameEngine instance;

    private Context context;
    private LuaEngine luaEngine;
    private SceneManager sceneManager;
    private Renderer renderer;
    private InputManager inputManager;

    private long lastFrameTime;
    private boolean initialized = false;
    private boolean running = false;

    // Script to execute once onSurfaceCreated fires (set from UI thread, read on GL thread)
    private volatile String pendingScript     = null;
    private volatile boolean pendingIsAsset   = false;

    private GameEngine() {}

    public static GameEngine getInstance() {
        if (instance == null) instance = new GameEngine();
        return instance;
    }

    /** Call before re-using the singleton (e.g. Activity recreated). */
    public static void reset() {
        instance = null;
    }

    public void initialize(Context context) {
        if (initialized) return;
        this.context = context.getApplicationContext();

        inputManager = new InputManager();
        sceneManager = new SceneManager();
        renderer     = new Renderer(sceneManager);
        luaEngine    = new LuaEngine(this);

        lastFrameTime = System.nanoTime();
        initialized   = true;
        running       = true;
        Log.i(TAG, "Engine initialized.");
    }

    /** Set the asset script to run on the GL thread after surface is created. */
    public void setPendingAssetScript(String assetPath) {
        pendingScript   = assetPath;
        pendingIsAsset  = true;
    }

    /**
     * Called from GameRenderer.onSurfaceCreated() ON THE GL THREAD.
     * Runs any pending script now that the GL context + shader are ready.
     */
    public void onGLReady() {
        Log.i(TAG, "GL ready. pendingScript=" + pendingScript);
        if (pendingScript != null) {
            String script  = pendingScript;
            boolean isAsset = pendingIsAsset;
            pendingScript  = null;
            if (isAsset) {
                luaEngine.executeAssetScript(script);
            } else {
                luaEngine.executeString(script);
            }
        }
    }

    /** Called every frame from GL thread. */
    public void tick() {
        if (!running) return;

        long now = System.nanoTime();
        float dt = (now - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = now;
        dt = Math.min(dt, 0.05f);

        inputManager.update();
        luaEngine.callGlobalFunction("onUpdate", (double) dt);
        sceneManager.update(dt);
        renderer.render();
    }

    public void onResume() {
        running = true;
        lastFrameTime = System.nanoTime();
        luaEngine.callGlobalFunction("onResume");
    }

    public void onPause() {
        running = false;
        luaEngine.callGlobalFunction("onPause");
    }

    public void onDestroy() {
        running = false;
        luaEngine.close();
    }

    public Context      getContext()      { return context;      }
    public LuaEngine    getLuaEngine()    { return luaEngine;    }
    public SceneManager getSceneManager() { return sceneManager; }
    public Renderer     getRenderer()     { return renderer;     }
    public InputManager getInputManager() { return inputManager; }
}
