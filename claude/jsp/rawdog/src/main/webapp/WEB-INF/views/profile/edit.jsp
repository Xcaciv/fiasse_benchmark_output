<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Edit Profile" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <h2 class="mb-4"><i class="bi bi-person-gear text-primary"></i> Edit Profile</h2>

        <c:if test="${not empty error}">
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> ${error}
            </div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success">
                <i class="bi bi-check-circle"></i> ${success}
            </div>
        </c:if>

        <div class="card shadow-sm mb-4">
            <div class="card-header"><h6 class="mb-0">Account Information</h6></div>
            <div class="card-body p-4">
                <form method="post" action="${pageContext.request.contextPath}/profile">
                    <div class="mb-3">
                        <label for="username" class="form-label fw-semibold">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               value="${user.username}" required minlength="3" maxlength="50">
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label fw-semibold">Email address</label>
                        <input type="email" class="form-control" id="email" name="email"
                               value="${user.email}" required maxlength="255">
                    </div>
                    <hr>
                    <h6 class="text-muted mb-3">Change Password (leave blank to keep current)</h6>
                    <div class="mb-3">
                        <label for="currentPassword" class="form-label">Current Password</label>
                        <input type="password" class="form-control" id="currentPassword" name="currentPassword">
                    </div>
                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <input type="password" class="form-control" id="newPassword" name="newPassword"
                               minlength="6">
                        <div class="form-text">At least 6 characters.</div>
                    </div>
                    <div class="mb-4">
                        <label for="confirmPassword" class="form-label">Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                               minlength="6">
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-save"></i> Save Changes
                        </button>
                        <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>

        <div class="card shadow-sm">
            <div class="card-body">
                <small class="text-muted">
                    <i class="bi bi-info-circle"></i>
                    Member since
                    <c:if test="${not empty user.createdAt}">
                        ${user.createdAt}
                    </c:if>
                    &middot; Role: <span class="badge ${user.admin ? 'bg-danger' : 'bg-secondary'}">${user.role}</span>
                </small>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
