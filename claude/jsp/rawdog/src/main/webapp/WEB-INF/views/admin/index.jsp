<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Admin Dashboard" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<h2 class="mb-4"><i class="bi bi-speedometer2 text-primary"></i> Admin Dashboard</h2>

<div class="row g-4 mb-4">
    <div class="col-md-4">
        <div class="card shadow-sm text-center">
            <div class="card-body py-4">
                <i class="bi bi-people text-primary" style="font-size: 2.5rem;"></i>
                <h2 class="mt-2 mb-0">${totalUsers}</h2>
                <p class="text-muted mb-0">Total Users</p>
            </div>
            <div class="card-footer">
                <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-sm btn-outline-primary">
                    Manage Users
                </a>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow-sm text-center">
            <div class="card-body py-4">
                <i class="bi bi-journal-text text-success" style="font-size: 2.5rem;"></i>
                <h2 class="mt-2 mb-0">${totalNotes}</h2>
                <p class="text-muted mb-0">Total Notes</p>
            </div>
            <div class="card-footer">
                <a href="${pageContext.request.contextPath}/search" class="btn btn-sm btn-outline-success">
                    Browse Notes
                </a>
            </div>
        </div>
    </div>
</div>

<div class="card shadow-sm">
    <div class="card-header">
        <h5 class="mb-0"><i class="bi bi-clock-history"></i> Recent Activity</h5>
    </div>
    <div class="card-body p-0">
        <c:choose>
            <c:when test="${empty recentActivity}">
                <p class="text-muted p-3 mb-0">No activity logged yet.</p>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
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
                                    <td class="small text-muted">${log.createdAt}</td>
                                    <td>${not empty log.username ? log.username : '<em>system</em>'}</td>
                                    <td><span class="badge bg-secondary">${log.action}</span></td>
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

<%@ include file="../shared/footer.jsp" %>
