<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:include page="../layout.jsp">
    <jsp:param name="pageTitle" value="Manage Users"/>
</jsp:include>
<jsp:body>
<div class="row mb-4">
    <div class="col-md-8">
        <h2>Manage Users</h2>
    </div>
    <div class="col-md-4">
        <form method="get" action="${pageContext.request.contextPath}/admin" class="d-flex">
            <input type="hidden" name="action" value="searchUsers">
            <input type="text" class="form-control me-2" name="query" 
                   value="${query}" placeholder="Search users...">
            <button type="submit" class="btn btn-outline-primary">Search</button>
        </form>
    </div>
</div>

<div class="card">
    <div class="card-body">
        <c:choose>
            <c:when test="${empty users}">
                <p class="text-muted">No users found.</p>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Username</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Notes</th>
                                <th>Joined</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="user" items="${users}">
                                <tr>
                                    <td>${user.id}</td>
                                    <td>${user.username}</td>
                                    <td>${user.email}</td>
                                    <td>
                                        <span class="badge ${user.admin ? 'bg-danger' : 'bg-primary'}">
                                            ${user.role}
                                        </span>
                                    </td>
                                    <td>${noteCounts[user.id]}</td>
                                    <td><fmt:formatDate value="${user.createdAt}" pattern="MMM d, yyyy"/></td>
                                    <td>
                                        <button class="btn btn-sm btn-outline-primary" 
                                                data-bs-toggle="modal" 
                                                data-bs-target="#reassignModal${user.id}">
                                            Reassign Notes
                                        </button>
                                    </td>
                                </tr>
                                
                                <div class="modal fade" id="reassignModal${user.id}" tabindex="-1">
                                    <div class="modal-dialog">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <h5 class="modal-title">Reassign Notes from ${user.username}</h5>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                            </div>
                                            <form method="post" action="${pageContext.request.contextPath}/admin">
                                                <div class="modal-body">
                                                    <input type="hidden" name="action" value="reassignNote">
                                                    <p>Select a new owner for notes owned by ${user.username}:</p>
                                                    <select class="form-select" name="newOwnerId" required>
                                                        <option value="">Select user...</option>
                                                        <c:forEach var="u" items="${users}">
                                                            <c:if test="${u.id != user.id}">
                                                                <option value="${u.id}">${u.username} (${u.email})</option>
                                                            </c:if>
                                                        </c:forEach>
                                                    </select>
                                                    <input type="hidden" name="noteId" value="${user.id}">
                                                </div>
                                                <div class="modal-footer">
                                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                                                    <button type="submit" class="btn btn-primary">Reassign</button>
                                                </div>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</jsp:body>
