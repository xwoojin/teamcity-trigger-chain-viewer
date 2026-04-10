package com.woojin.teamcity.triggerchain;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Service that builds trigger chain trees by traversing finish build triggers.
 *
 * For a given build configuration A, finds all build configurations that have
 * a "Finish Build Trigger" depending on A, then recursively finds their
 * downstream triggers to build the full chain.
 */
public class TriggerChainService {

    // Built-in TeamCity Finish Build Trigger
    private static final String FINISH_BUILD_TRIGGER_TYPE = "buildDependencyTrigger";
    private static final String DEPENDS_ON_PROPERTY = "dependsOn";

    // Finish Build Trigger (Plus) plugin
    private static final String FINISH_BUILD_TRIGGER_PLUS_TYPE = "FinishBuildTriggerPlus";
    private static final String WATCHED_BUILD_TYPE_ID_PROPERTY = "watchedBuildTypeId";

    private final ProjectManager projectManager;

    public TriggerChainService(@NotNull ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    /**
     * Builds the downstream trigger chain tree for a given build type.
     * Returns a root node representing the given build type, with children
     * being all build configs that are triggered by it (recursively).
     */
    @NotNull
    public TriggerChainNode buildDownstreamTree(@NotNull SBuildType buildType) {
        Map<String, List<SBuildType>> reverseMap = buildReverseTriggersMap();
        TriggerChainNode root = createNode(buildType);
        Set<String> visited = new HashSet<>();
        visited.add(buildType.getBuildTypeId());
        buildDownstreamWithMap(buildType, root, visited, reverseMap);
        return root;
    }

    /**
     * Builds downstream trigger chain trees for all build configurations
     * within the given project (including sub-projects).
     * Only returns trees that have at least one downstream trigger.
     */
    @NotNull
    public List<TriggerChainNode> buildProjectTrees(@NotNull SProject project) {
        Map<String, List<SBuildType>> reverseMap = buildReverseTriggersMap();
        List<TriggerChainNode> trees = new ArrayList<>();

        addProjectTrees(project, reverseMap, trees);
        return trees;
    }

    private void addProjectTrees(@NotNull SProject project,
                                  @NotNull Map<String, List<SBuildType>> reverseMap,
                                  @NotNull List<TriggerChainNode> trees) {
        for (SBuildType buildType : project.getOwnBuildTypes()) {
            TriggerChainNode root = createNode(buildType);
            Set<String> visited = new HashSet<>();
            visited.add(buildType.getBuildTypeId());
            buildDownstreamWithMap(buildType, root, visited, reverseMap);
            if (root.hasChildren()) {
                trees.add(root);
            }
        }
        for (SProject subProject : project.getOwnProjects()) {
            addProjectTrees(subProject, reverseMap, trees);
        }
    }

    /**
     * Builds a reverse lookup map: for each build type ID, which build types
     * have a finish build trigger depending on it.
     */
    @NotNull
    private Map<String, List<SBuildType>> buildReverseTriggersMap() {
        Map<String, List<SBuildType>> reverseMap = new HashMap<>();

        for (SBuildType bt : projectManager.getAllBuildTypes()) {
            for (BuildTriggerDescriptor trigger : bt.getBuildTriggersCollection()) {
                String triggerName = trigger.getTriggerName();
                String dependsOn = null;

                if (FINISH_BUILD_TRIGGER_TYPE.equals(triggerName)) {
                    // Built-in Finish Build Trigger
                    dependsOn = trigger.getProperties().get(DEPENDS_ON_PROPERTY);
                } else if (FINISH_BUILD_TRIGGER_PLUS_TYPE.equals(triggerName)) {
                    // Finish Build Trigger (Plus) plugin
                    dependsOn = trigger.getProperties().get(WATCHED_BUILD_TYPE_ID_PROPERTY);
                }

                if (dependsOn != null && !dependsOn.isEmpty()) {
                    reverseMap.computeIfAbsent(dependsOn, k -> new ArrayList<>()).add(bt);
                }
            }
        }

        return reverseMap;
    }

    private void buildDownstreamWithMap(@NotNull SBuildType buildType,
                                         @NotNull TriggerChainNode node,
                                         @NotNull Set<String> visited,
                                         @NotNull Map<String, List<SBuildType>> reverseMap) {
        // Look up by both Internal ID (used by built-in trigger) and External ID (used by Plus plugin)
        Set<SBuildType> downstreamSet = new LinkedHashSet<>();
        downstreamSet.addAll(reverseMap.getOrDefault(buildType.getBuildTypeId(), Collections.emptyList()));
        downstreamSet.addAll(reverseMap.getOrDefault(buildType.getExternalId(), Collections.emptyList()));
        List<SBuildType> downstream = new ArrayList<>(downstreamSet);

        // Sort by project name + build type name for consistent display
        downstream.sort(Comparator.comparing((SBuildType bt) -> bt.getProject().getName())
                .thenComparing(SBuildType::getName));

        for (SBuildType child : downstream) {
            if (visited.contains(child.getBuildTypeId())) {
                // Circular reference detected - add node but don't recurse
                TriggerChainNode circularNode = new TriggerChainNode(
                        child.getBuildTypeId(),
                        child.getName() + " (circular ref)",
                        child.getProject().getName(),
                        buildTypeUrl(child)
                );
                node.addChild(circularNode);
                continue;
            }

            visited.add(child.getBuildTypeId());
            TriggerChainNode childNode = createNode(child);
            node.addChild(childNode);
            buildDownstreamWithMap(child, childNode, visited, reverseMap);
        }
    }

    @NotNull
    private TriggerChainNode createNode(@NotNull SBuildType buildType) {
        return new TriggerChainNode(
                buildType.getBuildTypeId(),
                buildType.getName(),
                getProjectFullPath(buildType),
                buildTypeUrl(buildType)
        );
    }

    /**
     * Returns the full project path excluding "&lt;Root project&gt;".
     * e.g. "Release / Client / KR / Android" → "Release :: Client :: KR :: Android"
     */
    @NotNull
    private String getProjectFullPath(@NotNull SBuildType buildType) {
        String fullName = buildType.getProject().getFullName();
        // Remove "<Root project> / " prefix if present
        final String ROOT_PREFIX = "<Root project>";
        if (fullName.startsWith(ROOT_PREFIX)) {
            fullName = fullName.substring(ROOT_PREFIX.length()).trim();
            if (fullName.startsWith("/")) {
                fullName = fullName.substring(1).trim();
            }
        }
        // Replace TeamCity's " / " separator with " :: " for display
        return fullName.replace(" / ", " :: ");
    }

    @NotNull
    private String buildTypeUrl(@NotNull SBuildType buildType) {
        return "/buildConfiguration/" + buildType.getExternalId();
    }
}
