<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Admin Dashboard - Loose Notes" />
<%@ include file="../includes/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h1 class="h3 mb-0">
        <i class="bi bi-shield-lock me-2"></i>Admin Dashboard
    </h1>
    <a href="${pageContext.request.contextPath}/admin?action=users" class="btn btn-outline-primary">
        <i class="bi bi-people me-2"></i>Manage Users
    </a>
</div>

<!-- Stats -->
<div class="row g-4 mb-4">
    <div class="col-md-6">
        <div class="card bg-primary text-white text-center">
            <div class="card-body py-4">
                <h2 class="display-4">${userCount}</h2>
                <p class="mb-0 fs-5"><i class="bi bi-people me-2"></i>Total Users</p>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card bg-success text-white text-center">
            <div class="card-body py-4">
                <h2 class="display-4">${noteCount}</h2>
                <p class="mb-0 fs-5"><i class="bi bi-journal-text me-2"></i>Total Notes</p>
            </div>
        </div>
    </div>
</div>

<!-- Recent Activity -->
<div class="card shadow">
    <div class="card-header">
        <h5 class="mb-0"><i class="bi bi-activity me-2"></i>Recent Activity</h5>
    </div>
    <div class="card-body p-0">
        <c:choose>
            <c:when test="${empty recentActivity}">
                <p class="text-muted p-3">No activity logged yet.</p>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-hover table-sm mb-0">
                        <thead class="table-light">
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
                                    <td class="text-nowrap small text-muted">
                                        <c:if test="${not empty log.createdAt}">
                                            ${log.createdAt.toString().substring(0, 16).replace('T', ' ')}
                                        </c:if>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty log.username}">
                                                ${log.username}
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted">system</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <span class="badge bg-secondary">${log.action}</span>
                                    </td>
                                    <td class="small">${log.details}</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../includes/footer.jsp" %>
