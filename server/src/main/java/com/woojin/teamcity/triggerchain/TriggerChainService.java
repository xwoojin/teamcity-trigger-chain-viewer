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
 *
 * Supports multi-build AND triggers: when a build watches multiple upstream builds,
 * it is shown only once in the tree with a Condition label listing all requirements.
 *
 * Supports agent mode annotation from Finish Build Trigger (Plus):
 *   "all"  = trigger runs on all enabled compatible agents
 *   "same" = trigger runs on the same agent that ran the watched build
 */
public class TriggerChainService {

    // Built-in TeamCity Finish Build Trigger
    private static final String FINISH_BUILD_TRIGGER_TYPE = "buildDependencyTrigger";
    private static final String DEPENDS_ON_PROPERTY = "dependsOn";

    // Finish Build Trigger (Plus) plugin
    private static final String FINISH_BUILD_TRIGGER_PLUS_TYPE = "FinishBuildTriggerPlus";
    private static final String WATCHED_BUILD_TYPE_ID_PROPERTY = "watchedBuildTypeId";
    private static final String TRIGGER_ON_ALL_AGENTS_PROPERTY = "triggerBuildOnAllCompatibleAgents";
    private static final String TRIGGER_ON_SAME_AGENT_PROPERTY = "triggerOnSameAgent";

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
        Map<String, List<String>> andReqMap = buildAndRequirementsMap();
        Map<String, String> agentModeMap = buildAgentModeMap();

        TriggerChainNode root = createNode(buildType);
        Set<String> visited = new HashSet<>();
        visited.add(buildType.getBuildTypeId());
        buildDownstreamWithMap(buildType, root, visited, reverseMap, andReqMap, agentModeMap);
        return root;
    }

    /**
     * Builds downstream trigger chain trees for all build configurations
     * within the given project (including sub-projects).
     * Only returns trees that have at least one downstream trigger.
     * Trees whose root already appears as a descendant of another tree are excluded.
     */
    @NotNull
    public List<TriggerChainNode> buildProjectTrees(@NotNull SProject project) {
        Map<String, List<SBuildType>> reverseMap = buildReverseTriggersMap();
        Map<String, List<String>> andReqMap = buildAndRequirementsMap();
        Map<String, String> agentModeMap = buildAgentModeMap();
        List<TriggerChainNode> trees = new ArrayList<>();

        addProjectTrees(project, reverseMap, andReqMap, agentModeMap, trees);

        // Deduplicate: remove trees whose root is already shown in another tree
        Set<String> nonRootIds = new HashSet<>();
        for (TriggerChainNode root : trees) {
            collectDescendantIds(root, nonRootIds);
        }
        trees.removeIf(root -> nonRootIds.contains(root.getBuildTypeId()));

        return trees;
    }

    /**
     * Returns direct downstream builds only (one level deep).
     * Used by the "Trigger Usage" tab to show builds that directly watch the given build type.
     */
    @NotNull
    public List<TriggerChainNode> buildDirectDownstream(@NotNull SBuildType buildType) {
        Map<String, List<SBuildType>> reverseMap = buildReverseTriggersMap();
        Map<String, List<String>> andReqMap = buildAndRequirementsMap();
        Map<String, String> agentModeMap = buildAgentModeMap();

        Set<SBuildType> downstreamSet = new LinkedHashSet<>();
        downstreamSet.addAll(reverseMap.getOrDefault(buildType.getBuildTypeId(), Collections.emptyList()));
        downstreamSet.addAll(reverseMap.getOrDefault(buildType.getExternalId(), Collections.emptyList()));

        List<SBuildType> downstream = new ArrayList<>(downstreamSet);
        downstream.sort(Comparator.comparing((SBuildType bt) -> bt.getProject().getName())
                .thenComparing(SBuildType::getName));

        List<TriggerChainNode> nodes = new ArrayList<>();
        for (SBuildType child : downstream) {
            TriggerChainNode childNode = createNode(child);
            List<String> andReqs = andReqMap.get(child.getBuildTypeId());
            if (andReqs != null) childNode.setAndRequirements(andReqs);
            String agentMode = agentModeMap.get(child.getBuildTypeId());
            if (agentMode != null) childNode.setAgentMode(agentMode);
            nodes.add(childNode);
        }
        return nodes;
    }

    private void collectDescendantIds(@NotNull TriggerChainNode node,
                                       @NotNull Set<String> ids) {
        for (TriggerChainNode child : node.getChildren()) {
            ids.add(child.getBuildTypeId());
            collectDescendantIds(child, ids);
        }
    }

    private void addProjectTrees(@NotNull SProject project,
                                  @NotNull Map<String, List<SBuildType>> reverseMap,
                                  @NotNull Map<String, List<String>> andReqMap,
                                  @NotNull Map<String, String> agentModeMap,
                                  @NotNull List<TriggerChainNode> trees) {
        for (SBuildType buildType : project.getOwnBuildTypes()) {
            TriggerChainNode root = createNode(buildType);
            Set<String> visited = new HashSet<>();
            visited.add(buildType.getBuildTypeId());
            buildDownstreamWithMap(buildType, root, visited, reverseMap, andReqMap, agentModeMap);
            if (root.hasChildren()) {
                trees.add(root);
            }
        }
        for (SProject subProject : project.getOwnProjects()) {
            addProjectTrees(subProject, reverseMap, andReqMap, agentModeMap, trees);
        }
    }

    /**
     * Builds a reverse lookup map: for each build type ID, which build types
     * have a finish build trigger depending on it.
     * For multi-build AND triggers, each watched ID maps to the downstream build.
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
                    String[] ids = dependsOn.split(",");
                    for (String id : ids) {
                        String trimmed = id.trim();
                        if (!trimmed.isEmpty()) {
                            reverseMap.computeIfAbsent(trimmed, k -> new ArrayList<>()).add(bt);
                        }
                    }
                }
            }
        }

        return reverseMap;
    }

    /**
     * Builds a map of Condition (multi-watch) trigger requirements:
     * key = downstream build type ID (internal), value = list of upstream build names.
     * Only populated for multi-build triggers (2+ watched builds).
     */
    @NotNull
    private Map<String, List<String>> buildAndRequirementsMap() {
        Map<String, List<String>> andReqMap = new HashMap<>();

        for (SBuildType bt : projectManager.getAllBuildTypes()) {
            for (BuildTriggerDescriptor trigger : bt.getBuildTriggersCollection()) {
                if (!FINISH_BUILD_TRIGGER_PLUS_TYPE.equals(trigger.getTriggerName())) continue;

                String watchedIds = trigger.getProperties().get(WATCHED_BUILD_TYPE_ID_PROPERTY);
                if (watchedIds == null || watchedIds.isEmpty()) continue;

                String[] ids = watchedIds.split(",");
                if (ids.length < 2) continue; // Not a multi-build trigger

                // Resolve each watched ID and silently drop any that refer to
                // deleted / non-existent build configurations — these shouldn't
                // surface in the viewer.
                List<String> names = new ArrayList<>();
                for (String id : ids) {
                    String trimmed = id.trim();
                    if (trimmed.isEmpty()) continue;
                    SBuildType upstream = projectManager.findBuildTypeByExternalId(trimmed);
                    if (upstream == null) upstream = projectManager.findBuildTypeById(trimmed);
                    if (upstream == null) continue; // skip deleted / unresolvable
                    names.add(upstream.getName());
                }

                // Only label as a multi-watch Condition when 2+ valid watches remain
                if (names.size() >= 2) {
                    andReqMap.put(bt.getBuildTypeId(), names);
                }
            }
        }

        return andReqMap;
    }

    /**
     * Builds a map of agent mode per downstream build type:
     * key = downstream build type ID (internal), value = "all" | "same".
     * Only populated from Finish Build Trigger (Plus) triggers that have the option set.
     */
    @NotNull
    private Map<String, String> buildAgentModeMap() {
        Map<String, String> agentModeMap = new HashMap<>();

        for (SBuildType bt : projectManager.getAllBuildTypes()) {
            for (BuildTriggerDescriptor trigger : bt.getBuildTriggersCollection()) {
                if (!FINISH_BUILD_TRIGGER_PLUS_TYPE.equals(trigger.getTriggerName())) continue;

                Map<String, String> props = trigger.getProperties();
                if ("true".equals(props.get(TRIGGER_ON_ALL_AGENTS_PROPERTY))) {
                    agentModeMap.put(bt.getBuildTypeId(), "all");
                } else if ("true".equals(props.get(TRIGGER_ON_SAME_AGENT_PROPERTY))) {
                    agentModeMap.put(bt.getBuildTypeId(), "same");
                }
            }
        }

        return agentModeMap;
    }

    private void buildDownstreamWithMap(@NotNull SBuildType buildType,
                                         @NotNull TriggerChainNode node,
                                         @NotNull Set<String> visited,
                                         @NotNull Map<String, List<SBuildType>> reverseMap,
                                         @NotNull Map<String, List<String>> andReqMap,
                                         @NotNull Map<String, String> agentModeMap) {
        // Look up by both Internal ID (built-in trigger) and External ID (Plus plugin)
        Set<SBuildType> downstreamSet = new LinkedHashSet<>();
        downstreamSet.addAll(reverseMap.getOrDefault(buildType.getBuildTypeId(), Collections.emptyList()));
        downstreamSet.addAll(reverseMap.getOrDefault(buildType.getExternalId(), Collections.emptyList()));
        List<SBuildType> downstream = new ArrayList<>(downstreamSet);

        downstream.sort(Comparator.comparing((SBuildType bt) -> bt.getProject().getName())
                .thenComparing(SBuildType::getName));

        for (SBuildType child : downstream) {
            if (visited.contains(child.getBuildTypeId())) {
                // Condition (multi-watch) target already shown elsewhere — skip silently
                if (andReqMap.containsKey(child.getBuildTypeId())) {
                    continue;
                }
                // Genuine circular reference
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

            List<String> andReqs = andReqMap.get(child.getBuildTypeId());
            if (andReqs != null) childNode.setAndRequirements(andReqs);

            String agentMode = agentModeMap.get(child.getBuildTypeId());
            if (agentMode != null) childNode.setAgentMode(agentMode);

            node.addChild(childNode);
            buildDownstreamWithMap(child, childNode, visited, reverseMap, andReqMap, agentModeMap);
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
