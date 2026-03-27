<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="My Profile - Loose Notes" scope="request"/>
<%@ include file="layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-7">
        <div class="card shadow">
            <div class="card-header bg-dark text-white">
                <h4 class="mb-0"><i class="bi bi-person-circle"></i> Edit Profile</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i> ${error}</div>
                </c:if>
                <c:if test="${not empty success}">
                    <div class="alert alert-success"><i class="bi bi-check-circle"></i> ${success}</div>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/profile">
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               value="${user.username}" required>
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">Email</label>
                        <input type="email" class="form-control" id="email" name="email"
                               value="${user.email}" required>
                    </div>

                    <hr>
                    <h6 class="text-muted">Change Password (leave blank to keep current password)</h6>

                    <div class="mb-3">
                        <label for="currentPassword" class="form-label">Current Password</label>
                        <input type="password" class="form-control" id="currentPassword" name="currentPassword">
                    </div>
                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <input type="password" class="form-control" id="newPassword" name="newPassword" minlength="6">
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword">
                    </div>

                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-save"></i> Save Changes
                    </button>
                </form>
            </div>
            <div class="card-footer text-muted small">
                Account created: ${user.createdAt} &bull; Role: ${user.role}
            </div>
        </div>
    </div>
</div>

<%@ include file="layout/footer.jsp" %>
