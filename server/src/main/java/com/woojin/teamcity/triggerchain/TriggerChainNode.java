package com.woojin.teamcity.triggerchain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the trigger chain tree.
 * Each node is a build configuration that may trigger downstream builds.
 */
public class TriggerChainNode {
    private final String buildTypeId;
    private final String buildTypeName;
    private final String projectName;
    private final String buildTypeUrl;
    private final List<TriggerChainNode> children;

    public TriggerChainNode(String buildTypeId, String buildTypeName, String projectName, String buildTypeUrl) {
        this.buildTypeId = buildTypeId;
        this.buildTypeName = buildTypeName;
        this.projectName = projectName;
        this.buildTypeUrl = buildTypeUrl;
        this.children = new ArrayList<>();
    }

    public String getBuildTypeId() {
        return buildTypeId;
    }

    public String getBuildTypeName() {
        return buildTypeName;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getBuildTypeUrl() {
        return buildTypeUrl;
    }

    public List<TriggerChainNode> getChildren() {
        return children;
    }

    public void addChild(TriggerChainNode child) {
        children.add(child);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public int getTotalDescendants() {
        int count = 0;
        for (TriggerChainNode child : children) {
            count += 1 + child.getTotalDescendants();
        }
        return count;
    }
}
