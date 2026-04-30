--[[
  Lua3D Engine — Demo: Neon Solar System
  Spinning planets, orbital rings, pulsing colors, touch to orbit.
--]]

local nodes   = {}   -- all scene nodes
local time    = 0.0

-- Camera orbit
local camYaw   = 30.0
local camPitch = 28.0
local camDist  = 18.0

local function updateCamera()
    local yr = math.rad(camYaw)
    local pr = math.rad(camPitch)
    Camera.setPosition(
        camDist * math.cos(pr) * math.sin(yr),
        camDist * math.sin(pr),
        camDist * math.cos(pr) * math.cos(yr)
    )
    Camera.setTarget(0, 0, 0)
end

-- Helper: create a ring of small cubes around the origin
local function createRing(name, radius, count, r, g, b, y)
    for i = 1, count do
        local angle = (i / count) * math.pi * 2
        local n = Scene.createNode(name .. "_" .. i)
        n:setMesh(Mesh.cube())
        n:setColor(r, g, b)
        n:setScale(0.08, 0.08, 0.08)
        n:setPosition(
            math.cos(angle) * radius,
            y or 0,
            math.sin(angle) * radius
        )
        table.insert(nodes, { node=n, ring=name, angle=angle,
                               radius=radius, y=y or 0,
                               r=r, g=g, b=b })
    end
end

function onStart()
    Camera.setFov(55)
    updateCamera()

    -- Dark floor grid
    local floor = Scene.createNode("floor")
    floor:setMesh(Mesh.plane(40))
    floor:setColor(0.04, 0.04, 0.08)
    floor:setPosition(0, -2.2, 0)
    table.insert(nodes, { node=floor, kind="floor" })

    -- SUN — big glowing sphere
    local sun = Scene.createNode("sun")
    sun:setMesh(Mesh.sphere(1.4, 24, 24))
    sun:setColor(1.0, 0.9, 0.1)
    table.insert(nodes, { node=sun, kind="sun" })

    -- MERCURY
    local mercury = Scene.createNode("mercury")
    mercury:setMesh(Mesh.sphere(0.18, 14, 14))
    mercury:setColor(0.72, 0.65, 0.55)
    table.insert(nodes, { node=mercury, kind="planet",
        orbit=2.8, speed=1.6, tilt=0, spin=80, color={0.72,0.65,0.55} })

    -- VENUS
    local venus = Scene.createNode("venus")
    venus:setMesh(Mesh.sphere(0.28, 16, 16))
    venus:setColor(0.95, 0.75, 0.3)
    table.insert(nodes, { node=venus, kind="planet",
        orbit=4.0, speed=1.17, tilt=3, spin=30, color={0.95,0.75,0.3} })

    -- EARTH
    local earth = Scene.createNode("earth")
    earth:setMesh(Mesh.sphere(0.32, 18, 18))
    earth:setColor(0.2, 0.55, 0.95)
    table.insert(nodes, { node=earth, kind="earth",
        orbit=5.5, speed=0.9, tilt=23.5, spin=60, color={0.2,0.55,0.95} })

    -- MOON
    local moon = Scene.createNode("moon")
    moon:setMesh(Mesh.sphere(0.10, 12, 12))
    moon:setColor(0.75, 0.75, 0.72)
    table.insert(nodes, { node=moon, kind="moon",
        orbit=0.85, speed=3.2, color={0.75,0.75,0.72} })

    -- MARS
    local mars = Scene.createNode("mars")
    mars:setMesh(Mesh.sphere(0.25, 16, 16))
    mars:setColor(0.85, 0.32, 0.12)
    table.insert(nodes, { node=mars, kind="planet",
        orbit=7.2, speed=0.6, tilt=25, spin=55, color={0.85,0.32,0.12} })

    -- SATURN
    local saturn = Scene.createNode("saturn")
    saturn:setMesh(Mesh.sphere(0.55, 20, 20))
    saturn:setColor(0.9, 0.82, 0.55)
    table.insert(nodes, { node=saturn, kind="saturn",
        orbit=10.5, speed=0.35, tilt=27, spin=40, color={0.9,0.82,0.55} })

    -- Saturn's ring (flat plane tilted)
    local sring = Scene.createNode("saturn_ring")
    sring:setMesh(Mesh.plane(2.2))
    sring:setColor(0.8, 0.72, 0.45)
    table.insert(nodes, { node=sring, kind="saturn_ring" })

    -- Asteroid belt: ring of tiny cubes between Mars and Saturn
    createRing("belt", 8.7, 48, 0.55, 0.45, 0.35, 0.0)

    -- Inner neon ring (decoration)
    createRing("neon_inner", 3.2, 32, 0.1, 0.8, 1.0, 0.05)

    -- Outer neon ring
    createRing("neon_outer", 12.2, 60, 0.6, 0.1, 0.9, -0.05)

    Log.info("Neon Solar System demo started!")
end

function onUpdate(dt)
    time = time + dt

    -- Pulsing sun color (yellow <-> orange)
    local pulse = (math.sin(time * 2.0) + 1.0) * 0.5
    local sunNode = Scene.getNode("sun")
    if sunNode then
        sunNode:setColor(1.0, 0.7 + pulse * 0.25, 0.05 + pulse * 0.1)
        sunNode:setRotation(0, time * 12, 0)
    end

    -- Update each tracked node
    local earthX, earthZ = 0, 0

    for _, e in ipairs(nodes) do
        local n = e.node

        if e.kind == "planet" then
            local px = math.cos(time * e.speed) * e.orbit
            local pz = math.sin(time * e.speed) * e.orbit
            n:setPosition(px, 0, pz)
            n:setRotation(e.tilt, time * e.spin, 0)

        elseif e.kind == "earth" then
            earthX = math.cos(time * e.speed) * e.orbit
            earthZ = math.sin(time * e.speed) * e.orbit
            n:setPosition(earthX, 0, earthZ)
            n:setRotation(e.tilt, time * e.spin, 0)
            -- Subtle color pulse: ocean shimmer
            local shimmer = (math.sin(time * 3.1) + 1) * 0.5
            n:setColor(0.15 + shimmer*0.1, 0.5 + shimmer*0.05, 0.9 + shimmer*0.05)

        elseif e.kind == "moon" then
            local mx = earthX + math.cos(time * e.speed) * e.orbit
            local mz = earthZ + math.sin(time * e.speed) * e.orbit
            n:setPosition(mx, 0, mz)

        elseif e.kind == "saturn" then
            local sx = math.cos(time * e.speed) * e.orbit
            local sz = math.sin(time * e.speed) * e.orbit
            n:setPosition(sx, 0, sz)
            n:setRotation(e.tilt, time * e.spin, 0)
            -- Update ring to follow saturn
            local sring = Scene.getNode("saturn_ring")
            if sring then
                sring:setPosition(sx, 0, sz)
                sring:setRotation(e.tilt + 60, time * e.spin * 0.8, 0)
            end

        elseif e.ring == "belt" then
            -- Asteroid belt slowly rotates
            local newAngle = e.angle + time * 0.08
            n:setPosition(
                math.cos(newAngle) * e.radius,
                math.sin(newAngle * 3.0) * 0.08,  -- gentle bob
                math.sin(newAngle) * e.radius
            )
            n:setRotation(time * 30, time * 20, 0)

        elseif e.ring == "neon_inner" then
            -- Inner neon ring: pulsing cyan, counter-rotate
            local newAngle = e.angle - time * 0.5
            local glow = (math.sin(time * 4.0 + e.angle) + 1) * 0.5
            n:setPosition(
                math.cos(newAngle) * e.radius,
                e.y,
                math.sin(newAngle) * e.radius
            )
            n:setColor(0.0 + glow * 0.2, 0.7 + glow * 0.3, 1.0)
            n:setScale(0.07 + glow*0.05, 0.07 + glow*0.05, 0.07 + glow*0.05)

        elseif e.ring == "neon_outer" then
            -- Outer neon ring: pulsing purple
            local newAngle = e.angle + time * 0.25
            local glow = (math.sin(time * 2.5 + e.angle * 2) + 1) * 0.5
            n:setPosition(
                math.cos(newAngle) * e.radius,
                e.y,
                math.sin(newAngle) * e.radius
            )
            n:setColor(0.5 + glow * 0.4, 0.05, 0.8 + glow * 0.2)
            n:setScale(0.06 + glow*0.04, 0.06 + glow*0.04, 0.06 + glow*0.04)
        end
    end

    -- Touch drag to orbit camera
    if Input.isTouching() then
        local dx, dy = Input.getSwipeDelta()
        camYaw   = camYaw   + dx * 0.25
        camPitch = camPitch - dy * 0.25
        if camPitch >  80 then camPitch =  80 end
        if camPitch < -15 then camPitch = -15 end
        updateCamera()
    end
end

function onResize(w, h)
    Log.info("Resize: " .. w .. "x" .. h)
end

function onPause()  Log.info("Paused")  end
function onResume() Log.info("Resumed") end

print("Neon Solar System loaded!")
