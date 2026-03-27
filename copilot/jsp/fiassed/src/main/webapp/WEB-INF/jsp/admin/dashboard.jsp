<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Admin Dashboard - Loose Notes"/>
</jsp:include>
<div class="admin-page">
    <h2>Admin Dashboard</h2>
    <div class="stats-grid">
        <div class="stat-card">
            <h3>Total Users</h3>
            <p class="stat-number">${totalUsers}</p>
            <a href="${pageContext.request.contextPath}/admin/users">Manage Users</a>
        </div>
        <div class="stat-card">
            <h3>Total Notes</h3>
            <p class="stat-number">${totalNotes}</p>
        </div>
    </div>
    <div class="audit-log">
        <h3>Recent Audit Events</h3>
        <table class="table">
            <thead>
                <tr><th>Time</th><th>Event</th><th>Actor</th><th>Target</th><th>IP</th><th>Outcome</th></tr>
            </thead>
            <tbody>
                <c:forEach var="event" items="${recentEvents}">
                <tr>
                    <td>${event.timestamp}</td>
                    <td><c:out value="${event.eventType}"/></td>
                    <td>${event.actorUserId}</td>
                    <td><c:out value="${event.targetEntityId}"/></td>
                    <td><c:out value="${event.sourceIp}"/></td>
                    <td><c:out value="${event.outcome}"/></td>
                </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
