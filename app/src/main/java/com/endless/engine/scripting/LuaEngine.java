package com.endless.engine.scripting;

import android.content.res.AssetManager;
import android.util.Log;

import com.endless.engine.audio.AudioManager;
import com.endless.engine.core.GameActivity;
import com.endless.engine.core.GameEngine;
import com.endless.engine.renderer.Mesh;
import com.endless.engine.renderer.Texture;
import com.endless.engine.scene.SceneNode;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Embeds LuaJ and exposes the full Endless engine API to Lua scripts.
 *
 * Lua modules:
 *   Scene   — node creation/manipulation
 *   Camera  — position, target, fov
 *   Input   — touch / swipe
 *   Mesh    — cube, sphere, plane, loadOBJ
 *   Texture — load from assets/textures/
 *   Audio   — SFX (loadSound/play/stop) + Music (play/stop/volume)
 *   Log     — logcat from Lua
 */
public class LuaEngine {

    private static final String TAG = "LuaEngine";

    private final GameEngine engine;
    private final Globals    globals;

    // Integer-keyed registries to avoid LuaJ userdata issues
    private final Map<Integer, Mesh>    meshRegistry    = new HashMap<>();
    private final Map<Integer, Texture> textureRegistry = new HashMap<>();
    private int nextMeshId    = 1;
    private int nextTextureId = 1;

    public LuaEngine(GameEngine engine) {
        this.engine  = engine;
        this.globals = JsePlatform.standardGlobals();
        registerAll();
        Log.i(TAG, "LuaEngine ready.");
    }

    private void registerAll() {
        registerSceneAPI();
        registerCameraAPI();
        registerInputAPI();
        registerMeshAPI();
        registerTextureAPI();
        registerAudioAPI();
        registerLogAPI();
    }

    // ── Scene API ────────────────────────────────────────────────────────────

    private void registerSceneAPI() {
        LuaTable scene = new LuaTable();

        scene.set("createNode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue name) {
                SceneNode node = engine.getSceneManager().createNode(name.tojstring());
                GameActivity.log("[Lua] createNode: " + name.tojstring());
                return nodeToLua(node);
            }
        });

        scene.set("removeNode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue name) {
                engine.getSceneManager().removeNode(name.tojstring());
                return NIL;
            }
        });

        scene.set("getNode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue name) {
                SceneNode n = engine.getSceneManager().getNode(name.tojstring());
                return n != null ? nodeToLua(n) : NIL;
            }
        });

        globals.set("Scene", scene);
    }

    /**
     * Wraps a SceneNode as a Lua table.
     * All methods use colon-syntax (self = arg 1, real args start at arg 2).
     */
    private LuaTable nodeToLua(SceneNode node) {
        LuaTable t = new LuaTable();
        t.set("__name", LuaValue.valueOf(node.getName()));

        t.set("setPosition", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setPosition(a.tofloat(2), a.tofloat(3), a.tofloat(4)); return NIL;
            }
        });
        t.set("setRotation", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setRotation(a.tofloat(2), a.tofloat(3), a.tofloat(4)); return NIL;
            }
        });
        t.set("setScale", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setScale(a.tofloat(2), a.tofloat(3), a.tofloat(4)); return NIL;
            }
        });
        t.set("translate", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.translate(a.tofloat(2), a.tofloat(3), a.tofloat(4)); return NIL;
            }
        });
        t.set("rotate", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.rotate(a.tofloat(2), a.tofloat(3), a.tofloat(4)); return NIL;
            }
        });
        t.set("setColor", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setColor(a.tofloat(2), a.tofloat(3), a.tofloat(4)); return NIL;
            }
        });
        t.set("getPosition", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float[] p = node.getPosition();
                return LuaValue.varargsOf(
                    LuaValue.valueOf(p[0]), LuaValue.valueOf(p[1]), LuaValue.valueOf(p[2]));
            }
        });

        // node:setMesh(meshTable)
        t.set("setMesh", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                LuaValue v = a.arg(2);
                if (v instanceof LuaTable) {
                    int id = ((LuaTable) v).get("__meshId").toint();
                    Mesh m  = meshRegistry.get(id);
                    if (m != null) {
                        node.setMesh(m);
                        GameActivity.log("[Lua] setMesh OK: " + node.getName());
                    } else {
                        GameActivity.log("[Lua] setMesh FAIL: meshId=" + id + " not found");
                    }
                }
                return NIL;
            }
        });

        // node:setTexture(textureTable)
        t.set("setTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                LuaValue v = a.arg(2);
                if (v instanceof LuaTable) {
                    int id  = ((LuaTable) v).get("__texId").toint();
                    Texture tex = textureRegistry.get(id);
                    if (tex != null) {
                        node.setTexture(tex);
                        GameActivity.log("[Lua] setTexture OK: " + node.getName());
                    } else {
                        GameActivity.log("[Lua] setTexture FAIL: texId=" + id + " not found");
                    }
                }
                return NIL;
            }
        });

        // node:clearTexture()
        t.set("clearTexture", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setTexture(null);
                return NIL;
            }
        });

        return t;
    }

    // ── Camera API ───────────────────────────────────────────────────────────

    private void registerCameraAPI() {
        LuaTable cam = new LuaTable();

        cam.set("setPosition", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                engine.getRenderer().getActiveCamera()
                    .setPosition(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });
        cam.set("setTarget", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                engine.getRenderer().getActiveCamera()
                    .setTarget(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });
        cam.set("setFov", new OneArgFunction() {
            @Override public LuaValue call(LuaValue v) {
                engine.getRenderer().getActiveCamera().setFov(v.tofloat());
                return NIL;
            }
        });

        globals.set("Camera", cam);
    }

    // ── Input API ────────────────────────────────────────────────────────────

    private void registerInputAPI() {
        LuaTable input = new LuaTable();

        input.set("isTouching", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                return LuaValue.valueOf(engine.getInputManager().isTouching());
            }
        });
        input.set("getTouchCount", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                return LuaValue.valueOf(engine.getInputManager().getTouchCount());
            }
        });
        input.set("getSwipeDelta", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                return LuaValue.varargsOf(
                    LuaValue.valueOf(engine.getInputManager().getSwipeDeltaX()),
                    LuaValue.valueOf(engine.getInputManager().getSwipeDeltaY()));
            }
        });
        input.set("getPrimaryTouch", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float[] t = engine.getInputManager().getPrimaryTouch();
                if (t == null) return NIL;
                return LuaValue.varargsOf(LuaValue.valueOf(t[0]), LuaValue.valueOf(t[1]));
            }
        });

        globals.set("Input", input);
    }

    // ── Mesh API ─────────────────────────────────────────────────────────────

    private void registerMeshAPI() {
        LuaTable meshLib = new LuaTable();

        meshLib.set("cube", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                return meshToLua(Mesh.createCube());
            }
        });
        meshLib.set("sphere", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float radius  = (float) a.optdouble(1, 0.5);
                int   rings   = a.optint(2, 16);
                int   sectors = a.optint(3, 16);
                return meshToLua(Mesh.createSphere(radius, rings, sectors));
            }
        });
        meshLib.set("plane", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float size = (float) a.optdouble(1, 10.0);
                return meshToLua(Mesh.createPlane(size));
            }
        });

        // Mesh.loadOBJ("model.obj")  — loads from assets/models/
        meshLib.set("loadOBJ", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                String filename = a.tojstring(1);
                GameActivity.log("[Lua] Mesh.loadOBJ: " + filename);
                try {
                    Mesh m = Mesh.loadOBJ(engine.getContext(), filename);
                    return meshToLua(m);
                } catch (Exception e) {
                    GameActivity.log("[Lua] loadOBJ ERROR: " + e.getMessage());
                    return NIL;
                }
            }
        });

        globals.set("Mesh", meshLib);
    }

    private LuaTable meshToLua(Mesh mesh) {
        int id = nextMeshId++;
        meshRegistry.put(id, mesh);
        LuaTable t = new LuaTable();
        t.set("__meshId", LuaValue.valueOf(id));
        return t;
    }

    // ── Texture API ──────────────────────────────────────────────────────────

    private void registerTextureAPI() {
        LuaTable texLib = new LuaTable();

        // Texture.load("brick.png")  — loads from assets/textures/
        texLib.set("load", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                String filename = a.tojstring(1);
                GameActivity.log("[Lua] Texture.load: " + filename);
                Texture tex = Texture.fromAsset(engine.getContext(), filename);
                if (!tex.isValid()) {
                    GameActivity.log("[Lua] Texture.load FAIL: " + filename);
                    return NIL;
                }
                int id = nextTextureId++;
                textureRegistry.put(id, tex);
                LuaTable t = new LuaTable();
                t.set("__texId", LuaValue.valueOf(id));
                GameActivity.log("[Lua] Texture loaded OK id=" + id + " (" + tex.getWidth() + "x" + tex.getHeight() + ")");
                return t;
            }
        });

        globals.set("Texture", texLib);
    }

    // ── Audio API ────────────────────────────────────────────────────────────

    private void registerAudioAPI() {
        LuaTable audioLib = new LuaTable();
        AudioManager audio = engine.getAudioManager();

        // ─ SFX ───────────────────────────────────────────────────────────────

        // local sfx = Audio.loadSound("hit.wav")
        audioLib.set("loadSound", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                String filename = a.tojstring(1);
                int id = audio.loadSound(filename);
                GameActivity.log("[Audio] loadSound: " + filename + " id=" + id);
                return LuaValue.valueOf(id);
            }
        });

        // Audio.playSound(sfxId)
        // Audio.playSound(sfxId, volume, pitch, loop)
        audioLib.set("playSound", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                int    id     = a.toint(1);
                float  vol    = (float) a.optdouble(2, 1.0);
                float  pitch  = (float) a.optdouble(3, 1.0);
                boolean loop  = a.optboolean(4, false);
                int streamId  = audio.playSound(id, vol, pitch, loop);
                return LuaValue.valueOf(streamId);
            }
        });

        // Audio.stopSound(sfxId)
        audioLib.set("stopSound", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                audio.stopSound(a.toint(1));
                return NIL;
            }
        });

        // Audio.setSoundVolume(sfxId, volume)
        audioLib.set("setSoundVolume", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                audio.setSoundVolume(a.toint(1), a.tofloat(2));
                return NIL;
            }
        });

        // ─ Music ─────────────────────────────────────────────────────────────

        // Audio.playMusic("theme.mp3")
        // Audio.playMusic("theme.mp3", false)  -- no loop
        audioLib.set("playMusic", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                String filename = a.tojstring(1);
                boolean loop    = a.optboolean(2, true);
                audio.playMusic(filename, loop);
                GameActivity.log("[Audio] playMusic: " + filename);
                return NIL;
            }
        });

        audioLib.set("stopMusic", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                audio.stopMusic();
                return NIL;
            }
        });

        audioLib.set("pauseMusic", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                audio.pauseMusic();
                return NIL;
            }
        });

        audioLib.set("resumeMusic", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                audio.resumeMusic();
                return NIL;
            }
        });

        // Audio.setMusicVolume(0.5)
        audioLib.set("setMusicVolume", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                audio.setMusicVolume(a.tofloat(1));
                return NIL;
            }
        });

        // Audio.setMasterVolume(0.8)
        audioLib.set("setMasterVolume", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                audio.setMasterVolume(a.tofloat(1));
                return NIL;
            }
        });

        globals.set("Audio", audioLib);
    }

    // ── Log API ──────────────────────────────────────────────────────────────

    private void registerLogAPI() {
        LuaTable log = new LuaTable();
        log.set("info",  new OneArgFunction() {
            @Override public LuaValue call(LuaValue m) {
                Log.i("Lua", m.tojstring());
                GameActivity.log("[Lua] " + m.tojstring());
                return NIL;
            }
        });
        log.set("warn",  new OneArgFunction() {
            @Override public LuaValue call(LuaValue m) {
                Log.w("Lua", m.tojstring());
                GameActivity.log("[Lua] WARN: " + m.tojstring());
                return NIL;
            }
        });
        log.set("error", new OneArgFunction() {
            @Override public LuaValue call(LuaValue m) {
                Log.e("Lua", m.tojstring());
                GameActivity.log("[Lua] ERROR: " + m.tojstring());
                return NIL;
            }
        });
        globals.set("Log", log);
        globals.set("print", new OneArgFunction() {
            @Override public LuaValue call(LuaValue m) {
                Log.d("Lua", m.tojstring());
                return NIL;
            }
        });
    }

    // ── Script execution ─────────────────────────────────────────────────────

    public void executeAssetScript(String path) {
        GameActivity.log("[Lua] Loading: " + path);
        AssetManager assets = engine.getContext().getAssets();
        try (InputStream is = assets.open(path)) {
            byte[] b = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(b);
            String code = new String(b);
            GameActivity.log("[Lua] Script " + b.length + " bytes, executing...");
            globals.load(code, "@" + path).call();
            GameActivity.log("[Lua] Script executed, calling onStart()...");
            callGlobalFunction("onStart");
            GameActivity.log("[Lua] onStart() complete!");
        } catch (IOException e) {
            GameActivity.log("[Lua] ERROR IOException: " + e.getMessage());
        } catch (LuaError e) {
            GameActivity.log("[Lua] ERROR: " + e.getMessage());
            Log.e(TAG, "LuaError: " + e.getMessage());
        }
    }

    public void executeString(String code) {
        try {
            globals.load(code).call();
            callGlobalFunction("onStart");
        } catch (LuaError e) {
            GameActivity.log("[Lua] ERROR: " + e.getMessage());
            Log.e(TAG, "LuaError: " + e.getMessage());
        }
    }

    public void callGlobalFunction(String name, Object... args) {
        LuaValue fn = globals.get(name);
        if (fn.isnil() || !fn.isfunction()) return;
        try {
            LuaValue[] luaArgs = new LuaValue[args.length];
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if      (a instanceof Float)   luaArgs[i] = LuaValue.valueOf((Float)   a);
                else if (a instanceof Double)  luaArgs[i] = LuaValue.valueOf((Double)  a);
                else if (a instanceof Integer) luaArgs[i] = LuaValue.valueOf((Integer) a);
                else if (a instanceof String)  luaArgs[i] = LuaValue.valueOf((String)  a);
                else luaArgs[i] = LuaValue.NIL;
            }
            fn.invoke(LuaValue.varargsOf(luaArgs));
        } catch (LuaError e) {
            GameActivity.log("[Lua] ERROR in " + name + "(): " + e.getMessage());
            Log.e(TAG, "LuaError in " + name + ": " + e.getMessage());
        }
    }

    public void resetSceneAPI(com.endless.engine.scene.SceneManager sm) {
        registerSceneAPI();
    }

    public void close() {}
}
