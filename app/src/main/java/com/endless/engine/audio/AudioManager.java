package com.endless.engine.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Full audio manager:
 *  - SoundPool for short SFX  (Audio.loadSound / Audio.playSound / Audio.stopSound)
 *  - MediaPlayer for music     (Audio.playMusic / Audio.stopMusic / Audio.setMusicVolume)
 */
public class AudioManager {

    private static final String TAG = "AudioManager";

    private final Context   context;
    private SoundPool       soundPool;
    private MediaPlayer     musicPlayer;

    // filename → soundPool ID
    private final Map<String, Integer> soundCache    = new HashMap<>();
    // soundPool ID → last streamId (for stopping)
    private final Map<Integer, Integer> activeStreams = new HashMap<>();

    private float masterVolume = 1.0f;
    private float musicVolume  = 0.7f;
    private boolean initialized = false;

    public AudioManager(Context context) {
        this.context = context;
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        soundPool = new SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(attrs)
            .build();
        initialized = true;
        Log.i(TAG, "AudioManager initialized");
    }

    // ─── SFX ─────────────────────────────────────────────────────────────────

    /** Loads a sound from assets/sounds/<filename>. Returns soundId or -1 on error. */
    public int loadSound(String filename) {
        if (soundCache.containsKey(filename)) return soundCache.get(filename);
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("sounds/" + filename);
            int id = soundPool.load(afd, 1);
            soundCache.put(filename, id);
            Log.i(TAG, "Loaded sound: " + filename + " id=" + id);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Cannot load sound: " + filename + " — " + e.getMessage());
            return -1;
        }
    }

    /** Play a previously loaded sound. Returns streamId. */
    public int playSound(int soundId, float volume, float pitch, boolean loop) {
        if (!initialized || soundId < 0) return -1;
        float v = volume * masterVolume;
        int streamId = soundPool.play(soundId, v, v, 1, loop ? -1 : 0, pitch);
        activeStreams.put(soundId, streamId);
        return streamId;
    }

    public void stopSound(int soundId) {
        Integer streamId = activeStreams.get(soundId);
        if (streamId != null) soundPool.stop(streamId);
    }

    public void setSoundVolume(int soundId, float volume) {
        Integer streamId = activeStreams.get(soundId);
        if (streamId != null) {
            float v = volume * masterVolume;
            soundPool.setVolume(streamId, v, v);
        }
    }

    // ─── Music ────────────────────────────────────────────────────────────────

    /** Play background music from assets/music/<filename>. Loops by default. */
    public void playMusic(String filename, boolean loop) {
        stopMusic();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("music/" + filename);
            musicPlayer = new MediaPlayer();
            musicPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
            musicPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            musicPlayer.setLooping(loop);
            musicPlayer.setVolume(musicVolume, musicVolume);
            musicPlayer.prepare();
            musicPlayer.start();
            Log.i(TAG, "Playing music: " + filename);
        } catch (Exception e) {
            Log.e(TAG, "Cannot play music: " + filename + " — " + e.getMessage());
        }
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            if (musicPlayer.isPlaying()) musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
        }
    }

    public void pauseMusic() {
        if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
    }

    public void resumeMusic() {
        if (musicPlayer != null && !musicPlayer.isPlaying()) musicPlayer.start();
    }

    public void setMusicVolume(float vol) {
        musicVolume = Math.max(0f, Math.min(1f, vol));
        if (musicPlayer != null) musicPlayer.setVolume(musicVolume, musicVolume);
    }

    public void setMasterVolume(float vol) {
        masterVolume = Math.max(0f, Math.min(1f, vol));
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    public void onPause()  { pauseMusic(); }
    public void onResume() { resumeMusic(); }

    public void release() {
        stopMusic();
        if (soundPool != null) { soundPool.release(); soundPool = null; }
        initialized = false;
    }
}
