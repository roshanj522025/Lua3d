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
    private boolean glReady = false;

    // Script to auto-load once GL is ready (set before GL surface created)
    private String pendingScript = null;
    private boolean pendingIsAsset = false;

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

    /** Called from GL thread when surface + shader are ready. Runs any pending script. */
    public void onGLReady() {
        glReady = true;
        Log.i(TAG, "GL ready.");
        if (pendingScript != null) {
            String script = pendingScript;
            boolean isAsset = pendingIsAsset;
            pendingScript = null;
            if (isAsset) {
                luaEngine.executeAssetScript(script);
            } else {
                luaEngine.executeString(script);
            }
        }
    }

    /** Queue a script to run (or run immediately if GL is already ready). */
    public void loadAssetScript(String path) {
        if (glReady) {
            luaEngine.executeAssetScript(path);
        } else {
            pendingScript = path;
            pendingIsAsset = true;
        }
    }

    public void loadStringScript(String code) {
        if (glReady) {
            luaEngine.executeString(code);
        } else {
            pendingScript = code;
            pendingIsAsset = false;
        }
    }

    public void tick() {
        if (!running) return;

        long now = System.nanoTime();
        float dt = (now - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = now;
        dt = Math.min(dt, 0.05f);

        inputManager.update();
        luaEngine.callGlobalFunction("onUpdate", dt);
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
