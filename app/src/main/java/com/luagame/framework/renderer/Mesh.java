package com.luagame.framework.renderer;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * GPU-side mesh: owns a VAO, VBO (interleaved position+normal), and IBO.
 *
 * Vertex layout: [ px, py, pz, nx, ny, nz ]  (6 floats, 24 bytes per vertex)
 */
public class Mesh {

    private int vao;
    private int vbo;
    private int ibo;
    private int indexCount;

    /**
     * @param vertices  Interleaved float array: [x,y,z, nx,ny,nz, ...]
     * @param indices   Triangle indices (shorts)
     */
    public Mesh(float[] vertices, short[] indices) {
        this.indexCount = indices.length;
        uploadToGPU(vertices, indices);
    }

    private void uploadToGPU(float[] vertices, short[] indices) {
        int[] vaoBuf = new int[1];
        int[] vboBuf = new int[1];
        int[] iboBuf = new int[1];

        GLES30.glGenVertexArrays(1, vaoBuf, 0);
        GLES30.glGenBuffers(1, vboBuf, 0);
        GLES30.glGenBuffers(1, iboBuf, 0);

        vao = vaoBuf[0];
        vbo = vboBuf[0];
        ibo = iboBuf[0];

        GLES30.glBindVertexArray(vao);

        // Upload vertex data
        FloatBuffer vb = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vb.put(vertices).position(0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * 4, vb, GLES30.GL_STATIC_DRAW);

        final int STRIDE = 6 * 4;  // 6 floats × 4 bytes

        // aPosition (location 0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, STRIDE, 0);
        GLES30.glEnableVertexAttribArray(0);

        // aNormal (location 1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, STRIDE, 3 * 4);
        GLES30.glEnableVertexAttribArray(1);

        // Upload index data
        ShortBuffer ib = ByteBuffer.allocateDirect(indices.length * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer();
        ib.put(indices).position(0);

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, ib, GLES30.GL_STATIC_DRAW);

        GLES30.glBindVertexArray(0);
    }

    public void draw() {
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0);
        GLES30.glBindVertexArray(0);
    }

    public void dispose() {
        GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
        GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
        GLES30.glDeleteBuffers(1, new int[]{ibo}, 0);
    }

    // ─── Built-in primitive factories ────────────────────────────────────────

    /** Unit cube centered at origin */
    public static Mesh createCube() {
        float[] v = {
            // Front face  (normal  0, 0, 1)
            -0.5f,-0.5f, 0.5f,  0, 0, 1,
             0.5f,-0.5f, 0.5f,  0, 0, 1,
             0.5f, 0.5f, 0.5f,  0, 0, 1,
            -0.5f, 0.5f, 0.5f,  0, 0, 1,
            // Back face   (normal  0, 0,-1)
             0.5f,-0.5f,-0.5f,  0, 0,-1,
            -0.5f,-0.5f,-0.5f,  0, 0,-1,
            -0.5f, 0.5f,-0.5f,  0, 0,-1,
             0.5f, 0.5f,-0.5f,  0, 0,-1,
            // Left face   (normal -1, 0, 0)
            -0.5f,-0.5f,-0.5f, -1, 0, 0,
            -0.5f,-0.5f, 0.5f, -1, 0, 0,
            -0.5f, 0.5f, 0.5f, -1, 0, 0,
            -0.5f, 0.5f,-0.5f, -1, 0, 0,
            // Right face  (normal  1, 0, 0)
             0.5f,-0.5f, 0.5f,  1, 0, 0,
             0.5f,-0.5f,-0.5f,  1, 0, 0,
             0.5f, 0.5f,-0.5f,  1, 0, 0,
             0.5f, 0.5f, 0.5f,  1, 0, 0,
            // Top face    (normal  0, 1, 0)
            -0.5f, 0.5f, 0.5f,  0, 1, 0,
             0.5f, 0.5f, 0.5f,  0, 1, 0,
             0.5f, 0.5f,-0.5f,  0, 1, 0,
            -0.5f, 0.5f,-0.5f,  0, 1, 0,
            // Bottom face (normal  0,-1, 0)
            -0.5f,-0.5f,-0.5f,  0,-1, 0,
             0.5f,-0.5f,-0.5f,  0,-1, 0,
             0.5f,-0.5f, 0.5f,  0,-1, 0,
            -0.5f,-0.5f, 0.5f,  0,-1, 0,
        };
        short[] i = {
             0, 1, 2,  2, 3, 0,   // Front
             4, 5, 6,  6, 7, 4,   // Back
             8, 9,10, 10,11, 8,   // Left
            12,13,14, 14,15,12,   // Right
            16,17,18, 18,19,16,   // Top
            20,21,22, 22,23,20    // Bottom
        };
        return new Mesh(v, i);
    }

    /** Simple flat quad on the XZ plane */
    public static Mesh createPlane(float size) {
        float h = size * 0.5f;
        float[] v = {
            -h, 0,-h,  0, 1, 0,
             h, 0,-h,  0, 1, 0,
             h, 0, h,  0, 1, 0,
            -h, 0, h,  0, 1, 0,
        };
        short[] i = { 0, 1, 2,  2, 3, 0 };
        return new Mesh(v, i);
    }

    /** Low-poly UV sphere */
    public static Mesh createSphere(float radius, int rings, int sectors) {
        int vCount = (rings + 1) * (sectors + 1);
        float[] verts = new float[vCount * 6];
        int vi = 0;

        for (int r = 0; r <= rings; r++) {
            float phi = (float) (Math.PI * r / rings);
            for (int s = 0; s <= sectors; s++) {
                float theta = (float) (2 * Math.PI * s / sectors);
                float nx = (float)(Math.sin(phi) * Math.cos(theta));
                float ny = (float)(Math.cos(phi));
                float nz = (float)(Math.sin(phi) * Math.sin(theta));
                verts[vi++] = nx * radius;
                verts[vi++] = ny * radius;
                verts[vi++] = nz * radius;
                verts[vi++] = nx;
                verts[vi++] = ny;
                verts[vi++] = nz;
            }
        }

        int iCount = rings * sectors * 6;
        short[] inds = new short[iCount];
        int ii = 0;
        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < sectors; s++) {
                short a = (short)(r * (sectors + 1) + s);
                short b = (short)(a + sectors + 1);
                inds[ii++] = a; inds[ii++] = b;         inds[ii++] = (short)(a + 1);
                inds[ii++] = (short)(a + 1); inds[ii++] = b; inds[ii++] = (short)(b + 1);
            }
        }
        return new Mesh(verts, inds);
    }
}
