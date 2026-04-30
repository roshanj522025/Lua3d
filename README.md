# LuaGameFramework for Android

A 3D game framework for Android that uses **OpenGL ES 3.0** for rendering and **LuaJ** to embed Lua as a first-class scripting language. Write your entire game in Lua — the Java layer is just the engine.

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                   GameActivity                       │
│  (sets up full-screen GL surface, loads main.lua)    │
└────────────────────┬─────────────────────────────────┘
                     │
         ┌───────────▼───────────┐
         │      GameEngine       │  ← Central singleton
         │  (owns all subsystems)│
         └──┬──────┬──────┬──────┘
            │      │      │
     ┌──────▼──┐ ┌─▼────┐ ┌▼──────────┐
     │Renderer │ │Scene │ │ LuaEngine │
     │(OpenGL  │ │Mgr   │ │ (LuaJ)    │
     │ ES 3.0) │ │      │ │           │
     └──────┬──┘ └──────┘ └───────────┘
            │
     ┌──────▼──────────┐
     │  ShaderProgram  │  ← Default Phong GLSL shader
     │  Mesh           │  ← VAO/VBO/IBO geometry
     │  Camera         │  ← Perspective view/proj matrices
     └─────────────────┘
```

### Game Loop (runs on GL thread)
```
onDrawFrame()
   └── GameEngine.tick(dt)
          ├── InputManager.update()
          ├── LuaEngine.callGlobalFunction("onUpdate", dt)
          ├── SceneManager.update(dt)
          └── Renderer.render()
```

---

## Lua API Reference

### Scene
```lua
local node = Scene.createNode("myNode")  -- create & register a node
Scene.removeNode("myNode")               -- remove from scene
local n   = Scene.getNode("myNode")      -- lookup by name
```

### Node methods
```lua
node:setPosition(x, y, z)
node:setRotation(rx, ry, rz)   -- Euler degrees
node:setScale(sx, sy, sz)
node:translate(dx, dy, dz)
node:rotate(drx, dry, drz)
node:setColor(r, g, b)         -- 0.0 – 1.0
node:setMesh(meshTable)
local x, y, z = node:getPosition()
```

### Mesh (primitives)
```lua
local m = Mesh.cube()
local m = Mesh.sphere(radius, rings, sectors)   -- defaults: 0.5, 16, 16
local m = Mesh.plane(size)                      -- default: 10.0
```

### Camera
```lua
Camera.setPosition(x, y, z)
Camera.setTarget(x, y, z)
Camera.setFov(degrees)
```

### Input
```lua
if Input.isTouching() then
    local dx, dy = Input.getSwipeDelta()   -- pixels moved this frame
    local tx, ty = Input.getPrimaryTouch() -- screen coords
    local n      = Input.getTouchCount()
end
```

### Logging
```lua
Log.info("message")
Log.warn("message")
Log.error("message")
print("message")   -- alias for Log.info
```

---

## Lua Lifecycle Hooks

Implement any of these global functions in your `main.lua`:

| Function | Called when |
|---|---|
| `onStart()` | GL surface created, GPU ready |
| `onUpdate(dt)` | Every frame; `dt` = delta-time in seconds |
| `onResize(w, h)` | Surface resized |
| `onPause()` | App backgrounded |
| `onResume()` | App foregrounded |

---

## Bundled Demo

`assets/scripts/main.lua` ships a **Mini Solar System** demo:

- **Sun** — slow self-rotation (yellow)
- **Earth** — orbits the sun with 23.5° axial tilt (blue)
- **Moon** — orbits the earth (grey)
- **Mars** — wider, slower orbit (red)
- **Floor plane** — reference ground grid

**Touch & drag** to orbit the camera around the scene.

---

## Project Structure

```
LuaGameFramework/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── assets/scripts/
│   │   └── main.lua            ← Your game starts here
│   └── java/com/luagame/framework/
│       ├── core/
│       │   ├── GameActivity.java
│       │   └── GameEngine.java
│       ├── renderer/
│       │   ├── GameGLSurfaceView.java
│       │   ├── GameRenderer.java
│       │   ├── Renderer.java
│       │   ├── ShaderProgram.java
│       │   └── Mesh.java
│       ├── scene/
│       │   ├── Camera.java
│       │   ├── SceneNode.java
│       │   └── SceneManager.java
│       ├── scripting/
│       │   └── LuaEngine.java
│       ├── input/
│       │   └── InputManager.java
│       └── audio/
│           └── AudioManager.java
└── build.gradle
```

---

## Requirements

- Android Studio Hedgehog or newer
- Android SDK 34
- Device/Emulator with OpenGL ES 3.0 support (API 24+)

---

## How to Build

1. Open the `LuaGameFramework/` folder in Android Studio
2. Let Gradle sync (it will download `luaj-jse:3.0.1` and `joml:1.10.5`)
3. Run on a device or emulator (API 24+)

---

## Extending the Framework

### Add a new Lua API table
In `LuaEngine.java`, add a `registerXxxAPI()` method and call it from the constructor:

```java
private void registerPhysicsAPI() {
    LuaTable physics = new LuaTable();
    physics.set("applyForce", new VarArgFunction() {
        @Override public Varargs invoke(Varargs a) {
            String name = a.tojstring(1);
            float fx = a.tofloat(2), fy = a.tofloat(3), fz = a.tofloat(4);
            // ... your physics code ...
            return NIL;
        }
    });
    globals.set("Physics", physics);
}
```

### Hot-reload a Lua script at runtime
```java
gameEngine.getLuaEngine().executeAssetScript("scripts/level2.lua");
```

### Custom GLSL shaders
```java
ShaderProgram custom = ShaderProgram.create(vertSrc, fragSrc);
gameEngine.getRenderer().getDefaultShader(); // swap as needed
```

---

## License
MIT — use freely in commercial and open-source projects.
