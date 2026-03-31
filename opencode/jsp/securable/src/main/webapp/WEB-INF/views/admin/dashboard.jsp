<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:include page="../layout.jsp">
    <jsp:param name="pageTitle" value="Admin Dashboard"/>
</jsp:include>
<jsp:body>
<div class="row mb-4">
    <div class="col">
        <h2>Admin Dashboard</h2>
    </div>
</div>

<div class="row mb-4">
    <div class="col-md-4">
        <div class="card text-center">
            <div class="card-body">
                <i class="bi bi-people" style="font-size: 3rem; color: #4a6cf7;"></i>
                <h3 class="mt-2">${userCount}</h3>
                <p class="text-muted mb-0">Total Users</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card text-center">
            <div class="card-body">
                <i class="bi bi-journal-text" style="font-size: 3rem; color: #28a745;"></i>
                <h3 class="mt-2">${noteCount}</h3>
                <p class="text-muted mb-0">Total Notes</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card text-center">
            <div class="card-body">
                <i class="bi bi-person-badge" style="font-size: 3rem; color: #17a2b8;"></i>
                <a href="${pageContext.request.contextPath}/admin?action=users" class="btn btn-outline-primary mt-2">
                    Manage Users
                </a>
            </div>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h4 class="mb-0">Recent Activity</h4>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${empty recentActivity}">
                <p class="text-muted">No recent activity.</p>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>Time</th>
                                <th>User</th>
                                <th>Action</th>
                                <th>Details</th>
                                <th>IP Address</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="entry" items="${recentActivity}">
                                <tr>
                                    <td><fmt:formatDate value="${entry.createdAt}" pattern="MMM d, HH:mm"/></td>
                                    <td>${entry.username != null ? entry.username : 'System'}</td>
                                    <td>${entry.action}</td>
                                    <td>${entry.details}</td>
                                    <td><small class="text-muted">${entry.ipAddress}</small></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</jsp:body>
