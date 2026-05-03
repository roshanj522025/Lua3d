package com.endless.engine.core;

import android.content.Context;
import android.util.Log;

import com.endless.engine.audio.AudioManager;
import com.endless.engine.input.InputManager;
import com.endless.engine.renderer.Renderer;
import com.endless.engine.scene.SceneManager;
import com.endless.engine.scripting.LuaEngine;

public class GameEngine {

    private static final String TAG = "GameEngine";
    private static GameEngine instance;

    private Context context;
    private LuaEngine luaEngine;
    private SceneManager sceneManager;
    private Renderer renderer;
    private InputManager inputManager;
    private AudioManager audioManager;

    private long lastFrameTime;
    private boolean initialized = false;
    private boolean running = false;

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

        inputManager  = new InputManager();
        sceneManager  = new SceneManager();
        renderer      = new Renderer(sceneManager);
        audioManager  = new AudioManager(this.context);
        luaEngine     = new LuaEngine(this);

        lastFrameTime = System.nanoTime();
        initialized   = true;
        running       = true;
        Log.i(TAG, "Engine initialized.");
    }

    public void queueAssetScript(String path) {
        pendingScript  = path;
        pendingIsAsset = true;
    }

    public void queueStringScript(String code) {
        pendingScript  = code;
        pendingIsAsset = false;
    }

    public void onGLReady() {
        sceneManager = new SceneManager();
        renderer.setSceneManager(sceneManager);
        luaEngine.resetSceneAPI(sceneManager);

        lastFrameTime = System.nanoTime();
        running = true;

        if (pendingScript != null) {
            if (pendingIsAsset) luaEngine.executeAssetScript(pendingScript);
            else                luaEngine.executeString(pendingScript);
        }
    }

    public void tick() {
        if (!running) return;
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrameTime) / 1_000_000_000f, 0.05f);
        lastFrameTime = now;

        inputManager.update();
        luaEngine.callGlobalFunction("onUpdate", (Object) dt);
        sceneManager.update(dt);
        renderer.render();
    }

    public void onResume() {
        running = true;
        lastFrameTime = System.nanoTime();
        audioManager.onResume();
    }

    public void onPause() {
        running = false;
        audioManager.onPause();
    }

    public void onDestroy() {
        running = false;
        audioManager.release();
    }

    public Context       getContext()      { return context;      }
    public LuaEngine     getLuaEngine()    { return luaEngine;    }
    public SceneManager  getSceneManager() { return sceneManager; }
    public Renderer      getRenderer()     { return renderer;     }
    public InputManager  getInputManager() { return inputManager; }
    public AudioManager  getAudioManager() { return audioManager; }
}
