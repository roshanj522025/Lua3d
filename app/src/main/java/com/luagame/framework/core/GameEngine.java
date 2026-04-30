package com.luagame.framework.core;

import android.content.Context;
import android.util.Log;

import com.luagame.framework.input.InputManager;
import com.luagame.framework.renderer.Renderer;
import com.luagame.framework.scene.SceneManager;
import com.luagame.framework.scripting.LuaEngine;

/**
 * Central singleton that owns and coordinates all engine subsystems:
 *  - Lua scripting engine
 *  - Scene/entity graph
 *  - Renderer (OpenGL ES 3.0)
 *  - Input manager
 *
 * The game loop runs on the GL thread via GameRenderer.onDrawFrame().
 */
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

    // ─── Singleton ────────────────────────────────────────────────────────────

    private GameEngine() {}

    public static GameEngine getInstance() {
        if (instance == null) {
            instance = new GameEngine();
        }
        return instance;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    public void initialize(Context context) {
        if (initialized) return;
        this.context = context.getApplicationContext();

        Log.i(TAG, "Initializing LuaGameFramework engine…");

        inputManager  = new InputManager();
        sceneManager  = new SceneManager();
        renderer      = new Renderer(sceneManager);
        luaEngine     = new LuaEngine(this);

        lastFrameTime = System.nanoTime();
        initialized   = true;
        running       = true;

        Log.i(TAG, "Engine initialized.");
    }

    /**
     * Called every frame from the GL thread.
     * Computes delta-time, fires Lua update hooks, then renders.
     */
    public void tick() {
        if (!running) return;

        long now   = System.nanoTime();
        float dt   = (now - lastFrameTime) / 1_000_000_000f;   // seconds
        lastFrameTime = now;

        // Clamp delta time to avoid spiral-of-death on slow frames
        dt = Math.min(dt, 0.05f);

        // 1. Process input
        inputManager.update();

        // 2. Fire Lua update
        luaEngine.callGlobalFunction("onUpdate", dt);

        // 3. Tick scene graph (transforms, animations)
        sceneManager.update(dt);

        // 4. Render
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

    // ─── Accessors ────────────────────────────────────────────────────────────

    public Context        getContext()      { return context; }
    public LuaEngine      getLuaEngine()    { return luaEngine; }
    public SceneManager   getSceneManager() { return sceneManager; }
    public Renderer       getRenderer()     { return renderer; }
    public InputManager   getInputManager() { return inputManager; }
}
