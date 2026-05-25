<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--
  Trigger Chain View - Project level.
  Shows downstream trigger chains for all build configurations in the project.
--%>

<div class="trigger-chain-container">
    <c:choose>
        <c:when test="${hasAnyChains}">
            <div class="trigger-chain-header">
                <h3 class="trigger-chain-title">Downstream Trigger Chains</h3>
                <span class="trigger-chain-badge">${totalRoots} build<c:if test="${totalRoots > 1}">s</c:if> with triggers</span>
                <span class="trigger-chain-badge trigger-chain-badge-secondary">${totalTriggered} total triggered</span>
                <c:if test="${includeDisabled}"><span class="trigger-chain-badge trigger-chain-badge-secondary" title="Disabled triggers are currently included in this view">Disabled triggers included</span></c:if>
                <button type="button" class="btn trigger-chain-toggle-disabled" onclick="TriggerChain.toggleDisabledTriggers(${!includeDisabled})" title="${includeDisabled ? 'Re-hide disabled triggers' : 'Show triggers that are currently disabled'}">
                    <c:choose><c:when test="${includeDisabled}">Hide disabled triggers</c:when><c:otherwise>Show disabled triggers</c:otherwise></c:choose>
                </button>
                <button type="button" class="btn trigger-chain-toggle-all" onclick="TriggerChain.toggleAll()" title="Expand/Collapse All">
                    Expand All
                </button>
            </div>

            <c:forEach var="tree" items="${triggerChainTrees}" varStatus="treeStatus">
                <div class="trigger-chain-tree trigger-chain-project-section">
                    <div class="trigger-chain-root-node">
                        <span class="trigger-chain-node-icon root-icon">&#9654;</span>
                        <a href="${tree.buildTypeUrl}" class="trigger-chain-node-link root-link">
                            <span class="trigger-chain-project">${fn:escapeXml(tree.projectName)}</span>
                            <span class="trigger-chain-separator">&nbsp;::&nbsp;</span>
                            <span class="trigger-chain-buildtype">${fn:escapeXml(tree.buildTypeName)}</span>
                        </a>
                    </div>

                    <c:set var="nodes" value="${tree.children}" scope="request"/>
                    <c:set var="depth" value="1" scope="request"/>
                    <jsp:include page="triggerChainSubtree.jsp"/>
                </div>
            </c:forEach>
        </c:when>
        <c:otherwise>
            <div class="trigger-chain-empty">
                <div class="trigger-chain-empty-icon">&#128279;</div>
                <p class="trigger-chain-empty-text">No downstream finish build triggers found in this project.</p>
                <p class="trigger-chain-empty-hint">
                    Build configurations that have a <strong>Finish Build Trigger</strong> depending on builds in this project will appear here.
                </p>
                <c:if test="${!includeDisabled}">
                    <p class="trigger-chain-empty-hint">
                        <button type="button" class="btn trigger-chain-toggle-disabled" onclick="TriggerChain.toggleDisabledTriggers(true)">Show disabled triggers</button>
                        — there may be triggers that are currently disabled.
                    </p>
                </c:if>
            </div>
        </c:otherwise>
    </c:choose>
</div>
