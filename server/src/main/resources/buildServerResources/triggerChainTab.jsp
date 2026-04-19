<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--
  Trigger Chain View - Displays downstream finish build trigger chain as a tree.
  Each node shows a build configuration that is triggered when the parent finishes.
--%>

<div class="trigger-chain-container">
    <c:choose>
        <c:when test="${hasDownstream}">
            <div class="trigger-chain-header">
                <h3 class="trigger-chain-title">Downstream Trigger Chain</h3>
                <span class="trigger-chain-badge">${totalDownstream} triggered build<c:if test="${totalDownstream > 1}">s</c:if></span>
                <button type="button" class="btn trigger-chain-toggle-all" onclick="TriggerChain.toggleAll()" title="Expand/Collapse All">
                    Expand All
                </button>
            </div>
            <div class="trigger-chain-tree">
                <div class="trigger-chain-root-node">
                    <span class="trigger-chain-node-icon root-icon">&#9654;</span>
                    <a href="${triggerChainRoot.buildTypeUrl}" class="trigger-chain-node-link root-link">
                        <span class="trigger-chain-project">${fn:escapeXml(triggerChainRoot.projectName)}</span>
                        <span class="trigger-chain-separator">&nbsp;::&nbsp;</span>
                        <span class="trigger-chain-buildtype">${fn:escapeXml(triggerChainRoot.buildTypeName)}</span>
                    </a>
                    <span class="trigger-chain-current-tag">current</span>
                </div>

                <c:set var="nodes" value="${triggerChainRoot.children}" scope="request"/>
                <c:set var="depth" value="1" scope="request"/>
                <jsp:include page="triggerChainSubtree.jsp"/>
            </div>
        </c:when>
        <c:otherwise>
            <div class="trigger-chain-empty">
                <div class="trigger-chain-empty-icon">&#128279;</div>
                <p class="trigger-chain-empty-text">No downstream finish build triggers found for this build configuration.</p>
                <p class="trigger-chain-empty-hint">
                    Build configurations that have a <strong>Finish Build Trigger</strong> depending on this build will appear here as a chain.
                </p>
            </div>
        </c:otherwise>
    </c:choose>
</div>
