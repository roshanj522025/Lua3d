package com.luagame.framework.renderer;

import android.opengl.GLES30;
import android.util.Log;

/**
 * Wraps an OpenGL ES 3.0 shader program.
 * Provides helpers to compile shaders and set common uniforms.
 */
public class ShaderProgram {

    private static final String TAG = "ShaderProgram";

    // ─── Default Phong-lite GLSL source ───────────────────────────────────────

    private static final String DEFAULT_VERTEX_SHADER =
        "#version 300 es\n" +
        "layout(location = 0) in vec3 aPosition;\n" +
        "layout(location = 1) in vec3 aNormal;\n" +
        "uniform mat4 uModelMatrix;\n" +
        "uniform mat4 uVPMatrix;\n" +
        "out vec3 vNormal;\n" +
        "out vec3 vFragPos;\n" +
        "void main() {\n" +
        "    vec4 worldPos   = uModelMatrix * vec4(aPosition, 1.0);\n" +
        "    vFragPos        = worldPos.xyz;\n" +
        "    vNormal         = mat3(transpose(inverse(uModelMatrix))) * aNormal;\n" +
        "    gl_Position     = uVPMatrix * worldPos;\n" +
        "}\n";

    private static final String DEFAULT_FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in  vec3 vNormal;\n" +
        "in  vec3 vFragPos;\n" +
        "out vec4 fragColor;\n" +
        "uniform vec3 uColor;\n" +
        "const vec3 LIGHT_DIR = normalize(vec3(0.4, 1.0, 0.6));\n" +
        "const vec3 AMBIENT   = vec3(0.15);\n" +
        "void main() {\n" +
        "    vec3 norm    = normalize(vNormal);\n" +
        "    float diff   = max(dot(norm, LIGHT_DIR), 0.0);\n" +
        "    vec3 diffuse = diff * uColor;\n" +
        "    vec3 color   = (AMBIENT + diffuse) * uColor;\n" +
        "    fragColor    = vec4(color, 1.0);\n" +
        "}\n";

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final int programId;

    private ShaderProgram(int programId) {
        this.programId = programId;
    }

    // ─── Factory methods ──────────────────────────────────────────────────────

    public static ShaderProgram createDefault() {
        return create(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
    }

    public static ShaderProgram create(String vertSrc, String fragSrc) {
        int vert = compileShader(GLES30.GL_VERTEX_SHADER,   vertSrc);
        int frag = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vert);
        GLES30.glAttachShader(program, frag);
        GLES30.glLinkProgram(program);

        // Check link status
        int[] status = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(program);
            Log.e(TAG, "Program link error: " + log);
            GLES30.glDeleteProgram(program);
            throw new RuntimeException("Shader program link failed: " + log);
        }

        GLES30.glDeleteShader(vert);
        GLES30.glDeleteShader(frag);

        return new ShaderProgram(program);
    }

    private static int compileShader(int type, String src) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, src);
        GLES30.glCompileShader(shader);

        int[] status = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            Log.e(TAG, "Shader compile error (" + (type == GLES30.GL_VERTEX_SHADER ? "vert" : "frag") + "): " + log);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return shader;
    }

    // ─── Uniform helpers ─────────────────────────────────────────────────────

    public void use() {
        GLES30.glUseProgram(programId);
    }

    public void setUniformMatrix4fv(String name, float[] matrix) {
        int loc = GLES30.glGetUniformLocation(programId, name);
        if (loc >= 0) GLES30.glUniformMatrix4fv(loc, 1, false, matrix, 0);
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
