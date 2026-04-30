package com.luagame.framework.renderer;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.luagame.framework.core.GameEngine;
import com.luagame.framework.scripting.LuaEngine;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Bridges the Android GL thread to GameEngine.tick().
 * Also notifies Lua when the surface is created or resized.
 */
public class GameRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "GameRenderer";
    private final GameEngine gameEngine;

    public GameRenderer(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "GL Surface created");

        GLES30.glClearColor(0.05f, 0.05f, 0.10f, 1.0f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Let renderer rebuild GPU resources
        gameEngine.getRenderer().onSurfaceCreated();

        // Fire Lua start hook
        LuaEngine lua = gameEngine.getLuaEngine();
        lua.callGlobalFunction("onStart");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "GL Surface changed: " + width + "x" + height);
        GLES30.glViewport(0, 0, width, height);
        gameEngine.getRenderer().onSurfaceChanged(width, height);
        gameEngine.getLuaEngine().callGlobalFunction("onResize", width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        gameEngine.tick();
    }
}
