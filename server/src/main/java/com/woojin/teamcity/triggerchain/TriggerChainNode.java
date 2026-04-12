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

    // Build status fields
    private String buildStatus;   // "success", "failure", "running", "queued", "idle"
    private int buildProgress;    // 0-100, only meaningful when status is "running"
    private String buildNumber;   // e.g. "#123"
    private String buildUrl;      // URL to the specific build run

    public TriggerChainNode(String buildTypeId, String buildTypeName, String projectName, String buildTypeUrl) {
        this.buildTypeId = buildTypeId;
        this.buildTypeName = buildTypeName;
        this.projectName = projectName;
        this.buildTypeUrl = buildTypeUrl;
        this.children = new ArrayList<>();
        this.buildStatus = "idle";
        this.buildProgress = 0;
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

    // Build status getters and setters

    public String getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }

    public int getBuildProgress() {
        return buildProgress;
    }

    public void setBuildProgress(int buildProgress) {
        this.buildProgress = buildProgress;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    /**
     * Returns true if any node in this tree (including this node) has
     * a running or queued build.
     */
    public boolean hasActiveBuilds() {
        if ("running".equals(buildStatus) || "queued".equals(buildStatus) || "pending".equals(buildStatus)) {
            return true;
        }
        for (TriggerChainNode child : children) {
            if (child.hasActiveBuilds()) {
                return true;
            }
        }
        return false;
    }
}
