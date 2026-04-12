package com.woojin.teamcity.triggerchain;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SQueuedBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Service that builds trigger chain trees by traversing finish build triggers.
 *
 * For a given build configuration A, finds all build configurations that have
 * a "Finish Build Trigger" depending on A, then recursively finds their
 * downstream triggers to build the full chain.
 *
 * Build status is tracked relative to the current chain execution:
 * the root build's start time is used as a reference point to determine
 * which downstream builds belong to the current chain run.
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
     * Build status is populated relative to the current chain execution.
     */
    @NotNull
    public TriggerChainNode buildDownstreamTree(@NotNull SBuildType buildType) {
        Map<String, List<SBuildType>> reverseMap = buildReverseTriggersMap();
        Date chainStartTime = getChainStartTime(buildType);

        TriggerChainNode root = createNode(buildType);
        populateChainBuildStatus(root, buildType, chainStartTime);

        Set<String> visited = new HashSet<>();
        visited.add(buildType.getBuildTypeId());
        buildDownstreamWithMap(buildType, root, visited, reverseMap, chainStartTime);
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
            Date chainStartTime = getChainStartTime(buildType);
            TriggerChainNode root = createNode(buildType);
            populateChainBuildStatus(root, buildType, chainStartTime);

            Set<String> visited = new HashSet<>();
            visited.add(buildType.getBuildTypeId());
            buildDownstreamWithMap(buildType, root, visited, reverseMap, chainStartTime);
            if (root.hasChildren()) {
                trees.add(root);
            }
        }
        for (SProject subProject : project.getOwnProjects()) {
            addProjectTrees(subProject, reverseMap, trees);
        }
    }

    /**
     * Gets the chain start time from the root build type.
     * This is the start time of the most recent build (running or finished).
     * Returns null if the build type has never been built.
     */
    @Nullable
    private Date getChainStartTime(@NotNull SBuildType rootBuildType) {
        // Running build takes priority
        List<SRunningBuild> running = rootBuildType.getRunningBuilds();
        if (running != null && !running.isEmpty()) {
            return running.get(0).getStartDate();
        }
        // Then last finished build
        SFinishedBuild lastFinished = rootBuildType.getLastChangesFinished();
        if (lastFinished != null) {
            return lastFinished.getStartDate();
        }
        return null;
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
                    dependsOn = trigger.getProperties().get(DEPENDS_ON_PROPERTY);
                } else if (FINISH_BUILD_TRIGGER_PLUS_TYPE.equals(triggerName)) {
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
                                         @NotNull Map<String, List<SBuildType>> reverseMap,
                                         @Nullable Date chainStartTime) {
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
            populateChainBuildStatus(childNode, child, chainStartTime);
            node.addChild(childNode);
            buildDownstreamWithMap(child, childNode, visited, reverseMap, chainStartTime);
        }
    }

    /**
     * Populates build status for a node relative to the current chain execution.
     *
     * Uses chainStartTime (the root build's start time) as reference:
     * - If a build started at or after chainStartTime, it belongs to this chain run
     * - If no such build exists, the node is "pending" (waiting to be triggered)
     * - If chainStartTime is null (root never built), status is "idle"
     */
    private void populateChainBuildStatus(@NotNull TriggerChainNode node,
                                           @NotNull SBuildType buildType,
                                           @Nullable Date chainStartTime) {
        if (chainStartTime == null) {
            node.setBuildStatus("idle");
            return;
        }

        // 1. Check for running builds in this chain
        List<SRunningBuild> runningBuilds = buildType.getRunningBuilds();
        if (runningBuilds != null) {
            for (SRunningBuild running : runningBuilds) {
                Date startDate = running.getStartDate();
                if (startDate != null && !startDate.before(chainStartTime)) {
                    node.setBuildStatus("running");
                    node.setBuildProgress(running.getCompletedPercent());
                    node.setBuildNumber("#" + running.getBuildNumber());
                    node.setBuildUrl("/buildConfiguration/" + buildType.getExternalId() + "/" + running.getBuildId());
                    return;
                }
            }
        }

        // 2. Check for queued builds
        List<SQueuedBuild> queuedBuilds = buildType.getQueuedBuilds(null);
        if (queuedBuilds != null && !queuedBuilds.isEmpty()) {
            node.setBuildStatus("queued");
            return;
        }

        // 3. Check last finished build — only if it started after chain start
        SFinishedBuild lastFinished = buildType.getLastChangesFinished();
        if (lastFinished != null) {
            Date startDate = lastFinished.getStartDate();
            if (startDate != null && !startDate.before(chainStartTime)) {
                boolean success = lastFinished.getBuildStatus().isSuccessful();
                node.setBuildStatus(success ? "success" : "failure");
                node.setBuildNumber("#" + lastFinished.getBuildNumber());
                node.setBuildUrl("/buildConfiguration/" + buildType.getExternalId() + "/" + lastFinished.getBuildId());
                return;
            }
        }

        // 4. No build in this chain run yet — pending
        node.setBuildStatus("pending");
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
     */
    @NotNull
    private String getProjectFullPath(@NotNull SBuildType buildType) {
        String fullName = buildType.getProject().getFullName();
        final String ROOT_PREFIX = "<Root project>";
        if (fullName.startsWith(ROOT_PREFIX)) {
            fullName = fullName.substring(ROOT_PREFIX.length()).trim();
            if (fullName.startsWith("/")) {
                fullName = fullName.substring(1).trim();
            }
        }
        return fullName.replace(" / ", " :: ");
    }

    @NotNull
    private String buildTypeUrl(@NotNull SBuildType buildType) {
        return "/buildConfiguration/" + buildType.getExternalId();
    }
}
