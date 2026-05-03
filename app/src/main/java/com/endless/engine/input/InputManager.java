package com.endless.engine.input;

import android.view.MotionEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe input manager.
 * Touch events arrive on the UI thread; they are queued and consumed on the GL thread.
 */
public class InputManager {

    public enum TouchPhase { BEGAN, MOVED, ENDED, CANCELLED }

    public static class TouchEvent {
        public final int       pointerId;
        public final float     x, y;
        public final TouchPhase phase;

        TouchEvent(int id, float x, float y, TouchPhase phase) {
            this.pointerId = id; this.x = x; this.y = y; this.phase = phase;
        }
    }

    // Events queued from UI thread, consumed on GL thread
    private final ConcurrentLinkedQueue<TouchEvent> eventQueue = new ConcurrentLinkedQueue<>();

    // Current touch positions per pointer
    private final Map<Integer, float[]> activePointers = new HashMap<>();

    // Swipe delta since last frame
    private float swipeDeltaX = 0;
    private float swipeDeltaY = 0;

    // ─── Called from UI thread ────────────────────────────────────────────────

    public void handleTouchEvent(MotionEvent event) {
        int action      = event.getActionMasked();
        int pointerIdx  = event.getActionIndex();
        int pointerId   = event.getPointerId(pointerIdx);
        float x         = event.getX(pointerIdx);
        float y         = event.getY(pointerIdx);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                eventQueue.add(new TouchEvent(pointerId, x, y, TouchPhase.BEGAN));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    eventQueue.add(new TouchEvent(
                        event.getPointerId(i), event.getX(i), event.getY(i), TouchPhase.MOVED));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                eventQueue.add(new TouchEvent(pointerId, x, y, TouchPhase.ENDED));
                break;
            case MotionEvent.ACTION_CANCEL:
                eventQueue.add(new TouchEvent(pointerId, x, y, TouchPhase.CANCELLED));
                break;
        }
    }

    // ─── Called from GL thread ────────────────────────────────────────────────

    public void update() {
        swipeDeltaX = 0;
        swipeDeltaY = 0;

        TouchEvent evt;
        while ((evt = eventQueue.poll()) != null) {
            switch (evt.phase) {
                case BEGAN:
                    activePointers.put(evt.pointerId, new float[]{ evt.x, evt.y });
                    break;
                case MOVED: {
                    float[] prev = activePointers.get(evt.pointerId);
                    if (prev != null) {
                        swipeDeltaX += evt.x - prev[0];
                        swipeDeltaY += evt.y - prev[1];
                        prev[0] = evt.x;
                        prev[1] = evt.y;
                    }
                    break;
                }
                case ENDED:
                case CANCELLED:
                    activePointers.remove(evt.pointerId);
                    break;
            }
        }
    }

    // ─── Query methods (GL thread) ────────────────────────────────────────────

    public boolean isTouching() { return !activePointers.isEmpty(); }
    public int getTouchCount()  { return activePointers.size(); }

    public float getSwipeDeltaX() { return swipeDeltaX; }
    public float getSwipeDeltaY() { return swipeDeltaY; }

    /** Returns touch position for pointer 0, or null */
    public float[] getPrimaryTouch() {
        if (activePointers.isEmpty()) return null;
        return activePointers.values().iterator().next();
    }
}
