<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Manage Users - Admin" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="bi bi-people"></i> Manage Users</h2>
    <a href="${pageContext.request.contextPath}/admin" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back to Dashboard
    </a>
</div>

<div class="card shadow">
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>Username</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Created</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="user" items="${users}">
                        <tr>
                            <td>${user.id}</td>
                            <td>${user.username}</td>
                            <td>${user.email}</td>
                            <td>
                                <span class="badge ${user.role == 'ADMIN' ? 'bg-danger' : 'bg-primary'}">
                                    ${user.role}
                                </span>
                            </td>
                            <td><small>${user.createdAt}</small></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<c:if test="${not empty notes}">
    <div class="card shadow mt-4">
        <div class="card-header">
            <h5 class="mb-0">Notes for: ${selectedUser.username}</h5>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Visibility</th>
                            <th>Created</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="note" items="${notes}">
                            <tr>
                                <td>${note.title}</td>
                                <td><span class="badge ${note.public ? 'bg-success' : 'bg-secondary'}">${note.public ? 'Public' : 'Private'}</span></td>
                                <td><small>${note.createdAt}</small></td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/admin/reassign?noteId=${note.id}"
                                       class="btn btn-sm btn-outline-warning">
                                        <i class="bi bi-arrow-left-right"></i> Reassign
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
