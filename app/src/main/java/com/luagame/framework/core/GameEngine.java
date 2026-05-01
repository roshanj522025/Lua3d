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

    // Script queued to run on the GL thread once surface is ready
    private volatile String pendingScript   = null;
    private volatile boolean pendingIsAsset = false;

    private GameEngine() {}

    public static GameEngine getInstance() {
        if (instance == null) instance = new GameEngine();
        return instance;
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

    /**
     * Queue a script to run once the GL surface is ready.
     * Safe to call from any thread at any time.
     * The actual execution always happens on the GL thread via onGLReady().
     */
    public void queueAssetScript(String path) {
        pendingScript   = path;
        pendingIsAsset  = true;
        Log.i(TAG, "Script queued: " + path);
    }

    public void queueStringScript(String code) {
        pendingScript   = code;
        pendingIsAsset  = false;
    }

    /**
     * Called from the GL thread inside onSurfaceCreated(), after the shader is built.
     * Resets GL-dependent state and runs any pending script.
     */
    public void onGLReady() {
        // Reset scene — old VAOs/VBOs from previous GL context are gone
        sceneManager = new SceneManager();
        renderer.setSceneManager(sceneManager);
        luaEngine.resetSceneAPI(sceneManager);

        lastFrameTime = System.nanoTime();
        running = true;
        Log.i(TAG, "GL ready. Running pending script: " + pendingScript);

        if (pendingScript != null) {
            String script  = pendingScript;
            boolean isAsset = pendingIsAsset;
            // Don't clear pendingScript — keep it so surface re-creation re-runs it
            if (isAsset) {
                luaEngine.executeAssetScript(script);
            } else {
                luaEngine.executeString(script);
            }
        }
    }

    /** Called every frame on the GL thread. */
    public void tick() {
        if (!running) return;

        long now = System.nanoTime();
        float dt = (now - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = now;
        dt = Math.min(dt, 0.05f);

        inputManager.update();
        luaEngine.callGlobalFunction("onUpdate", (Object) dt);
        sceneManager.update(dt);
        renderer.render();
    }

    public void onResume() {
        running = true;
        lastFrameTime = System.nanoTime();
    }

    public void onPause() {
        running = false;
    }

    public void onDestroy() {
        running = false;
    }

    public Context      getContext()      { return context;      }
    public LuaEngine    getLuaEngine()    { return luaEngine;    }
    public SceneManager getSceneManager() { return sceneManager; }
    public Renderer     getRenderer()     { return renderer;     }
    public InputManager getInputManager() { return inputManager; }
}
