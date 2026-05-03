package com.endless.engine.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe AudioManager.
 * SoundPool calls are safe from any thread.
 * MediaPlayer must be created/controlled on the main thread — all music
 * operations are posted to the main Looper via Handler.
 */
public class AudioManager {

    private static final String TAG = "AudioManager";

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SoundPool soundPool;
    private MediaPlayer musicPlayer;

    private final Map<String, Integer> soundCache    = new HashMap<>();
    private final Map<Integer, Integer> activeStreams = new HashMap<>();

    private float masterVolume = 1.0f;
    private float musicVolume  = 0.7f;

    public AudioManager(Context context) {
        this.context = context.getApplicationContext();

        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        soundPool = new SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(attrs)
            .build();

        Log.i(TAG, "AudioManager initialized");
    }

    // ─── SFX (safe to call from GL thread) ───────────────────────────────────

    public int loadSound(String filename) {
        if (soundCache.containsKey(filename)) return soundCache.get(filename);
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("sounds/" + filename);
            int id = soundPool.load(afd, 1);
            soundCache.put(filename, id);
            Log.i(TAG, "Loaded sound: " + filename + " id=" + id);
            return id;
        } catch (Exception e) {
            Log.w(TAG, "Cannot load sound: " + filename + " — " + e.getMessage());
            return -1;
        }
    }

    public int playSound(int soundId, float volume, float pitch, boolean loop) {
        if (soundPool == null || soundId < 0) return -1;
        float v = Math.max(0f, Math.min(1f, volume * masterVolume));
        int streamId = soundPool.play(soundId, v, v, 1, loop ? -1 : 0, pitch);
        activeStreams.put(soundId, streamId);
        return streamId;
    }

    public void stopSound(int soundId) {
        Integer streamId = activeStreams.get(soundId);
        if (streamId != null && soundPool != null) soundPool.stop(streamId);
    }

    public void setSoundVolume(int soundId, float volume) {
        Integer streamId = activeStreams.get(soundId);
        if (streamId != null && soundPool != null) {
            float v = Math.max(0f, Math.min(1f, volume * masterVolume));
            soundPool.setVolume(streamId, v, v);
        }
    }

    // ─── Music (posted to main thread to avoid GL thread MediaPlayer crash) ──

    public void playMusic(String filename, boolean loop) {
        mainHandler.post(() -> {
            stopMusicInternal();
            try {
                AssetFileDescriptor afd = context.getAssets().openFd("music/" + filename);
                musicPlayer = new MediaPlayer();
                musicPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
                musicPlayer.setDataSource(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                musicPlayer.setLooping(loop);
                musicPlayer.setVolume(musicVolume, musicVolume);
                musicPlayer.prepareAsync();   // non-blocking
                musicPlayer.setOnPreparedListener(MediaPlayer::start);
                musicPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                    return true;
                });
                Log.i(TAG, "playMusic: " + filename + " loop=" + loop);
            } catch (Exception e) {
                Log.w(TAG, "Cannot play music: " + filename + " — " + e.getMessage());
            }
        });
    }

    public void stopMusic() {
        mainHandler.post(this::stopMusicInternal);
    }

    private void stopMusicInternal() {
        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) musicPlayer.stop();
                musicPlayer.release();
            } catch (Exception ignored) {}
            musicPlayer = null;
        }
    }

    public void pauseMusic() {
        mainHandler.post(() -> {
            if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
        });
    }

    public void resumeMusic() {
        mainHandler.post(() -> {
            if (musicPlayer != null && !musicPlayer.isPlaying()) musicPlayer.start();
        });
    }

    public void setMusicVolume(float vol) {
        musicVolume = Math.max(0f, Math.min(1f, vol));
        mainHandler.post(() -> {
            if (musicPlayer != null) musicPlayer.setVolume(musicVolume, musicVolume);
        });
    }

    public void setMasterVolume(float vol) {
        masterVolume = Math.max(0f, Math.min(1f, vol));
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    public void onPause()  { pauseMusic(); }
    public void onResume() { resumeMusic(); }

    public void release() {
        mainHandler.post(() -> {
            stopMusicInternal();
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        });
    }
}
