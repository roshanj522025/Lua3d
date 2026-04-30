package com.luagame.framework.scripting;

import android.content.res.AssetManager;
import android.util.Log;

import com.luagame.framework.core.GameEngine;
import com.luagame.framework.renderer.Mesh;
import com.luagame.framework.scene.Camera;
import com.luagame.framework.scene.SceneManager;
import com.luagame.framework.scene.SceneNode;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStream;

/**
 * Embeds LuaJ and exposes the entire game framework API to Lua scripts.
 *
 * Exposed Lua modules:
 *  - Scene   : node creation / manipulation / removal
 *  - Camera  : camera position / target
 *  - Input   : touch / swipe queries
 *  - Mesh    : primitive factory (cube, sphere, plane)
 *  - Log     : Android logcat from Lua
 */
public class LuaEngine {

    private static final String TAG = "LuaEngine";

    private final GameEngine engine;
    private final Globals    globals;

    public LuaEngine(GameEngine engine) {
        this.engine  = engine;
        this.globals = JsePlatform.standardGlobals();

        registerSceneAPI();
        registerCameraAPI();
        registerInputAPI();
        registerMeshAPI();
        registerLogAPI();

        Log.i(TAG, "Lua engine ready.");
    }

    // ─── Script loading ───────────────────────────────────────────────────────

    /** Load and execute a Lua script from assets/. */
    public void executeAssetScript(String assetPath) {
        AssetManager assets = engine.getContext().getAssets();
        try (InputStream is = assets.open(assetPath)) {
            byte[] bytes = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(bytes);
            String code = new String(bytes);
            globals.load(code, "@" + assetPath).call();
            Log.i(TAG, "Executed: " + assetPath);
        } catch (IOException e) {
            Log.e(TAG, "Could not open asset: " + assetPath, e);
        } catch (LuaError e) {
            Log.e(TAG, "Lua error in " + assetPath + ": " + e.getMessage());
        }
    }

    /** Execute a raw Lua string (useful for console / hot-reload). */
    public void executeString(String code) {
        try {
            globals.load(code).call();
        } catch (LuaError e) {
            Log.e(TAG, "Lua error: " + e.getMessage());
        }
    }

    /** Call a Lua global function with optional arguments. */
    public void callGlobalFunction(String name, Object... args) {
        LuaValue fn = globals.get(name);
        if (fn.isnil() || !fn.isfunction()) return;
        try {
            LuaValue[] luaArgs = new LuaValue[args.length];
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if (a instanceof Float)   luaArgs[i] = LuaValue.valueOf((Float)  a);
                else if (a instanceof Double) luaArgs[i] = LuaValue.valueOf((Double) a);
                else if (a instanceof Integer) luaArgs[i] = LuaValue.valueOf((Integer) a);
                else if (a instanceof String)  luaArgs[i] = LuaValue.valueOf((String)  a);
                else luaArgs[i] = LuaValue.NIL;
            }
            fn.invoke(LuaValue.varargsOf(luaArgs));
        } catch (LuaError e) {
            Log.e(TAG, "Lua error in " + name + ": " + e.getMessage());
        }
    }

    public void close() { /* LuaJ doesn't need explicit cleanup */ }

    // ─── Scene API ───────────────────────────────────────────────────────────

    private void registerSceneAPI() {
        LuaTable scene = new LuaTable();

        // Scene.createNode(name) -> node table
        scene.set("createNode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue name) {
                SceneNode node = engine.getSceneManager().createNode(name.tojstring());
                return nodeToLua(node);
            }
        });

        // Scene.removeNode(name)
        scene.set("removeNode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue name) {
                engine.getSceneManager().removeNode(name.tojstring());
                return NIL;
            }
        });

        // Scene.getNode(name) -> node table or nil
        scene.set("getNode", new OneArgFunction() {
            @Override public LuaValue call(LuaValue name) {
                SceneNode node = engine.getSceneManager().getNode(name.tojstring());
                return node != null ? nodeToLua(node) : NIL;
            }
        });

        globals.set("Scene", scene);
    }

    /**
     * Wraps a SceneNode as a Lua table with methods:
     *   node:setPosition(x,y,z)
     *   node:setRotation(rx,ry,rz)
     *   node:setScale(sx,sy,sz)
     *   node:translate(dx,dy,dz)
     *   node:rotate(drx,dry,drz)
     *   node:setColor(r,g,b)
     *   node:setMesh(meshTable)
     *   node:getPosition() -> x,y,z
     */
    private LuaTable nodeToLua(SceneNode node) {
        LuaTable t = new LuaTable();
        t.set("__name", LuaValue.valueOf(node.getName()));

        t.set("setPosition", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setPosition(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });

        t.set("setRotation", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setRotation(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });

        t.set("setScale", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setScale(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });

        t.set("translate", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.translate(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });

        t.set("rotate", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.rotate(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });

        t.set("setColor", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                node.setColor(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });

        t.set("setMesh", new OneArgFunction() {
            @Override public LuaValue call(LuaValue meshTable) {
                if (meshTable instanceof LuaTable) {
                    Object data = ((LuaTable) meshTable).get("__mesh").touserdata();
                    if (data instanceof Mesh) node.setMesh((Mesh) data);
                }
                return NIL;
            }
        });

        t.set("getPosition", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float[] p = node.getPosition();
                return LuaValue.varargsOf(
                    LuaValue.valueOf(p[0]), LuaValue.valueOf(p[1]), LuaValue.valueOf(p[2]));
            }
        });

        return t;
    }

    // ─── Camera API ──────────────────────────────────────────────────────────

    private void registerCameraAPI() {
        LuaTable cam = new LuaTable();
        Camera camera = engine.getRenderer().getActiveCamera();

        cam.set("setPosition", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                camera.setPosition(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });

        cam.set("setTarget", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                camera.setTarget(a.tofloat(1), a.tofloat(2), a.tofloat(3));
                return NIL;
            }
        });

        cam.set("setFov", new OneArgFunction() {
            @Override public LuaValue call(LuaValue v) {
                camera.setFov(v.tofloat());
                return NIL;
            }
        });

        globals.set("Camera", cam);
    }

    // ─── Input API ───────────────────────────────────────────────────────────

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

    // ─── Mesh API ────────────────────────────────────────────────────────────

    private void registerMeshAPI() {
        LuaTable meshLib = new LuaTable();

        meshLib.set("cube", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                return meshToLua(Mesh.createCube());
            }
        });

        meshLib.set("sphere", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float radius  = a.optfloat(1, 0.5f);
                int   rings   = a.optint(2, 16);
                int   sectors = a.optint(3, 16);
                return meshToLua(Mesh.createSphere(radius, rings, sectors));
            }
        });

        meshLib.set("plane", new VarArgFunction() {
            @Override public Varargs invoke(Varargs a) {
                float size = a.optfloat(1, 10f);
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

    // ─── Log API ─────────────────────────────────────────────────────────────

    private void registerLogAPI() {
        LuaTable log = new LuaTable();

        log.set("info", new OneArgFunction() {
            @Override public LuaValue call(LuaValue msg) {
                Log.i("Lua", msg.tojstring()); return NIL;
            }
        });
        log.set("warn", new OneArgFunction() {
            @Override public LuaValue call(LuaValue msg) {
                Log.w("Lua", msg.tojstring()); return NIL;
            }
        });
        log.set("error", new OneArgFunction() {
            @Override public LuaValue call(LuaValue msg) {
                Log.e("Lua", msg.tojstring()); return NIL;
            }
        });

        globals.set("Log", log);

        // Also override Lua's print() to go to logcat
        globals.set("print", new OneArgFunction() {
            @Override public LuaValue call(LuaValue msg) {
                Log.d("Lua", msg.tojstring()); return NIL;
            }
        });
    }
}
