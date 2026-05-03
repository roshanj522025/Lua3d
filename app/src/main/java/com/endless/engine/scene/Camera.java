package com.endless.engine.scene;

import android.opengl.Matrix;

/**
 * Perspective camera.
 * Exposes position, target (look-at point), and up vector.
 * Produces view + projection matrices ready for GL uniforms.
 */
public class Camera {

    private float[] position    = { 0f, 3f, 6f };
    private float[] target      = { 0f, 0f, 0f };
    private float[] up          = { 0f, 1f, 0f };

    private float fovDegrees    = 60f;
    private float aspectRatio   = 16f / 9f;
    private float nearPlane     = 0.1f;
    private float farPlane      = 500f;

    private final float[] viewMatrix       = new float[16];
    private final float[] projectionMatrix = new float[16];

    private boolean viewDirty = true;
    private boolean projDirty = true;

    // ─── View matrix ─────────────────────────────────────────────────────────

    public float[] getViewMatrix(float[] out) {
        if (viewDirty) {
            Matrix.setLookAtM(viewMatrix, 0,
                position[0], position[1], position[2],
                target[0],   target[1],   target[2],
                up[0],       up[1],       up[2]);
            viewDirty = false;
        }
        System.arraycopy(viewMatrix, 0, out, 0, 16);
        return out;
    }

    // ─── Projection matrix ───────────────────────────────────────────────────

    public float[] getProjectionMatrix(float[] out) {
        if (projDirty) {
            Matrix.perspectiveM(projectionMatrix, 0, fovDegrees, aspectRatio, nearPlane, farPlane);
            projDirty = false;
        }
        System.arraycopy(projectionMatrix, 0, out, 0, 16);
        return out;
    }

    // ─── Setters / getters ───────────────────────────────────────────────────

    public void setPosition(float x, float y, float z) {
        position[0] = x; position[1] = y; position[2] = z;
        viewDirty = true;
    }

    public void setTarget(float x, float y, float z) {
        target[0] = x; target[1] = y; target[2] = z;
        viewDirty = true;
    }

    public void setUp(float x, float y, float z) {
        up[0] = x; up[1] = y; up[2] = z;
        viewDirty = true;
    }

    public void setFov(float degrees) {
        fovDegrees = degrees;
        projDirty = true;
    }

    public void setAspectRatio(float aspect) {
        aspectRatio = aspect;
        projDirty = true;
    }

    public void setNearFar(float near, float far) {
        nearPlane = near;
        farPlane  = far;
        projDirty = true;
    }

    public float[] getPosition() { return position.clone(); }
    public float[] getTarget()   { return target.clone();   }
}
