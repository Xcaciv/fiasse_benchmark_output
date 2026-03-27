<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="fas fa-users text-primary"></i> User Management</h2>
    <a href="${pageContext.request.contextPath}/admin" class="btn btn-secondary">
        <i class="fas fa-arrow-left"></i> Back to Dashboard
    </a>
</div>

<div class="card shadow mb-4">
    <div class="card-body">
        <form action="${pageContext.request.contextPath}/admin" method="post" class="form-inline">
            <input type="hidden" name="action" value="searchUsers">
            <div class="input-group w-100">
                <input type="text" class="form-control" name="query" placeholder="Search by username or email..." 
                       value="${searchQuery}">
                <div class="input-group-append">
                    <button type="submit" class="btn btn-primary">
                        <i class="fas fa-search"></i> Search
                    </button>
                </div>
            </div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="card-body">
        <c:if test="${empty users}">
            <p class="text-muted text-center">No users found</p>
        </c:if>
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Username</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th>Created</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="user" items="${users}">
                        <tr>
                            <td>${user.id}</td>
                            <td><strong>${user.username}</strong></td>
                            <td>${user.email}</td>
                            <td>
                                <span class="badge badge-${user.role == 'ADMIN' ? 'danger' : 'primary'}">
                                    ${user.role}
                                </span>
                            </td>
                            <td>
                                <span class="badge badge-${user.active ? 'success' : 'secondary'}">
                                    ${user.active ? 'Active' : 'Inactive'}
                                </span>
                            </td>
                            <td><small>${user.createdAt}</small></td>
                            <td>
                                <a href="${pageContext.request.contextPath}/admin?action=notes&userId=${user.id}" class="btn btn-sm btn-outline-primary">
                                    View Notes
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<jsp:include page="footer.jsp"/>
