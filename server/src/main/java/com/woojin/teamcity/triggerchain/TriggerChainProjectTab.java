package com.woojin.teamcity.triggerchain;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.project.ProjectTab;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Adds a "Trigger Chain" tab to each project page.
 * Shows downstream trigger chains for all build configurations within the project.
 */
public class TriggerChainProjectTab extends ProjectTab {

    private static final String TAB_ID = "triggerChainProjectView";
    private static final String TAB_TITLE = "Trigger Chain";
    private static final String JSP_PAGE = "triggerChainProjectTab.jsp";

    private final TriggerChainService triggerChainService;

    public TriggerChainProjectTab(@NotNull PagePlaces pagePlaces,
                                   @NotNull ProjectManager projectManager,
                                   @NotNull PluginDescriptor pluginDescriptor,
                                   @NotNull TriggerChainService triggerChainService) {
        super(TAB_ID, TAB_TITLE, pagePlaces, projectManager,
                pluginDescriptor.getPluginResourcesPath(JSP_PAGE));
        this.triggerChainService = triggerChainService;
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/triggerChain.css"));
        addJsFile(pluginDescriptor.getPluginResourcesPath("js/triggerChain.js"));
        register();
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> model,
                             @NotNull HttpServletRequest request,
                             @NotNull SProject project,
                             @NotNull SUser user) {
        List<TriggerChainNode> trees = triggerChainService.buildProjectTrees(project);
        model.put("triggerChainTrees", trees);
        model.put("hasAnyChains", !trees.isEmpty());

        int totalTriggered = 0;
        for (TriggerChainNode tree : trees) {
            totalTriggered += tree.getTotalDescendants();
        }
        model.put("totalTriggered", totalTriggered);
        model.put("totalRoots", trees.size());
    }
}
