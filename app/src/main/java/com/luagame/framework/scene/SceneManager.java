package com.luagame.framework.scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the scene graph.
 * Nodes are stored in a flat registry by name for O(1) Lua lookup,
 * while a root node owns the hierarchy.
 */
public class SceneManager {

    private final SceneNode root = new SceneNode("__root__");
    private final Map<String, SceneNode> registry = new HashMap<>();

    // Flat list rebuilt each frame for the renderer
    private final List<SceneNode> visibleNodes = new ArrayList<>();

    // ─── Node management ─────────────────────────────────────────────────────

    public SceneNode createNode(String name) {
        SceneNode node = new SceneNode(name);
        root.addChild(node);
        registry.put(name, node);
        return node;
    }

    public SceneNode createNode(String name, SceneNode parent) {
        SceneNode node = new SceneNode(name);
        parent.addChild(node);
        registry.put(name, node);
        return node;
    }

    public void removeNode(String name) {
        SceneNode node = registry.remove(name);
        if (node != null) {
            SceneNode p = findParent(node, root);
            if (p != null) p.removeChild(node);
        }
    }

    public SceneNode getNode(String name) {
        return registry.get(name);
    }

    public SceneNode getRoot() { return root; }

    // ─── Update / query ──────────────────────────────────────────────────────

    public void update(float dt) {
        // Rebuild visible list (frustum culling can go here later)
        visibleNodes.clear();
        collectVisible(root, visibleNodes);
    }

    public List<SceneNode> getVisibleNodes() {
        return visibleNodes;
    }

    private void collectVisible(SceneNode node, List<SceneNode> out) {
        if (node.getMesh() != null) out.add(node);
        for (SceneNode child : node.getChildren()) collectVisible(child, out);
    }

    private SceneNode findParent(SceneNode target, SceneNode candidate) {
        for (SceneNode child : candidate.getChildren()) {
            if (child == target) return candidate;
            SceneNode found = findParent(target, child);
            if (found != null) return found;
        }
        return null;
    }
}
