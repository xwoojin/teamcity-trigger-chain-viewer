package com.woojin.teamcity.triggerchain;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Adds a "Trigger Chain" tab to each build configuration page.
 * The tab shows the downstream finish build trigger chain as a tree.
 */
public class TriggerChainTab extends BuildTypeTab {

    private static final String TAB_ID = "triggerChainView";
    private static final String TAB_TITLE = "Trigger Chain";
    private static final String JSP_PAGE = "triggerChainTab.jsp";

    private final TriggerChainService triggerChainService;
    private final PluginDescriptor pluginDescriptor;

    public TriggerChainTab(@NotNull WebControllerManager controllerManager,
                           @NotNull ProjectManager projectManager,
                           @NotNull PluginDescriptor pluginDescriptor,
                           @NotNull TriggerChainService triggerChainService) {
        super(TAB_ID, TAB_TITLE, controllerManager, projectManager,
                pluginDescriptor.getPluginResourcesPath(JSP_PAGE));
        this.triggerChainService = triggerChainService;
        this.pluginDescriptor = pluginDescriptor;
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/triggerChain.css"));
        addJsFile(pluginDescriptor.getPluginResourcesPath("js/triggerChain.js"));
        register();
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> model,
                             @NotNull HttpServletRequest request,
                             @NotNull SBuildType buildType,
                             @NotNull SUser user) {
        TriggerChainNode root = triggerChainService.buildDownstreamTree(buildType);
        model.put("triggerChainRoot", root);
        model.put("hasDownstream", root.hasChildren());
        model.put("totalDownstream", root.getTotalDescendants());
    }
}
