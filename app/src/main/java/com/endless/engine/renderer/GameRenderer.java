package com.endless.engine.renderer;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import com.endless.engine.core.GameActivity;
import com.endless.engine.core.GameEngine;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {

    private final GameEngine gameEngine;
    private int frameCount = 0;

    public GameRenderer(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GameActivity.log("[GL] onSurfaceCreated - thread: " + Thread.currentThread().getName());
        GLES30.glClearColor(0.01f, 0.01f, 0.05f, 1.0f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);

        gameEngine.getRenderer().onSurfaceCreated();
        GameActivity.log("[GL] Shader compiled OK");

        gameEngine.onGLReady();
        GameActivity.log("[GL] onGLReady() done");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GameActivity.log("[GL] onSurfaceChanged: " + width + "x" + height);
        GLES30.glViewport(0, 0, width, height);
        gameEngine.getRenderer().onSurfaceChanged(width, height);
        gameEngine.getLuaEngine().callGlobalFunction("onResize", width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        gameEngine.tick();
        frameCount++;
        if (frameCount == 1 || frameCount == 10 || frameCount == 60) {
            int nodeCount = gameEngine.getSceneManager().getVisibleNodes().size();
            GameActivity.log("[GL] frame=" + frameCount + " nodes=" + nodeCount);
        }
    }
}
