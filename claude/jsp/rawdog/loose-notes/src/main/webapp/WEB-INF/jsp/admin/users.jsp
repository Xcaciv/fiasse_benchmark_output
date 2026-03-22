<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Manage Users - Admin - Loose Notes" />
<%@ include file="../includes/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h1 class="h3 mb-0">
        <i class="bi bi-people me-2"></i>Manage Users
    </h1>
    <a href="${pageContext.request.contextPath}/admin" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i>Back to Dashboard
    </a>
</div>

<c:if test="${not empty param.success}">
    <div class="alert alert-success alert-dismissible fade show" role="alert">
        <i class="bi bi-check-circle me-2"></i>${param.success}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<c:if test="${not empty param.error}">
    <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle me-2"></i>${param.error}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<!-- Search -->
<form action="${pageContext.request.contextPath}/admin" method="get" class="mb-4">
    <input type="hidden" name="action" value="users">
    <div class="input-group">
        <input type="text" class="form-control" name="search"
               value="${not empty searchQuery ? searchQuery : ''}"
               placeholder="Search by username or email...">
        <button class="btn btn-outline-primary" type="submit">
            <i class="bi bi-search me-1"></i>Search
        </button>
        <c:if test="${not empty searchQuery}">
            <a href="${pageContext.request.contextPath}/admin?action=users" class="btn btn-outline-secondary">
                Clear
            </a>
        </c:if>
    </div>
</form>

<!-- Users Table -->
<div class="card shadow mb-4">
    <div class="card-header">
        <h6 class="mb-0">
            Users (${users.size()})
            <c:if test="${not empty searchQuery}">
                &mdash; results for "<strong>${searchQuery}</strong>"
            </c:if>
        </h6>
    </div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
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
                        <td><strong>${user.username}</strong></td>
                        <td>${user.email}</td>
                        <td>
                            <c:choose>
                                <c:when test="${user.role == 'ADMIN'}">
                                    <span class="badge bg-warning text-dark">ADMIN</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge bg-secondary">USER</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>${noteCounts[user.id]}</td>
                        <td class="small text-muted">
                            <c:if test="${not empty user.createdAt}">
                                ${user.createdAt.toString().substring(0, 10)}
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${noteCounts[user.id] > 0}">
                                <button class="btn btn-sm btn-outline-secondary"
                                        onclick="showReassignModal(${user.id}, '${user.username}')">
                                    <i class="bi bi-arrow-left-right me-1"></i>Reassign Notes
                                </button>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty users}">
                    <tr>
                        <td colspan="7" class="text-center text-muted py-3">No users found</td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<!-- Reassign Notes Modal -->
<div class="modal fade" id="reassignModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Reassign Note</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form action="${pageContext.request.contextPath}/admin" method="post">
                <div class="modal-body">
                    <input type="hidden" name="action" value="reassign">
                    <p>Reassign a note from user: <strong id="fromUsername"></strong></p>
                    <div class="mb-3">
                        <label for="noteId" class="form-label">Select Note to Reassign</label>
                        <select class="form-select" name="noteId" id="noteSelect" required>
                            <option value="">Choose a note...</option>
                            <c:forEach var="note" items="${allNotes}">
                                <option value="${note.id}" data-user="${note.userId}">
                                    ${note.title} (by ${note.authorUsername})
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label for="newUserId" class="form-label">Assign to User</label>
                        <select class="form-select" name="newUserId" required>
                            <option value="">Choose a user...</option>
                            <c:forEach var="user" items="${users}">
                                <option value="${user.id}">${user.username}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-arrow-left-right me-1"></i>Reassign
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function showReassignModal(userId, username) {
    document.getElementById('fromUsername').textContent = username;
    const noteSelect = document.getElementById('noteSelect');
    // Show only notes belonging to this user
    Array.from(noteSelect.options).forEach(function(option) {
        if (option.value === '') return;
        option.style.display = option.dataset.user == userId ? '' : 'none';
    });
    new bootstrap.Modal(document.getElementById('reassignModal')).show();
}
</script>

<%@ include file="../includes/footer.jsp" %>
