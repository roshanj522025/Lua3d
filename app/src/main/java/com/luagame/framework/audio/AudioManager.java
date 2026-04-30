package com.luagame.framework.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple audio manager using Android SoundPool for short SFX
 * and MediaPlayer for background music.
 *
 * Exposed to Lua via the Audio table (register in LuaEngine if needed).
 */
public class AudioManager {

    private static final String TAG = "AudioManager";

    private final Context context;
    private SoundPool soundPool;
    private final Map<String, Integer> soundCache = new HashMap<>();

    public AudioManager(Context context) {
        this.context = context;

        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        soundPool = new SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build();
    }

    /** Load a sound from assets/sounds/<filename> */
    public int loadSound(String filename) {
        if (soundCache.containsKey(filename)) return soundCache.get(filename);
        try {
            int id = soundPool.load(context.getAssets().openFd("sounds/" + filename), 1);
            soundCache.put(filename, id);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Could not load sound: " + filename, e);
            return -1;
        }
    }

    public void playSound(int soundId) {
        if (soundId < 0) return;
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
