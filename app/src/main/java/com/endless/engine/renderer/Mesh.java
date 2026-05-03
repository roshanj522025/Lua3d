package com.endless.engine.renderer;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * GPU-side mesh with position, normal, and UV coordinates.
 *
 * Vertex layout: [ px, py, pz, nx, ny, nz, u, v ]  (8 floats, 32 bytes)
 *
 * Supports:
 *  - Built-in primitives: cube, sphere, plane
 *  - OBJ file loading from assets/models/
 */
public class Mesh {

    private static final String TAG = "Mesh";

    private int vao, vbo, ibo;
    private int indexCount;

    public Mesh(float[] vertices, short[] indices) {
        this.indexCount = indices.length;
        uploadToGPU(vertices, indices);
    }

    private void uploadToGPU(float[] vertices, short[] indices) {
        int[] buf = new int[3];
        GLES30.glGenVertexArrays(1, buf, 0); vao = buf[0];
        GLES30.glGenBuffers(1, buf, 0);      vbo = buf[0];
        GLES30.glGenBuffers(1, buf, 1);      ibo = buf[1];

        GLES30.glBindVertexArray(vao);

        FloatBuffer vb = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vb.put(vertices).position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * 4, vb, GLES30.GL_STATIC_DRAW);

        final int STRIDE = 8 * 4;  // 8 floats × 4 bytes
        // aPosition (loc 0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, STRIDE, 0);
        GLES30.glEnableVertexAttribArray(0);
        // aNormal (loc 1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, STRIDE, 3 * 4);
        GLES30.glEnableVertexAttribArray(1);
        // aTexCoord (loc 2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, STRIDE, 6 * 4);
        GLES30.glEnableVertexAttribArray(2);

        ShortBuffer ib = ByteBuffer.allocateDirect(indices.length * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer();
        ib.put(indices).position(0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, ib, GLES30.GL_STATIC_DRAW);

        GLES30.glBindVertexArray(0);
        Log.i(TAG, "Mesh uploaded: vao=" + vao + " indices=" + indexCount);
        com.endless.engine.core.GameActivity.log("[Mesh] Created vao=" + vao + " indices=" + indexCount);
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

    // ─── Primitive factories ─────────────────────────────────────────────────
    // Vertex layout per entry: x,y,z, nx,ny,nz, u,v

    public static Mesh createCube() {
        float[] v = {
            // Front (0,0,1)
            -0.5f,-0.5f, 0.5f,  0,0,1,  0,1,
             0.5f,-0.5f, 0.5f,  0,0,1,  1,1,
             0.5f, 0.5f, 0.5f,  0,0,1,  1,0,
            -0.5f, 0.5f, 0.5f,  0,0,1,  0,0,
            // Back (0,0,-1)
             0.5f,-0.5f,-0.5f,  0,0,-1, 0,1,
            -0.5f,-0.5f,-0.5f,  0,0,-1, 1,1,
            -0.5f, 0.5f,-0.5f,  0,0,-1, 1,0,
             0.5f, 0.5f,-0.5f,  0,0,-1, 0,0,
            // Left (-1,0,0)
            -0.5f,-0.5f,-0.5f, -1,0,0,  0,1,
            -0.5f,-0.5f, 0.5f, -1,0,0,  1,1,
            -0.5f, 0.5f, 0.5f, -1,0,0,  1,0,
            -0.5f, 0.5f,-0.5f, -1,0,0,  0,0,
            // Right (1,0,0)
             0.5f,-0.5f, 0.5f,  1,0,0,  0,1,
             0.5f,-0.5f,-0.5f,  1,0,0,  1,1,
             0.5f, 0.5f,-0.5f,  1,0,0,  1,0,
             0.5f, 0.5f, 0.5f,  1,0,0,  0,0,
            // Top (0,1,0)
            -0.5f, 0.5f, 0.5f,  0,1,0,  0,1,
             0.5f, 0.5f, 0.5f,  0,1,0,  1,1,
             0.5f, 0.5f,-0.5f,  0,1,0,  1,0,
            -0.5f, 0.5f,-0.5f,  0,1,0,  0,0,
            // Bottom (0,-1,0)
            -0.5f,-0.5f,-0.5f,  0,-1,0, 0,1,
             0.5f,-0.5f,-0.5f,  0,-1,0, 1,1,
             0.5f,-0.5f, 0.5f,  0,-1,0, 1,0,
            -0.5f,-0.5f, 0.5f,  0,-1,0, 0,0,
        };
        short[] i = {
             0, 1, 2,  2, 3, 0,
             4, 5, 6,  6, 7, 4,
             8, 9,10, 10,11, 8,
            12,13,14, 14,15,12,
            16,17,18, 18,19,16,
            20,21,22, 22,23,20
        };
        return new Mesh(v, i);
    }

    public static Mesh createPlane(float size) {
        float h = size * 0.5f;
        float[] v = {
            -h,0,-h,  0,1,0,  0,size,
             h,0,-h,  0,1,0,  size,size,
             h,0, h,  0,1,0,  size,0,
            -h,0, h,  0,1,0,  0,0,
        };
        short[] i = { 0,1,2, 2,3,0 };
        return new Mesh(v, i);
    }

    public static Mesh createSphere(float radius, int rings, int sectors) {
        int vCount = (rings + 1) * (sectors + 1);
        float[] verts = new float[vCount * 8];
        int vi = 0;
        for (int r = 0; r <= rings; r++) {
            float phi = (float)(Math.PI * r / rings);
            for (int s = 0; s <= sectors; s++) {
                float theta = (float)(2 * Math.PI * s / sectors);
                float nx = (float)(Math.sin(phi) * Math.cos(theta));
                float ny = (float)(Math.cos(phi));
                float nz = (float)(Math.sin(phi) * Math.sin(theta));
                verts[vi++] = nx * radius;
                verts[vi++] = ny * radius;
                verts[vi++] = nz * radius;
                verts[vi++] = nx; verts[vi++] = ny; verts[vi++] = nz;
                verts[vi++] = (float)s / sectors;
                verts[vi++] = (float)r / rings;
            }
        }
        int iCount = rings * sectors * 6;
        short[] inds = new short[iCount];
        int ii = 0;
        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < sectors; s++) {
                short a = (short)(r * (sectors + 1) + s);
                short b = (short)(a + sectors + 1);
                inds[ii++]=a; inds[ii++]=b;           inds[ii++]=(short)(a+1);
                inds[ii++]=(short)(a+1); inds[ii++]=b; inds[ii++]=(short)(b+1);
            }
        }
        return new Mesh(verts, inds);
    }

    // ─── OBJ Loader ──────────────────────────────────────────────────────────

    /**
     * Loads a Wavefront OBJ file from assets/models/<filename>.
     * Supports: v, vn, vt, f (triangulated faces).
     * Does NOT support: materials, quads (use Blender's triangulate modifier).
     */
    public static Mesh loadOBJ(Context context, String filename) {
        Log.i(TAG, "Loading OBJ: " + filename);
        com.endless.engine.core.GameActivity.log("[OBJ] Loading: " + filename);

        List<float[]> positions = new ArrayList<>();
        List<float[]> normals   = new ArrayList<>();
        List<float[]> uvs       = new ArrayList<>();

        // face indices: [posIdx, uvIdx, normIdx] per vertex
        List<int[]> faceVerts = new ArrayList<>();

        try (InputStream is = context.getAssets().open("models/" + filename);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] p = line.substring(2).trim().split("\\s+");
                    positions.add(new float[]{Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                } else if (line.startsWith("vn ")) {
                    String[] p = line.substring(3).trim().split("\\s+");
                    normals.add(new float[]{Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                } else if (line.startsWith("vt ")) {
                    String[] p = line.substring(3).trim().split("\\s+");
                    uvs.add(new float[]{Float.parseFloat(p[0]), 1.0f - Float.parseFloat(p[1])}); // flip V
                } else if (line.startsWith("f ")) {
                    String[] tokens = line.substring(2).trim().split("\\s+");
                    if (tokens.length < 3) continue;
                    // Fan-triangulate if more than 3 verts
                    int[] v0 = parseFaceVertex(tokens[0]);
                    for (int k = 1; k + 1 < tokens.length; k++) {
                        faceVerts.add(v0);
                        faceVerts.add(parseFaceVertex(tokens[k]));
                        faceVerts.add(parseFaceVertex(tokens[k + 1]));
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "OBJ load error: " + e.getMessage());
            com.endless.engine.core.GameActivity.log("[OBJ] ERROR: " + e.getMessage());
            return createCube(); // safe fallback
        }

        if (faceVerts.isEmpty()) {
            Log.w(TAG, "OBJ has no faces: " + filename);
            return createCube();
        }

        // Build interleaved vertex array (unindex for simplicity — max 32767 tris)
        int vertCount = Math.min(faceVerts.size(), 32766);
        float[] verts = new float[vertCount * 8];
        short[] inds  = new short[vertCount];
        boolean hasNormals = !normals.isEmpty();
        boolean hasUVs     = !uvs.isEmpty();

        for (int i = 0; i < vertCount; i++) {
            int[] fv = faceVerts.get(i);
            int pi = fv[0], ui = fv[1], ni = fv[2];

            float[] pos = (pi >= 0 && pi < positions.size()) ? positions.get(pi) : new float[]{0,0,0};
            float[] uv  = (hasUVs && ui >= 0 && ui < uvs.size()) ? uvs.get(ui) : new float[]{0,0};
            float[] nor = (hasNormals && ni >= 0 && ni < normals.size()) ? normals.get(ni) : new float[]{0,1,0};

            int base = i * 8;
            verts[base]   = pos[0]; verts[base+1] = pos[1]; verts[base+2] = pos[2];
            verts[base+3] = nor[0]; verts[base+4] = nor[1]; verts[base+5] = nor[2];
            verts[base+6] = uv[0];  verts[base+7] = uv[1];
            inds[i] = (short) i;
        }

        Log.i(TAG, "OBJ loaded: " + filename + " — " + (vertCount/3) + " triangles");
        com.endless.engine.core.GameActivity.log("[OBJ] OK: " + (vertCount/3) + " tris");
        return new Mesh(verts, inds);
    }

    /** Parse "v/vt/vn", "v//vn", "v/vt", or "v" → [posIdx, uvIdx, normIdx] (0-based) */
    private static int[] parseFaceVertex(String token) {
        String[] parts = token.split("/", -1);
        int pi = parts.length > 0 && !parts[0].isEmpty() ? Integer.parseInt(parts[0]) - 1 : 0;
        int ui = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) - 1 : -1;
        int ni = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) - 1 : -1;
        return new int[]{pi, ui, ni};
    }
}
