<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Manage Users" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="bi bi-people text-primary"></i> Users</h2>
    <a href="${pageContext.request.contextPath}/admin" class="btn btn-outline-secondary btn-sm">
        <i class="bi bi-arrow-left"></i> Dashboard
    </a>
</div>

<c:if test="${not empty param.success}">
    <div class="alert alert-success alert-dismissible fade show">
        <i class="bi bi-check-circle"></i> Note reassigned successfully.
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<!-- Search -->
<form method="get" action="${pageContext.request.contextPath}/admin/users" class="mb-4">
    <div class="input-group">
        <input type="text" class="form-control" name="q"
               value="${searchQuery}" placeholder="Search by username or email...">
        <button type="submit" class="btn btn-outline-primary">
            <i class="bi bi-search"></i> Search
        </button>
        <c:if test="${not empty searchQuery}">
            <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary">Clear</a>
        </c:if>
    </div>
</form>

<div class="card shadow-sm">
    <div class="card-body p-0">
        <c:choose>
            <c:when test="${empty users}">
                <p class="text-muted p-3 mb-0">No users found.</p>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>ID</th>
                                <th>Username</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Registered</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="user" items="${users}">
                                <tr>
                                    <td class="text-muted small">${user.id}</td>
                                    <td>
                                        <strong>${user.username}</strong>
                                        <c:if test="${user.admin}">
                                            <span class="badge bg-danger ms-1">Admin</span>
                                        </c:if>
                                    </td>
                                    <td>${user.email}</td>
                                    <td>
                                        <span class="badge ${user.admin ? 'bg-danger' : 'bg-secondary'}">
                                            ${user.role}
                                        </span>
                                    </td>
                                    <td class="small text-muted">
                                        <c:if test="${not empty user.createdAt}">
                                            ${user.createdAtDisplay}
                                        </c:if>
                                    </td>
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
