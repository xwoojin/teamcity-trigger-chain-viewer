package com.woojin.teamcity.triggerchain;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Adds a "Trigger Usage" tab to each build configuration page.
 * Shows the build configurations that have a Finish Build Trigger watching THIS build.
 * One level deep (direct usages only) — use the Trigger Chain tab for the full recursive view.
 */
public class TriggerUsageTab extends BuildTypeTab {

    private static final String TAB_ID = "triggerUsageView";
    private static final String TAB_TITLE = "Trigger Usage";
    private static final String JSP_PAGE = "triggerUsageTab.jsp";

    private final TriggerChainService triggerChainService;

    public TriggerUsageTab(@NotNull WebControllerManager controllerManager,
                           @NotNull ProjectManager projectManager,
                           @NotNull PluginDescriptor pluginDescriptor,
                           @NotNull TriggerChainService triggerChainService) {
        super(TAB_ID, TAB_TITLE, controllerManager, projectManager,
                pluginDescriptor.getPluginResourcesPath(JSP_PAGE));
        this.triggerChainService = triggerChainService;
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/triggerChain.css"));
        addJsFile(pluginDescriptor.getPluginResourcesPath("js/triggerChain.js"));
        register();
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> model,
                             @NotNull HttpServletRequest request,
                             @NotNull SBuildType buildType,
                             @NotNull SUser user) {
        List<TriggerChainNode> usages = triggerChainService.buildDirectDownstream(buildType);
        model.put("triggerUsages", usages);
        model.put("hasUsages", !usages.isEmpty());
        model.put("usageCount", usages.size());
        model.put("currentBuildTypeName", buildType.getName());
    }
}
