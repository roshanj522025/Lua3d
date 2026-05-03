package com.endless.engine.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an OpenGL ES 3.0 texture.
 * Loads from assets/textures/<filename> using Android BitmapFactory.
 */
public class Texture {

    private static final String TAG = "Texture";

    private int textureId = 0;
    private int width, height;

    private Texture() {}

    /** Load a texture from assets/textures/<filename>. Must be called on GL thread. */
    public static Texture fromAsset(Context context, String filename) {
        Texture tex = new Texture();
        try (InputStream is = context.getAssets().open("textures/" + filename)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) throw new IOException("BitmapFactory returned null for: " + filename);
            tex.width  = bmp.getWidth();
            tex.height = bmp.getHeight();
            tex.textureId = uploadBitmap(bmp);
            bmp.recycle();
            Log.i(TAG, "Loaded texture: " + filename + " (" + tex.width + "x" + tex.height + ") id=" + tex.textureId);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load texture: " + filename + " — " + e.getMessage());
        }
        return tex;
    }

    /** Create a 1×1 white texture (safe fallback when no texture assigned). */
    public static Texture createWhite() {
        Texture tex = new Texture();
        Bitmap bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bmp.setPixel(0, 0, 0xFFFFFFFF);
        tex.textureId = uploadBitmap(bmp);
        tex.width = tex.height = 1;
        bmp.recycle();
        return tex;
    }

    private static int uploadBitmap(Bitmap bmp) {
        int[] ids = new int[1];
        GLES30.glGenTextures(1, ids, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, ids[0]);

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        return ids[0];
    }

    public void bind(int unit) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    public void dispose() {
        if (textureId != 0) {
            GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = 0;
        }
    }

    public int getTextureId() { return textureId; }
    public int getWidth()     { return width;     }
    public int getHeight()    { return height;    }
    public boolean isValid()  { return textureId != 0; }
}
