package com.endless.engine.renderer;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.endless.engine.scene.Camera;
import com.endless.engine.scene.SceneManager;
import com.endless.engine.scene.SceneNode;

import java.util.List;

public class Renderer {

    private static final String TAG = "Renderer";

    private SceneManager  sceneManager;
    private ShaderProgram defaultShader;
    private Camera        activeCamera;
    private Texture       whiteTexture;   // fallback when node has no texture

    private int screenWidth = 1, screenHeight = 1;

    private final float[] viewMatrix       = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] vpMatrix         = new float[16];

    public Renderer(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.activeCamera = new Camera();
    }

    public void setSceneManager(SceneManager sm) { this.sceneManager = sm; }

    public void onSurfaceCreated() {
        defaultShader = ShaderProgram.createDefault();
        whiteTexture  = Texture.createWhite();
        Log.i(TAG, "Renderer surface created, shader=" + defaultShader.getProgramId());
    }

    public void onSurfaceChanged(int w, int h) {
        screenWidth  = w;
        screenHeight = h;
        activeCamera.setAspectRatio((float) w / h);
        Log.i(TAG, "Surface " + w + "x" + h);
    }

    public void render() {
        if (defaultShader == null) return;

        activeCamera.getViewMatrix(viewMatrix);
        activeCamera.getProjectionMatrix(projectionMatrix);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        defaultShader.use();
        defaultShader.setUniformMatrix4fv("uVPMatrix", vpMatrix);
        defaultShader.setUniform1i("uTexture", 0);  // texture unit 0

        sceneManager.update(0);
        List<SceneNode> nodes = sceneManager.getVisibleNodes();

        for (SceneNode node : nodes) {
            if (node.getMesh() == null) continue;

            defaultShader.setUniformMatrix4fv("uModelMatrix", node.getWorldTransform());
            defaultShader.setUniform3f("uColor", node.getColor());

            // Bind texture if node has one, else bind 1x1 white
            Texture tex = node.getTexture();
            if (tex != null && tex.isValid()) {
                tex.bind(0);
                defaultShader.setUniform1i("uUseTexture", 1);
            } else {
                whiteTexture.bind(0);
                defaultShader.setUniform1i("uUseTexture", 0);
            }

            node.getMesh().draw();
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    public Camera        getActiveCamera()           { return activeCamera;  }
    public void          setActiveCamera(Camera c)   { this.activeCamera = c; }
    public ShaderProgram getDefaultShader()          { return defaultShader; }
    public int           getScreenWidth()            { return screenWidth;   }
    public int           getScreenHeight()           { return screenHeight;  }
}
