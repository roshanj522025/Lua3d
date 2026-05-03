--[[
  Endless Engine — Feature Showcase
  ===================================
  Demonstrates all current engine capabilities:
    • Mesh primitives + OBJ loading
    • Texture mapping
    • Audio (SFX + background music)
    • Scene graph / hierarchy
    • Camera control
    • Touch input (tap = cycle scenes, drag = orbit)
--]]

local time       = 0.0
local scene      = 1
local tapTimer   = 0.0
local wasTouching = false
local camYaw, camPitch, camDist = 30.0, 25.0, 10.0
local nodes = {}

-- Preloaded assets (loaded once in onStart)
local sfxPop    = nil
local sfxWhoosh = nil

local function clamp(v, lo, hi) return math.max(lo, math.min(hi, v)) end

local function updateCamera()
    local yr, pr = math.rad(camYaw), math.rad(camPitch)
    Camera.setPosition(
        camDist * math.cos(pr) * math.sin(yr),
        camDist * math.sin(pr),
        camDist * math.cos(pr) * math.cos(yr))
    Camera.setTarget(0, 0, 0)
end

local function clearNodes()
    for _, n in ipairs(nodes) do Scene.removeNode(n) end
    nodes = {}
end

local function add(name)
    nodes[#nodes + 1] = name
    return Scene.createNode(name)
end

-- ─── Scene 1: Textured primitives ────────────────────────────────────────────
local function buildScene1()
    Log.info("Scene 1: Textured Primitives")
    Camera.setFov(55); camDist=10; camPitch=22; camYaw=30

    -- Floor with texture (falls back to color if no texture file present)
    local floor = add("floor")
    floor:setMesh(Mesh.plane(16))
    floor:setColor(0.4, 0.4, 0.45)
    floor:setPosition(0, -1.5, 0)
    local floorTex = Texture.load("grid.png")
    if floorTex then floor:setTexture(floorTex) end

    -- Textured cube
    local cube = add("cube")
    cube:setMesh(Mesh.cube())
    cube:setColor(1.0, 1.0, 1.0)   -- white tint so texture shows true color
    cube:setPosition(-3, 0, 0)
    cube:setScale(1.3, 1.3, 1.3)
    local crateTex = Texture.load("crate.png")
    if crateTex then cube:setTexture(crateTex) end

    -- Textured sphere
    local sphere = add("sphere")
    sphere:setMesh(Mesh.sphere(0.9, 24, 24))
    sphere:setColor(1.0, 1.0, 1.0)
    sphere:setPosition(0, 0, 0)
    local earthTex = Texture.load("earth.png")
    if earthTex then sphere:setTexture(earthTex) end

    -- OBJ model (falls back to cube if file not present)
    local model = add("model")
    local objMesh = Mesh.loadOBJ("teapot.obj")
    if objMesh then
        model:setMesh(objMesh)
    else
        model:setMesh(Mesh.cube())
    end
    model:setColor(0.8, 0.6, 0.2)
    model:setPosition(3, -0.5, 0)
    model:setScale(0.7, 0.7, 0.7)
end

-- ─── Scene 2: Audio demo ──────────────────────────────────────────────────────
local function buildScene2()
    Log.info("Scene 2: Audio Demo — tap objects to play sounds")
    Camera.setFov(60); camDist=10; camPitch=20; camYaw=0

    -- Visual representation of audio: bouncing spheres
    local colors = {
        {1.0, 0.3, 0.3}, {0.3, 0.9, 0.3}, {0.3, 0.5, 1.0},
        {1.0, 0.8, 0.1}, {0.9, 0.3, 0.9}
    }
    for i = 1, 5 do
        local n = add("ball" .. i)
        n:setMesh(Mesh.sphere(0.5, 16, 16))
        n:setColor(colors[i][1], colors[i][2], colors[i][3])
        n:setPosition((i - 3) * 2.2, 0, 0)
    end

    -- Floor
    local f = add("s2floor")
    f:setMesh(Mesh.plane(20))
    f:setColor(0.1, 0.1, 0.15)
    f:setPosition(0, -1.5, 0)

    -- Start background music (if file present)
    Audio.playMusic("ambient.mp3", true)
    Audio.setMusicVolume(0.5)
    Log.info("Music started (if ambient.mp3 exists in assets/music/)")
end

-- ─── Scene 3: Solar system ────────────────────────────────────────────────────
local function buildScene3()
    Log.info("Scene 3: Solar System")
    Camera.setFov(60); camDist=14; camPitch=30; camYaw=20

    add("s3floor"):setMesh(Mesh.plane(30))
    Scene.getNode("s3floor"):setColor(0.04, 0.04, 0.1)
    Scene.getNode("s3floor"):setPosition(0,-2.5,0)

    local sun = add("sun")
    sun:setMesh(Mesh.sphere(1.1, 20, 20))
    sun:setColor(1.0, 0.85, 0.1)

    local p1 = add("p1"); p1:setMesh(Mesh.sphere(0.38,16,16)); p1:setColor(0.2,0.5,0.95)
    local m1 = add("m1"); m1:setMesh(Mesh.sphere(0.13,12,12)); m1:setColor(0.75,0.75,0.75)
    local p2 = add("p2"); p2:setMesh(Mesh.sphere(0.28,16,16)); p2:setColor(0.9,0.45,0.1)
    local p3 = add("p3"); p3:setMesh(Mesh.sphere(0.5,16,16));  p3:setColor(0.55,0.2,0.85)

    for i = 1, 6 do
        local r = add("ring"..i)
        r:setMesh(Mesh.cube())
        r:setColor(0.7, 0.6, 0.9)
        r:setScale(0.15, 0.04, 0.15)
    end
end

-- ─── Scene 4: Floating islands ────────────────────────────────────────────────
local function buildScene4()
    Log.info("Scene 4: Floating Islands")
    Camera.setFov(50); camDist=11; camPitch=22; camYaw=55

    local base = add("base"); base:setMesh(Mesh.cube()); base:setColor(0.35,0.6,0.3)
    base:setPosition(0,0,0); base:setScale(5,0.5,5)
    local baseTex = Texture.load("grass.png")
    if baseTex then base:setTexture(baseTex) end

    local dirt = add("dirt"); dirt:setMesh(Mesh.cube()); dirt:setColor(0.45,0.3,0.15)
    dirt:setPosition(0,-0.6,0); dirt:setScale(4.5,0.7,4.5)

    local tp = {{-1.5,0.3,-1.2},{1.2,0.3,0.8},{-0.5,0.3,1.5},{1.8,0.3,-1.5}}
    for i, p in ipairs(tp) do
        local trunk = add("trunk"..i); trunk:setMesh(Mesh.cube())
        trunk:setColor(0.4,0.25,0.1); trunk:setPosition(p[1],p[2]+0.5,p[3]); trunk:setScale(0.15,0.8,0.15)
        local leaves = add("leaves"..i); leaves:setMesh(Mesh.sphere(0.4,10,10))
        leaves:setColor(0.2, 0.65+i*0.04, 0.25); leaves:setPosition(p[1],p[2]+1.3,p[3])
    end

    local minis = {{3.5,0.8,2.0},{-3.2,1.5,-1.5},{2.0,2.2,-3.0}}
    for i, m in ipairs(minis) do
        local mini = add("mini"..i); mini:setMesh(Mesh.cube())
        mini:setColor(0.35+i*0.05, 0.55, 0.25); mini:setPosition(m[1],m[2],m[3]); mini:setScale(1.5,0.35,1.5)
        local crystal = add("crystal"..i); crystal:setMesh(Mesh.sphere(0.22,8,8))
        crystal:setColor(0.4+i*0.2, 0.8, 0.9); crystal:setPosition(m[1],m[2]+0.45,m[3])
    end

    for h = 1, 5 do
        local b = add("tb"..h); b:setMesh(Mesh.cube())
        local t = h/5.0; b:setColor(0.6+t*0.3, 0.6-t*0.2, 0.7+t*0.2)
        b:setPosition(0, 0.25+(h-1)*0.85, 0); b:setScale(0.8-h*0.08, 0.8, 0.8-h*0.08)
    end
end

-- ─── Scene switcher ──────────────────────────────────────────────────────────

local builders = {buildScene1, buildScene2, buildScene3, buildScene4}
local names    = {"Textured Primitives", "Audio Demo", "Solar System", "Floating Islands"}

local function switchScene(n)
    Audio.stopMusic()
    scene = ((n - 1) % 4) + 1
    clearNodes()
    builders[scene]()
    updateCamera()
    if sfxWhoosh then Audio.playSound(sfxWhoosh, 0.7, 1.0, false) end
    Log.info("Scene " .. scene .. ": " .. names[scene])
end

-- ─── Lifecycle ────────────────────────────────────────────────────────────────

function onStart()
    Log.info("=== Endless Engine — Full Showcase ===")

    -- Preload SFX (won't error if files are missing)
    sfxPop    = Audio.loadSound("pop.wav")
    sfxWhoosh = Audio.loadSound("whoosh.wav")

    switchScene(1)
    Log.info("Tap to cycle scenes | Drag to orbit")
end

function onUpdate(dt)
    time     = time + dt
    tapTimer = tapTimer + dt

    -- Touch input
    local touching = Input.isTouching()
    local dx, dy   = Input.getSwipeDelta()
    if touching then
        local moved = math.abs(dx) + math.abs(dy)
        if moved > 1.0 then
            camYaw   = camYaw   + dx * 0.25
            camPitch = clamp(camPitch - dy * 0.25, -5, 80)
            updateCamera()
        end
        if not wasTouching and tapTimer > 0.15 then
            tapTimer = 0.0
            switchScene(scene + 1)
        end
    end
    wasTouching = touching

    -- ── Per-scene animation ──────────────────────────────────────────────────
    if scene == 1 then
        local cube = Scene.getNode("cube")
        if cube then cube:setRotation(time*25, time*40, 0) end
        local sphere = Scene.getNode("sphere")
        if sphere then sphere:setRotation(0, time*20, 0) end
        local model = Scene.getNode("model")
        if model then model:setRotation(0, time*30, 0) end

    elseif scene == 2 then
        for i = 1, 5 do
            local b = Scene.getNode("ball"..i)
            if b then
                local phase = i * 1.2
                local y = math.sin(time * 2.5 + phase) * 0.8
                b:setPosition((i-3)*2.2, y, 0)
                b:setScale(1.0, 1.0 - math.abs(math.sin(time*2.5+phase))*0.2, 1.0)
                -- Pulse color with music beat (every ~0.5s)
                local beat = math.sin(time * math.pi * 2) * 0.5 + 0.5
                local base = {
                    {1.0,0.3,0.3},{0.3,0.9,0.3},{0.3,0.5,1.0},
                    {1.0,0.8,0.1},{0.9,0.3,0.9}
                }
                local c = base[i]
                b:setColor(c[1]*(0.6+beat*0.4), c[2]*(0.6+beat*0.4), c[3]*(0.6+beat*0.4))
            end
        end

    elseif scene == 3 then
        local sun = Scene.getNode("sun")
        if sun then sun:setRotation(0, time*15, 0) end
        local p1 = Scene.getNode("p1")
        if p1 then
            p1:setPosition(math.cos(time*0.7)*4.0, 0, math.sin(time*0.7)*4.0)
            p1:setRotation(23, time*60, 0)
        end
        local m1 = Scene.getNode("m1")
        if m1 and p1 then
            local px = math.cos(time*0.7)*4.0
            local pz = math.sin(time*0.7)*4.0
            m1:setPosition(px+math.cos(time*2.5)*0.9, 0, pz+math.sin(time*2.5)*0.9)
        end
        local p2 = Scene.getNode("p2")
        if p2 then p2:setPosition(math.cos(time*0.42+1.2)*6.5, 0, math.sin(time*0.42+1.2)*6.5) end
        local p3 = Scene.getNode("p3")
        if p3 then
            p3:setPosition(math.cos(time*0.22+2.5)*9.0, 0, math.sin(time*0.22+2.5)*9.0)
            p3:setRotation(10, time*30, 0)
            local p3x = math.cos(time*0.22+2.5)*9.0
            local p3z = math.sin(time*0.22+2.5)*9.0
            for i = 1, 6 do
                local ring = Scene.getNode("ring"..i)
                if ring then
                    local angle = math.pi*2*(i/6)+time*0.8
                    ring:setPosition(p3x+math.cos(angle)*1.0, math.sin(angle*0.5)*0.15, p3z+math.sin(angle)*1.0)
                    ring:setRotation(0, math.deg(angle), 0)
                end
            end
        end

    elseif scene == 4 then
        local baseY = math.sin(time*0.5)*0.15
        local base = Scene.getNode("base")
        if base then base:setPosition(0, baseY, 0) end
        local dirt = Scene.getNode("dirt")
        if dirt then dirt:setPosition(0, -0.6+baseY, 0) end

        local tp = {{-1.5,1.3,-1.2},{1.2,1.3,0.8},{-0.5,1.3,1.5},{1.8,1.3,-1.5}}
        for i = 1, 4 do
            local leaves = Scene.getNode("leaves"..i)
            if leaves then
                leaves:setPosition(
                    tp[i][1]+math.sin(time*1.2+i)*0.05,
                    tp[i][2]+baseY,
                    tp[i][3]+math.cos(time+i)*0.05)
            end
        end

        local orbits = {{3.5,0.4,0.0,0.8},{3.2,0.3,2.1,1.5},{3.8,0.5,4.2,2.2}}
        for i, o in ipairs(orbits) do
            local mini = Scene.getNode("mini"..i)
            local crystal = Scene.getNode("crystal"..i)
            if mini then
                local mx = math.cos(time*o[2]+o[3])*o[1]
                local mz = math.sin(time*o[2]+o[3])*o[1]
                local my = o[4]+math.sin(time*0.7+i)*0.3
                mini:setPosition(mx, my, mz)
                if crystal then
                    local pulse = 0.22+math.sin(time*3+i)*0.06
                    crystal:setPosition(mx, my+0.45, mz)
                    crystal:setScale(pulse, pulse, pulse)
                    crystal:setColor(
                        0.4+math.sin(time*0.5+i)*0.4,
                        0.6+math.sin(time*0.5+i+2.1)*0.3,
                        0.8+math.sin(time*0.5+i+4.2)*0.2)
                end
            end
        end

        for h = 1, 5 do
            local b = Scene.getNode("tb"..h)
            if b then
                b:setRotation(0, time*(30+h*15), 0)
                b:setPosition(0, 0.25+(h-1)*0.85+baseY, 0)
            end
        end
    end
end

function onResize(w, h)
    updateCamera()
end

function onPause()
    Audio.pauseMusic()
end

function onResume()
    Audio.resumeMusic()
    updateCamera()
end

print("main.lua loaded — tap to cycle scenes, drag to orbit")
