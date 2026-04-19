<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Recursive subtree rendering for trigger chain nodes --%>

<c:if test="${not empty nodes}">
    <ul class="trigger-chain-list" data-depth="${depth}">
        <c:forEach var="node" items="${nodes}" varStatus="status">
            <li class="trigger-chain-item ${status.last ? 'last-child' : ''}">
                <div class="trigger-chain-connector">
                    <span class="trigger-chain-line-vertical"></span>
                    <span class="trigger-chain-line-horizontal"></span>
                </div>
                <div class="trigger-chain-node" data-buildtype-id="${fn:escapeXml(node.buildTypeId)}">
                    <c:if test="${node.hasChildren()}">
                        <button type="button" class="trigger-chain-expand-btn" onclick="TriggerChain.toggle(this)" title="Expand/Collapse">
                            <span class="trigger-chain-arrow">&#9654;</span>
                        </button>
                    </c:if>
                    <c:if test="${!node.hasChildren()}">
                        <span class="trigger-chain-leaf-dot">&#9654;</span>
                    </c:if>
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

                <c:if test="${node.hasChildren()}">
                    <c:set var="parentNodes" value="${nodes}" scope="request"/>
                    <c:set var="parentDepth" value="${depth}" scope="request"/>
                    <c:set var="nodes" value="${node.children}" scope="request"/>
                    <c:set var="depth" value="${depth + 1}" scope="request"/>
                    <jsp:include page="triggerChainSubtree.jsp"/>
                    <c:set var="nodes" value="${parentNodes}" scope="request"/>
                    <c:set var="depth" value="${parentDepth}" scope="request"/>
                </c:if>
            </li>
        </c:forEach>
    </ul>
</c:if>
