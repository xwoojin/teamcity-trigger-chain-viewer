<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--
  Trigger Usage View - Build-level.
  Shows which other build configurations have a Finish Build Trigger
  watching THIS build (direct usages only).
--%>

<div class="trigger-chain-container trigger-usage-container">
    <c:choose>
        <c:when test="${hasUsages}">
            <div class="trigger-chain-header">
                <h3 class="trigger-chain-title">Used As Trigger By</h3>
                <span class="trigger-chain-badge">${usageCount} build<c:if test="${usageCount > 1}">s</c:if></span>
            </div>
            <p class="trigger-usage-desc">
                The following build configuration<c:if test="${usageCount > 1}">s</c:if>
                <c:choose><c:when test="${usageCount > 1}">have</c:when><c:otherwise>has</c:otherwise></c:choose>
                a Finish Build Trigger that watches <strong>${fn:escapeXml(currentBuildTypeName)}</strong>.
            </p>

            <ul class="trigger-chain-list trigger-usage-list" data-depth="0">
                <c:forEach var="node" items="${triggerUsages}" varStatus="status">
                    <li class="trigger-chain-item ${status.last ? 'last-child' : ''}">
                        <div class="trigger-chain-connector">
                            <span class="trigger-chain-line-vertical"></span>
                            <span class="trigger-chain-line-horizontal"></span>
                        </div>
                        <c:choose>
                            <%-- Group: several downstream builds share an identical AND-condition --%>
                            <c:when test="${node.group}">
                                <div class="trigger-chain-group" title="These builds share a single AND-condition trigger">
                                    <div class="trigger-chain-group-tag">Condition (all must finish):
                                        <c:forEach var="req" items="${node.andRequirements}" varStatus="rs">
                                            <c:if test="${!rs.first}"> + </c:if>${fn:escapeXml(req)}
                                        </c:forEach>
                                    </div>
                                    <c:forEach var="member" items="${node.groupMembers}">
                                        <div class="trigger-chain-node trigger-chain-group-member" data-buildtype-id="${fn:escapeXml(member.buildTypeId)}">
                                            <span class="trigger-chain-leaf-dot">&#9654;</span>
                                            <a href="${member.buildTypeUrl}" class="trigger-chain-node-link">
                                                <span class="trigger-chain-project">${fn:escapeXml(member.projectName)}</span>
                                                <span class="trigger-chain-separator">&nbsp;::&nbsp;</span>
                                                <span class="trigger-chain-buildtype">${fn:escapeXml(member.buildTypeName)}</span>
                                            </a>
                                            <c:if test="${member.hasAgentMode()}">
                                                <c:choose>
                                                    <c:when test="${member.agentMode == 'all'}">
                                                        <span class="trigger-chain-agent-label trigger-chain-agent-all"
                                                              title="Triggered build runs on all enabled compatible agents">All Agents</span>
                                                    </c:when>
                                                    <c:when test="${member.agentMode == 'same'}">
                                                        <span class="trigger-chain-agent-label trigger-chain-agent-same"
                                                              title="Triggered build runs on the same agent as the watched build">Same Agent</span>
                                                    </c:when>
                                                </c:choose>
                                            </c:if>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="trigger-chain-node" data-buildtype-id="${fn:escapeXml(node.buildTypeId)}">
                                    <span class="trigger-chain-leaf-dot">&#9654;</span>
                                    <a href="${node.buildTypeUrl}" class="trigger-chain-node-link">
                                        <span class="trigger-chain-project">${fn:escapeXml(node.projectName)}</span>
                                        <span class="trigger-chain-separator">&nbsp;::&nbsp;</span>
                                        <span class="trigger-chain-buildtype">${fn:escapeXml(node.buildTypeName)}</span>
                                    </a>
                                    <c:if test="${node.hasAndRequirements()}">
                                        <span class="trigger-chain-and-label" title="Triggers when ALL listed builds complete">Condition:
                                            <c:forEach var="req" items="${node.andRequirements}" varStatus="rs">
                                                <c:if test="${!rs.first}"> + </c:if>${fn:escapeXml(req)}
                                            </c:forEach>
                                        </span>
                                    </c:if>
                                    <c:if test="${node.hasAgentMode()}">
                                        <c:choose>
                                            <c:when test="${node.agentMode == 'all'}">
                                                <span class="trigger-chain-agent-label trigger-chain-agent-all"
                                                      title="Triggered build runs on all enabled compatible agents">All Agents</span>
                                            </c:when>
                                            <c:when test="${node.agentMode == 'same'}">
                                                <span class="trigger-chain-agent-label trigger-chain-agent-same"
                                                      title="Triggered build runs on the same agent as the watched build">Same Agent</span>
                                            </c:when>
                                        </c:choose>
                                    </c:if>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </li>
                </c:forEach>
            </ul>
        </c:when>
        <c:otherwise>
            <div class="trigger-chain-empty">
                <div class="trigger-chain-empty-icon">&#128279;</div>
                <p class="trigger-chain-empty-text">No other build configuration currently uses <strong>${fn:escapeXml(currentBuildTypeName)}</strong> as a trigger.</p>
                <p class="trigger-chain-empty-hint">
                    When another build configuration adds a <strong>Finish Build Trigger</strong> (or <strong>Finish Build Trigger (Plus)</strong>) that watches this build, it will appear here.
                </p>
            </div>
        </c:otherwise>
    </c:choose>
</div>
