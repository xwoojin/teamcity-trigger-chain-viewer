<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Build status indicator for a trigger chain node. Expects: statusNode --%>

<c:choose>
    <c:when test="${statusNode.buildStatus == 'running'}">
        <span class="trigger-chain-status trigger-chain-status-running">
            <span class="trigger-chain-status-icon">&#9654;</span>
            <span class="trigger-chain-progress-bar">
                <span class="trigger-chain-progress-fill" style="width: ${statusNode.buildProgress}%"></span>
            </span>
            <a href="${statusNode.buildUrl}" class="trigger-chain-status-link">${fn:escapeXml(statusNode.buildNumber)} ${statusNode.buildProgress}%</a>
        </span>
    </c:when>
    <c:when test="${statusNode.buildStatus == 'queued'}">
        <span class="trigger-chain-status trigger-chain-status-queued">
            <span class="trigger-chain-status-icon">&#9203;</span> Queued
        </span>
    </c:when>
    <c:when test="${statusNode.buildStatus == 'success'}">
        <span class="trigger-chain-status trigger-chain-status-success">
            <a href="${statusNode.buildUrl}" class="trigger-chain-status-link">${fn:escapeXml(statusNode.buildNumber)}</a>
        </span>
    </c:when>
    <c:when test="${statusNode.buildStatus == 'failure'}">
        <span class="trigger-chain-status trigger-chain-status-failure">
            <a href="${statusNode.buildUrl}" class="trigger-chain-status-link">${fn:escapeXml(statusNode.buildNumber)}</a>
        </span>
    </c:when>
    <c:when test="${statusNode.buildStatus == 'pending'}">
        <span class="trigger-chain-status trigger-chain-status-pending">Pending</span>
    </c:when>
    <c:when test="${statusNode.buildStatus == 'idle'}">
        <span class="trigger-chain-status trigger-chain-status-idle">Idle</span>
    </c:when>
</c:choose>
