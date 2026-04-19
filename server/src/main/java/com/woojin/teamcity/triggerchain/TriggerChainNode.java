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

    // Condition (multi-watch) requirements (e.g. ["Build C", "Build B", "Build D"])
    private List<String> andRequirements;

    // Agent mode from the Finish Build Trigger (Plus) trigger that points at this node.
    // Values: "all" (run on all compatible agents), "same" (same agent as watched build), or null.
    private String agentMode;

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

    // Condition (multi-watch) requirements

    public List<String> getAndRequirements() {
        return andRequirements;
    }

    public void setAndRequirements(List<String> andRequirements) {
        this.andRequirements = andRequirements;
    }

    public boolean hasAndRequirements() {
        return andRequirements != null && !andRequirements.isEmpty();
    }

    // Agent mode

    public String getAgentMode() {
        return agentMode;
    }

    public void setAgentMode(String agentMode) {
        this.agentMode = agentMode;
    }

    public boolean hasAgentMode() {
        return agentMode != null && !agentMode.isEmpty();
    }
}
