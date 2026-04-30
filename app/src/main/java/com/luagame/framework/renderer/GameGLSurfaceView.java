package com.luagame.framework.renderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.luagame.framework.core.GameEngine;
import com.luagame.framework.input.InputManager;

/**
 * GLSurfaceView subclass that:
 *  - Requests an OpenGL ES 3.0 context
 *  - Attaches the GameRenderer
 *  - Forwards touch events to InputManager
 */
public class GameGLSurfaceView extends GLSurfaceView {

    private final GameEngine gameEngine;
    private final GameRenderer gameRenderer;

    public GameGLSurfaceView(Context context, GameEngine engine) {
        super(context);
        this.gameEngine = engine;

        // Request OpenGL ES 3.0
        setEGLContextClientVersion(3);

        // Enable depth buffer
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        gameRenderer = new GameRenderer(engine);
        setRenderer(gameRenderer);

        // Render continuously (game loop mode)
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputManager input = gameEngine.getInputManager();
        input.handleTouchEvent(event);
        return true;
    }
}
