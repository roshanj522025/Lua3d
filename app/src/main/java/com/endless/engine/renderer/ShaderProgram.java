package com.endless.engine.renderer;

import android.opengl.GLES30;
import android.util.Log;

/**
 * Wraps an OpenGL ES 3.0 shader program.
 * Default shader supports: Phong lighting + optional texture + tint color.
 */
public class ShaderProgram {

    private static final String TAG = "ShaderProgram";

    // ─── Default shader — Phong + texture ─────────────────────────────────────

    private static final String DEFAULT_VERT =
        "#version 300 es\n" +
        "layout(location=0) in vec3 aPosition;\n" +
        "layout(location=1) in vec3 aNormal;\n" +
        "layout(location=2) in vec2 aTexCoord;\n" +
        "uniform mat4 uModelMatrix;\n" +
        "uniform mat4 uVPMatrix;\n" +
        "out vec3 vNormal;\n" +
        "out vec3 vFragPos;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);\n" +
        "    vFragPos     = worldPos.xyz;\n" +
        "    vNormal      = mat3(transpose(inverse(uModelMatrix))) * aNormal;\n" +
        "    vTexCoord    = aTexCoord;\n" +
        "    gl_Position  = uVPMatrix * worldPos;\n" +
        "}\n";

    private static final String DEFAULT_FRAG =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in  vec3 vNormal;\n" +
        "in  vec3 vFragPos;\n" +
        "in  vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "uniform vec3      uColor;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform int       uUseTexture;\n" +
        "const vec3 LIGHT_DIR = normalize(vec3(0.5, 1.0, 0.6));\n" +
        "const vec3 AMBIENT   = vec3(0.18);\n" +
        "void main() {\n" +
        "    vec3 norm    = normalize(vNormal);\n" +
        "    float diff   = max(dot(norm, LIGHT_DIR), 0.0);\n" +
        "    vec3 baseCol = (uUseTexture == 1)\n" +
        "                   ? texture(uTexture, vTexCoord).rgb * uColor\n" +
        "                   : uColor;\n" +
        "    vec3 lit     = (AMBIENT + diff * vec3(0.82)) * baseCol;\n" +
        "    fragColor    = vec4(lit, 1.0);\n" +
        "}\n";

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final int programId;

    private ShaderProgram(int id) { this.programId = id; }

    // ─── Factory ─────────────────────────────────────────────────────────────

    public static ShaderProgram createDefault() {
        return create(DEFAULT_VERT, DEFAULT_FRAG);
    }

    public static ShaderProgram create(String vertSrc, String fragSrc) {
        int vert = compile(GLES30.GL_VERTEX_SHADER,   vertSrc);
        int frag = compile(GLES30.GL_FRAGMENT_SHADER, fragSrc);
        int prog = GLES30.glCreateProgram();
        GLES30.glAttachShader(prog, vert);
        GLES30.glAttachShader(prog, frag);
        GLES30.glLinkProgram(prog);
        int[] st = new int[1];
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, st, 0);
        if (st[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(prog);
            GLES30.glDeleteProgram(prog);
            throw new RuntimeException("Shader link failed: " + log);
        }
        GLES30.glDeleteShader(vert);
        GLES30.glDeleteShader(frag);
        Log.i(TAG, "Shader compiled OK, programId=" + prog);
        com.endless.engine.core.GameActivity.log("[GL] Shader compiled OK");
        return new ShaderProgram(prog);
    }

    private static int compile(int type, String src) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, src);
        GLES30.glCompileShader(shader);
        int[] st = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, st, 0);
        if (st[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed (" +
                (type == GLES30.GL_VERTEX_SHADER ? "vert" : "frag") + "): " + log);
        }
        return shader;
    }

    // ─── Uniform helpers ─────────────────────────────────────────────────────

    public void use() { GLES30.glUseProgram(programId); }

    public void setUniformMatrix4fv(String name, float[] m) {
        int loc = GLES30.glGetUniformLocation(programId, name);
        if (loc >= 0) GLES30.glUniformMatrix4fv(loc, 1, false, m, 0);
    }

    public void setUniform3f(String name, float[] v) {
        int loc = GLES30.glGetUniformLocation(programId, name);
        if (loc >= 0 && v.length >= 3) GLES30.glUniform3f(loc, v[0], v[1], v[2]);
    }

    public void setUniform1f(String name, float v) {
        int loc = GLES30.glGetUniformLocation(programId, name);
        if (loc >= 0) GLES30.glUniform1f(loc, v);
    }

    public void setUniform1i(String name, int v) {
        int loc = GLES30.glGetUniformLocation(programId, name);
        if (loc >= 0) GLES30.glUniform1i(loc, v);
    }

    public int getProgramId() { return programId; }
}
