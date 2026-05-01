package com.luagame.framework.renderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.luagame.framework.core.GameEngine;

public class GameGLSurfaceView extends GLSurfaceView {

    private final GameEngine gameEngine;

    public GameGLSurfaceView(Context context, GameEngine engine) {
        super(context);
        this.gameEngine = engine;

        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(new GameRenderer(engine));
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gameEngine.getInputManager().handleTouchEvent(event);
        return true;
    }
}
