package com.luagame.framework.renderer;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.luagame.framework.scene.Camera;
import com.luagame.framework.scene.SceneManager;
import com.luagame.framework.scene.SceneNode;

import java.util.List;

public class Renderer {

    private static final String TAG = "Renderer";

    private SceneManager sceneManager;
    private ShaderProgram defaultShader;
    private Camera activeCamera;

    private int screenWidth  = 1;
    private int screenHeight = 1;

    private final float[] viewMatrix       = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] vpMatrix         = new float[16];

    public Renderer(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.activeCamera = new Camera();
    }

    public void setSceneManager(SceneManager sm) {
        this.sceneManager = sm;
    }

    /** Called on GL thread when surface is (re)created. */
    public void onSurfaceCreated() {
        // Re-create shader — old GL context is gone
        defaultShader = ShaderProgram.createDefault();
        Log.i(TAG, "Shader compiled OK, programId=" + defaultShader.getProgramId());

        // Verify shader actually linked
        int[] prog = {defaultShader.getProgramId()};
        if (prog[0] == 0) {
            Log.e(TAG, "Shader program ID is 0 — shader failed!");
        }
    }

    public void onSurfaceChanged(int width, int height) {
        this.screenWidth  = width;
        this.screenHeight = height;
        activeCamera.setAspectRatio((float) width / height);
        Log.i(TAG, "Surface " + width + "x" + height + ", aspect=" + ((float)width/height));
    }

    public void render() {
        if (defaultShader == null) {
            Log.w(TAG, "render() called but shader is null");
            return;
        }

        activeCamera.getViewMatrix(viewMatrix);
        activeCamera.getProjectionMatrix(projectionMatrix);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        defaultShader.use();
        defaultShader.setUniformMatrix4fv("uVPMatrix", vpMatrix);

        // Rebuild visible list fresh every frame
        sceneManager.update(0);
        List<SceneNode> nodes = sceneManager.getVisibleNodes();

        for (SceneNode node : nodes) {
            if (node.getMesh() != null) {
                defaultShader.setUniformMatrix4fv("uModelMatrix", node.getWorldTransform());
                defaultShader.setUniform3f("uColor", node.getColor());
                node.getMesh().draw();
            }
        }
    }

    public Camera getActiveCamera()              { return activeCamera;  }
    public void   setActiveCamera(Camera cam)    { this.activeCamera = cam; }
    public ShaderProgram getDefaultShader()      { return defaultShader; }
    public int getScreenWidth()                  { return screenWidth;   }
    public int getScreenHeight()                 { return screenHeight;  }
}
