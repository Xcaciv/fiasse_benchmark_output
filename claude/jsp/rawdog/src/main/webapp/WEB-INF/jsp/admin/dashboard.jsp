<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Admin Dashboard - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<h2 class="mb-4"><i class="bi bi-shield-check"></i> Admin Dashboard</h2>

<div class="row g-4 mb-4">
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <i class="bi bi-people display-4 text-primary"></i>
                <h2 class="mt-2">${userCount}</h2>
                <p class="text-muted">Total Users</p>
                <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-primary btn-sm">
                    Manage Users
                </a>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <i class="bi bi-journals display-4 text-success"></i>
                <h2 class="mt-2">${noteCount}</h2>
                <p class="text-muted">Total Notes</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <i class="bi bi-activity display-4 text-warning"></i>
                <h2 class="mt-2">${recentActivity.size()}</h2>
                <p class="text-muted">Recent Activities</p>
            </div>
        </div>
    </div>
</div>

<div class="card shadow">
    <div class="card-header">
        <h5 class="mb-0"><i class="bi bi-activity"></i> Recent Activity Log</h5>
    </div>
    <div class="card-body p-0">
        <c:choose>
            <c:when test="${empty recentActivity}">
                <p class="p-3 text-muted">No activity recorded yet.</p>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th>Time</th>
                                <th>User</th>
                                <th>Action</th>
                                <th>Details</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="log" items="${recentActivity}">
                                <tr>
                                    <td><small>${log.createdAt}</small></td>
                                    <td>${not empty log.username ? log.username : '<em>system</em>'}</td>
                                    <td><span class="badge bg-secondary">${log.action}</span></td>
                                    <td><small>${log.details}</small></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
