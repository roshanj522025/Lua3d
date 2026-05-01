package com.luagame.framework.scripting;

import android.content.res.AssetManager;
import android.util.Log;

import com.luagame.framework.core.GameEngine;
import com.luagame.framework.renderer.Mesh;
import com.luagame.framework.scene.Camera;
import com.luagame.framework.scene.SceneNode;

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

public class LuaEngine {

    private static final String TAG = "LuaEngine";
    private final GameEngine engine;
    private final Globals    globals;

    public LuaEngine(GameEngine engine) {
        this.engine  = engine;
        this.globals = JsePlatform.standardGlobals();
        registerAll();
        Log.i(TAG, "LuaEngine created.");
    }

    private void registerAll() {
        registerSceneAPI();
        registerCameraAPI();
        registerInputAPI();
        registerMeshAPI();
        registerLogAPI();
    }

    // ── Scene API ────────────────────────────────────────────────────────────
    // NOTE: always reads engine.getSceneManager() at call-time so it works
    // even after the SceneManager is replaced on GL context loss.

    private void registerSceneAPI() {
        LuaTable scene = new LuaTable();

        scene.set("createNode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue name) {
                SceneNode node = engine.getSceneManager().createNode(name.tojstring());
                Log.d(TAG, "Scene.createNode: " + name.tojstring());
                com.luagame.framework.core.GameActivity.log("[Lua] createNode: " + name.tojstring());
                return nodeToLua(node);
            }
        });
        scene.set("removeNode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue name) {
                engine.getSceneManager().removeNode(name.tojstring()); return NIL;
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

    private LuaTable nodeToLua(SceneNode node) {
        LuaTable t = new LuaTable();
        t.set("__name", LuaValue.valueOf(node.getName()));

        t.set("setPosition", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setPosition(a.tofloat(1), a.tofloat(2), a.tofloat(3)); return NIL;
            }
        });
        t.set("setRotation", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setRotation(a.tofloat(1), a.tofloat(2), a.tofloat(3)); return NIL;
            }
        });
        t.set("setScale", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setScale(a.tofloat(1), a.tofloat(2), a.tofloat(3)); return NIL;
            }
        });
        t.set("translate", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.translate(a.tofloat(1), a.tofloat(2), a.tofloat(3)); return NIL;
            }
        });
        t.set("rotate", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.rotate(a.tofloat(1), a.tofloat(2), a.tofloat(3)); return NIL;
            }
        });
        t.set("setColor", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setColor(a.tofloat(1), a.tofloat(2), a.tofloat(3)); return NIL;
            }
        });
        t.set("setMesh", new OneArgFunction() {
            @Override public LuaValue call(LuaValue v) {
                if (v instanceof LuaTable) {
                    Object ud = ((LuaTable)v).get("__mesh").touserdata();
                    if (ud instanceof Mesh) {
                        node.setMesh((Mesh) ud);
                        Log.d(TAG, "setMesh OK on " + node.getName());
                        com.luagame.framework.core.GameActivity.log("[Lua] setMesh OK: " + node.getName());
                    } else {
                        Log.e(TAG, "setMesh: __mesh userdata is null: " + ud);
                        com.luagame.framework.core.GameActivity.log("[Lua] setMesh FAIL: ud=" + ud);
                    }
                } else {
                    Log.e(TAG, "setMesh: not a table: " + v.getClass());
                    com.luagame.framework.core.GameActivity.log("[Lua] setMesh FAIL: not table: " + v);
                }
                return NIL;
            }
        });
        t.set("getPosition", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float[] p = node.getPosition();
                return LuaValue.varargsOf(LuaValue.valueOf(p[0]),LuaValue.valueOf(p[1]),LuaValue.valueOf(p[2]));
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
        input.set("getSwipeDelta", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                return LuaValue.varargsOf(
                    LuaValue.valueOf(engine.getInputManager().getSwipeDeltaX()),
                    LuaValue.valueOf(engine.getInputManager().getSwipeDeltaY()));
            }
        });
        globals.set("Input", input);
    }

    // ── Mesh API ─────────────────────────────────────────────────────────────

    private void registerMeshAPI() {
        LuaTable meshLib = new LuaTable();

        meshLib.set("cube", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                Log.d(TAG, "Mesh.cube() called");
                return meshToLua(Mesh.createCube());
            }
        });
        meshLib.set("sphere", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float radius  = (float) a.optdouble(1, 0.5);
                int   rings   = a.optint(2, 16);
                int   sectors = a.optint(3, 16);
                Log.d(TAG, "Mesh.sphere(" + radius + "," + rings + "," + sectors + ")");
                return meshToLua(Mesh.createSphere(radius, rings, sectors));
            }
        });
        meshLib.set("plane", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float size = (float) a.optdouble(1, 10.0);
                Log.d(TAG, "Mesh.plane(" + size + ")");
                return meshToLua(Mesh.createPlane(size));
            }
        });

        globals.set("Mesh", meshLib);
    }

    private LuaTable meshToLua(Mesh mesh) {
        LuaTable t = new LuaTable();
        t.set("__mesh", LuaValue.userdataOf(mesh));
        return t;
    }

    // ── Log API ──────────────────────────────────────────────────────────────

    private void registerLogAPI() {
        LuaTable log = new LuaTable();
        log.set("info",  new OneArgFunction() {
            @Override public LuaValue call(LuaValue m) { Log.i("Lua", m.tojstring()); return NIL; }
        });
        log.set("warn",  new OneArgFunction() {
            @Override public LuaValue call(LuaValue m) { Log.w("Lua", m.tojstring()); return NIL; }
        });
        log.set("error", new OneArgFunction() {
            @Override public LuaValue call(LuaValue m) { Log.e("Lua", m.tojstring()); return NIL; }
        });
        globals.set("Log", log);
        globals.set("print", new OneArgFunction() {
            @Override public LuaValue call(LuaValue m) { Log.d("Lua", m.tojstring()); return NIL; }
        });
    }

    // ── Script execution ─────────────────────────────────────────────────────

    /** Must be called on GL thread. Loads script and fires onStart(). */
    public void executeAssetScript(String path) {
        Log.i(TAG, "executeAssetScript: " + path);
        com.luagame.framework.core.GameActivity.log("[Lua] Loading: " + path);
        AssetManager assets = engine.getContext().getAssets();
        try (InputStream is = assets.open(path)) {
            byte[] b = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(b);
            String code = new String(b);
            Log.i(TAG, "Script loaded, " + b.length + " bytes.");
            com.luagame.framework.core.GameActivity.log("[Lua] Script " + b.length + " bytes, executing...");
            globals.load(code, "@" + path).call();
            Log.i(TAG, "Script executed.");
            com.luagame.framework.core.GameActivity.log("[Lua] Script executed, calling onStart()...");
            callGlobalFunction("onStart");
            Log.i(TAG, "onStart() done.");
            com.luagame.framework.core.GameActivity.log("[Lua] onStart() complete!");
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        com.luagame.framework.core.GameActivity.log("[Lua] ERROR IOException: " + e.getMessage());
        } catch (LuaError e) {
            Log.e(TAG, "LuaError: " + e.getMessage());
        com.luagame.framework.core.GameActivity.log("[Lua] ERROR: " + e.getMessage());
        }
    }

    /** Must be called on GL thread. Executes Lua string and fires onStart(). */
    public void executeString(String code) {
        Log.i(TAG, "executeString: " + code.length() + " chars");
        try {
            globals.load(code).call();
            callGlobalFunction("onStart");
        } catch (LuaError e) {
            Log.e(TAG, "LuaError: " + e.getMessage());
        }
    }

    public void callGlobalFunction(String name, Object... args) {
        LuaValue fn = globals.get(name);
        if (fn.isnil() || !fn.isfunction()) {
            Log.w(TAG, "callGlobalFunction: '" + name + "' not defined");
            return;
        }
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
            Log.e(TAG, "LuaError in " + name + "(): " + e.getMessage());
        }
    }

    public void resetSceneAPI(com.luagame.framework.scene.SceneManager sm) {
        registerSceneAPI(); // re-registers with fresh lambda closures using engine.getSceneManager()
    }

    public void close() {}
}
