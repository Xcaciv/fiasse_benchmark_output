<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Admin Dashboard</h1>
</div>

<div class="stats-grid">
    <div class="card stat-card">
        <div class="stat-value"><c:out value="${userCount}"/></div>
        <div class="stat-label">Total Users</div>
    </div>
    <div class="card stat-card">
        <div class="stat-value"><c:out value="${noteCount}"/></div>
        <div class="stat-label">Total Notes</div>
    </div>
</div>

<div class="admin-links">
    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-primary">Manage Users</a>
</div>

<section class="admin-section">
    <h2>Recent Audit Events</h2>

    <c:choose>
        <c:when test="${not empty auditEvents}">
            <table class="data-table audit-table">
                <thead>
                    <tr>
                        <th>Timestamp</th>
                        <th>Event Type</th>
                        <th>Actor</th>
                        <th>IP Address</th>
                        <th>Outcome</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="event" items="${auditEvents}">
                        <tr>
                            <td><c:out value="${event.timestamp}"/></td>
                            <td><c:out value="${event.eventType}"/></td>
                            <td><c:out value="${event.actorUsername}"/></td>
                            <td><c:out value="${event.ipAddress}"/></td>
                            <td>
                                <span class="outcome-badge outcome-${fn:toLowerCase(event.outcome)}">
                                    <c:out value="${event.outcome}"/>
                                </span>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:when>
        <c:otherwise>
            <p class="empty-text">No audit events recorded.</p>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
