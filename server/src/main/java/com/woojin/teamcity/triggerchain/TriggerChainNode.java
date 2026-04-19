package com.woojin.teamcity.triggerchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a node in the trigger chain tree.
 *
 * A node is normally a single build configuration (isGroup() == false).
 *
 * When multiple sibling builds are all co-watched by downstream builds via
 * an identical AND-condition, they are bundled into a "group" pseudo-node
 * (isGroup() == true). The group's members are the co-watched upstream builds;
 * the group's children are the shared downstream builds that watch all members.
 *
 * Usage-tab groups use the same pseudo-node: members are downstream builds that
 * share an identical AND-condition, and andRequirements holds the shared
 * condition names for display on the group header.
 */
public class TriggerChainNode {
    private final String buildTypeId;
    private final String buildTypeName;
    private final String projectName;
    private final String buildTypeUrl;
    private List<TriggerChainNode> children;

    // Condition (multi-watch) requirements — display names, e.g. ["Build C", "Build B"]
    private List<String> andRequirements;
    // Condition (multi-watch) requirements — internal build type IDs, used for identity matching
    private List<String> andRequirementIds;

    // Agent mode from the Finish Build Trigger (Plus) trigger that points at this node.
    // Values: "all" (run on all compatible agents), "same" (same agent as watched build), or null.
    private String agentMode;

    // Group pseudo-node fields
    private boolean group;
    private List<TriggerChainNode> groupMembers;

    public TriggerChainNode(String buildTypeId, String buildTypeName, String projectName, String buildTypeUrl) {
        this.buildTypeId = buildTypeId;
        this.buildTypeName = buildTypeName;
        this.projectName = projectName;
        this.buildTypeUrl = buildTypeUrl;
        this.children = new ArrayList<>();
    }

    /**
     * Creates a "chain" group pseudo-node that bundles co-watched upstream builds
     * (members) and the shared downstream builds that watch all of them (sharedChildren).
     */
    public static TriggerChainNode createChainGroup(List<TriggerChainNode> members,
                                                    List<TriggerChainNode> sharedChildren) {
        TriggerChainNode g = new TriggerChainNode("", "", "", "");
        g.group = true;
        g.groupMembers = new ArrayList<>(members);
        g.children = new ArrayList<>(sharedChildren);
        return g;
    }

    /**
     * Creates a "usage" group pseudo-node that bundles downstream builds sharing
     * the same AND-condition. The shared condition names are stored in andRequirements
     * for display on the group header.
     */
    public static TriggerChainNode createUsageGroup(List<TriggerChainNode> members,
                                                    List<String> sharedConditionNames) {
        TriggerChainNode g = new TriggerChainNode("", "", "", "");
        g.group = true;
        g.groupMembers = new ArrayList<>(members);
        g.children = Collections.emptyList();
        if (sharedConditionNames != null) {
            g.andRequirements = new ArrayList<>(sharedConditionNames);
        }
        return g;
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

    public void setChildren(List<TriggerChainNode> children) {
        this.children = children;
    }

    public void addChild(TriggerChainNode child) {
        children.add(child);
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public int getTotalDescendants() {
        int count = 0;
        if (group && groupMembers != null) count += groupMembers.size();
        for (TriggerChainNode child : children) {
            count += (child.isGroup() ? 0 : 1) + child.getTotalDescendants();
        }
        return count;
    }

    // Group pseudo-node

    public boolean isGroup() {
        return group;
    }

    public List<TriggerChainNode> getGroupMembers() {
        return groupMembers;
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

    public void clearAndRequirements() {
        this.andRequirements = null;
        this.andRequirementIds = null;
    }

    public List<String> getAndRequirementIds() {
        return andRequirementIds;
    }

    public void setAndRequirementIds(List<String> andRequirementIds) {
        this.andRequirementIds = andRequirementIds;
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
