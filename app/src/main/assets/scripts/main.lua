--[[
  LuaGameFramework — Demo: Mini Solar System
  ==========================================
  Demonstrates:
    • Scene node creation with primitive meshes
    • Per-frame rotation via onUpdate(dt)
    • Camera setup
    • Hierarchical transforms (moon orbits planet)
    • Touch-swipe to orbit the camera
  
  Available framework globals:
    Scene   – createNode, removeNode, getNode
    Camera  – setPosition, setTarget, setFov
    Mesh    – cube(), sphere(radius, rings, sectors), plane(size)
    Input   – isTouching(), getSwipeDelta(), getPrimaryTouch()
    Log     – info(msg), warn(msg), error(msg)
    print(msg) – alias for Log.info
--]]

-- ─── Scene state ─────────────────────────────────────────────────────────────

local sun, earth, moon, mars, floor

-- Camera orbit state (modified by swipe)
local camYaw   = 20.0   -- horizontal angle (degrees)
local camPitch = 25.0   -- vertical angle   (degrees)
local camDist  = 12.0   -- distance from origin

-- Accumulated time for orbital animations
local time = 0.0

-- ─── Helper: update camera from spherical coords ─────────────────────────────

local function updateCamera()
    local yawRad   = math.rad(camYaw)
    local pitchRad = math.rad(camPitch)
    local cx = camDist * math.cos(pitchRad) * math.sin(yawRad)
    local cy = camDist * math.sin(pitchRad)
    local cz = camDist * math.cos(pitchRad) * math.cos(yawRad)
    Camera.setPosition(cx, cy, cz)
    Camera.setTarget(0, 0, 0)
end

-- ─── onStart: called once when GL surface is ready ───────────────────────────

function onStart()
    Log.info("=== Solar System Demo Starting ===")

    Camera.setFov(55)
    updateCamera()

    -- Floor / grid plane
    floor = Scene.createNode("floor")
    floor:setMesh(Mesh.plane(30))
    floor:setColor(0.1, 0.15, 0.1)
    floor:setPosition(0, -1.5, 0)

    -- Sun (large yellow sphere at origin)
    sun = Scene.createNode("sun")
    sun:setMesh(Mesh.sphere(1.0, 20, 20))
    sun:setColor(1.0, 0.85, 0.1)
    sun:setPosition(0, 0, 0)

    -- Earth (medium blue-green sphere)
    earth = Scene.createNode("earth")
    earth:setMesh(Mesh.sphere(0.35, 16, 16))
    earth:setColor(0.2, 0.5, 0.9)

    -- Moon (small grey sphere, child of earth for relative orbit)
    moon = Scene.createNode("moon")
    moon:setMesh(Mesh.sphere(0.12, 12, 12))
    moon:setColor(0.7, 0.7, 0.7)

    -- Mars (rusty red sphere)
    mars = Scene.createNode("mars")
    mars:setMesh(Mesh.sphere(0.28, 16, 16))
    mars:setColor(0.8, 0.3, 0.15)

    Log.info("Scene nodes created.")
end

-- ─── onUpdate: called every frame with delta-time in seconds ─────────────────

function onUpdate(dt)
    time = time + dt

    -- Sun: slow self-rotation
    sun:setRotation(0, time * 15, 0)

    -- Earth: orbit the sun at radius 4, period ~10s
    local ex = math.cos(time * 0.63) * 4.0
    local ez = math.sin(time * 0.63) * 4.0
    earth:setPosition(ex, 0, ez)
    earth:setRotation(23.5, time * 60, 0)   -- axial tilt + self-spin

    -- Moon: orbit earth at radius 0.85, period ~3s
    local mx = ex + math.cos(time * 2.1) * 0.85
    local mz = ez + math.sin(time * 2.1) * 0.85
    moon:setPosition(mx, 0, mz)

    -- Mars: orbit the sun at radius 6.5, slower period ~16s
    local arx = math.cos(time * 0.39) * 6.5
    local arz = math.sin(time * 0.39) * 6.5
    mars:setPosition(arx, 0, arz)
    mars:setRotation(0, time * 45, 0)

    -- Touch-drag to orbit camera
    if Input.isTouching() then
        local dx, dy = Input.getSwipeDelta()
        camYaw   = camYaw   + dx * 0.3
        camPitch = camPitch - dy * 0.3
        -- Clamp pitch to avoid gimbal issues
        if camPitch >  80 then camPitch =  80 end
        if camPitch < -10 then camPitch = -10 end
        updateCamera()
    end
end

-- ─── onResize: surface dimensions changed ────────────────────────────────────

function onResize(w, h)
    Log.info("Screen resized: " .. w .. "x" .. h)
    -- Camera aspect ratio is updated by the Java side automatically
end

-- ─── Lifecycle hooks ─────────────────────────────────────────────────────────

function onPause()
    Log.info("Game paused")
end

function onResume()
    Log.info("Game resumed")
end

print("main.lua loaded successfully")
