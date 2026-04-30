package com.luagame.framework.scene;

import android.opengl.Matrix;

import com.luagame.framework.renderer.Mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Node in the scene graph.
 * Each node has a local transform (TRS) and optional mesh + color.
 * Parent-child hierarchy is supported for hierarchical transforms.
 */
public class SceneNode {

    private final String name;

    // Local transform components
    private float[] position  = { 0f, 0f, 0f };
    private float[] rotation  = { 0f, 0f, 0f };  // Euler angles in degrees (X, Y, Z)
    private float[] scale     = { 1f, 1f, 1f };

    // Rendering
    private Mesh  mesh;
    private float[] color = { 1f, 1f, 1f };      // RGB

    // Hierarchy
    private SceneNode          parent;
    private final List<SceneNode> children = new ArrayList<>();

    // Cached world transform (recomputed when dirty)
    private final float[] localMatrix = new float[16];
    private final float[] worldMatrix = new float[16];
    private boolean dirty = true;

    // User data (accessible from Lua)
    private Object userData;

    public SceneNode(String name) {
        this.name = name;
    }

    // ─── Transform ───────────────────────────────────────────────────────────

    public void setPosition(float x, float y, float z) {
        position[0] = x; position[1] = y; position[2] = z;
        markDirty();
    }

    public void setRotation(float rx, float ry, float rz) {
        rotation[0] = rx; rotation[1] = ry; rotation[2] = rz;
        markDirty();
    }

    public void setScale(float sx, float sy, float sz) {
        scale[0] = sx; scale[1] = sy; scale[2] = sz;
        markDirty();
    }

    public void translate(float dx, float dy, float dz) {
        position[0] += dx; position[1] += dy; position[2] += dz;
        markDirty();
    }

    public void rotate(float drx, float dry, float drz) {
        rotation[0] += drx; rotation[1] += dry; rotation[2] += drz;
        markDirty();
    }

    /** Returns the world-space transform matrix (recomputed if dirty). */
    public float[] getWorldTransform() {
        if (dirty) recomputeMatrix();
        return worldMatrix;
    }

    private void recomputeMatrix() {
        // Build local TRS matrix
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.translateM(localMatrix, 0, position[0], position[1], position[2]);
        Matrix.rotateM(localMatrix, 0, rotation[0], 1, 0, 0);
        Matrix.rotateM(localMatrix, 0, rotation[1], 0, 1, 0);
        Matrix.rotateM(localMatrix, 0, rotation[2], 0, 0, 1);
        Matrix.scaleM(localMatrix, 0, scale[0], scale[1], scale[2]);

        if (parent != null) {
            Matrix.multiplyMM(worldMatrix, 0, parent.getWorldTransform(), 0, localMatrix, 0);
        } else {
            System.arraycopy(localMatrix, 0, worldMatrix, 0, 16);
        }
        dirty = false;
    }

    private void markDirty() {
        dirty = true;
        for (SceneNode child : children) child.markDirty();
    }

    // ─── Hierarchy ───────────────────────────────────────────────────────────

    public void addChild(SceneNode child) {
        child.parent = this;
        children.add(child);
        child.markDirty();
    }

    public void removeChild(SceneNode child) {
        if (children.remove(child)) {
            child.parent = null;
        }
    }

    public List<SceneNode> getChildren() { return children; }

    // ─── Properties ──────────────────────────────────────────────────────────

    public String  getName()    { return name;     }
    public Mesh    getMesh()    { return mesh;     }
    public float[] getColor()   { return color;    }
    public Object  getUserData(){ return userData; }

    public void setMesh(Mesh mesh)          { this.mesh = mesh; }
    public void setColor(float r, float g, float b) { color[0]=r; color[1]=g; color[2]=b; }
    public void setUserData(Object data)    { this.userData = data; }

    public float[] getPosition() { return position.clone(); }
    public float[] getRotation() { return rotation.clone(); }
    public float[] getScale()    { return scale.clone();    }
}
