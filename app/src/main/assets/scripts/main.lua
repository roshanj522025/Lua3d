-- Minimal test: one red cube, camera pulled back
function onStart()
    Log.info("=== onStart called ===")

    Camera.setPosition(0, 3, 8)
    Camera.setTarget(0, 0, 0)
    Camera.setFov(60)

    local box = Scene.createNode("box")
    box:setMesh(Mesh.cube())
    box:setColor(1, 0, 0)
    box:setPosition(0, 0, 0)
    box:setScale(1, 1, 1)

    Log.info("=== onStart: cube created ===")
end

function onUpdate(dt)
    local box = Scene.getNode("box")
    if box then
        box:rotate(0, 45 * dt, 0)
    end
end
