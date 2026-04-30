package com.luagame.framework.renderer;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.luagame.framework.scene.Camera;
import com.luagame.framework.scene.SceneManager;
import com.luagame.framework.scene.SceneNode;

import java.util.List;

/**
 * High-level renderer: owns the default shader program, camera matrices,
 * and orchestrates draw calls for all visible scene nodes.
 */
public class Renderer {

    private static final String TAG = "Renderer";

    private final SceneManager sceneManager;
    private ShaderProgram defaultShader;
    private Camera activeCamera;

    private int screenWidth  = 1;
    private int screenHeight = 1;

    // Reusable matrix buffers
    private final float[] viewMatrix       = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] vpMatrix         = new float[16];

    public Renderer(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.activeCamera = new Camera();
    }

    // Called on GL thread when surface is (re)created
    public void onSurfaceCreated() {
        defaultShader = ShaderProgram.createDefault();
        Log.i(TAG, "Default shader compiled. ProgramID=" + defaultShader.getProgramId());
    }

    public void onSurfaceChanged(int width, int height) {
        this.screenWidth  = width;
        this.screenHeight = height;
        activeCamera.setAspectRatio((float) width / height);
    }

    /** Called every frame from GameEngine.tick() */
    public void render() {
        if (defaultShader == null) return;

        // Build view + projection matrices
        activeCamera.getViewMatrix(viewMatrix);
        activeCamera.getProjectionMatrix(projectionMatrix);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        defaultShader.use();
        defaultShader.setUniformMatrix4fv("uVPMatrix", vpMatrix);

        // Walk scene graph and draw each mesh node
        List<SceneNode> nodes = sceneManager.getVisibleNodes();
        for (SceneNode node : nodes) {
            if (node.getMesh() != null) {
                float[] modelMatrix = node.getWorldTransform();
                defaultShader.setUniformMatrix4fv("uModelMatrix", modelMatrix);
                defaultShader.setUniform3f("uColor", node.getColor());
                node.getMesh().draw();
            }
        }
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public Camera getActiveCamera() { return activeCamera; }

    public void setActiveCamera(Camera cam) {
        this.activeCamera = cam;
    }

    public ShaderProgram getDefaultShader() { return defaultShader; }

    public int getScreenWidth()  { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
}
