<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Profile - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-7">
        <h1 class="h3 mb-4"><i class="bi bi-person-circle me-2"></i>Profile</h1>

        <c:if test="${not empty success}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <i class="bi bi-check-circle me-2"></i>${success}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <c:if test="${not empty error}">
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="bi bi-exclamation-triangle me-2"></i>${error}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Stats -->
        <div class="row g-3 mb-4">
            <div class="col-md-4">
                <div class="card text-center bg-primary text-white">
                    <div class="card-body py-3">
                        <h3>${noteCount}</h3>
                        <p class="mb-0 small">Total Notes</p>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card text-center bg-success text-white">
                    <div class="card-body py-3">
                        <h3>${publicNoteCount}</h3>
                        <p class="mb-0 small">Public Notes</p>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card text-center bg-info text-white">
                    <div class="card-body py-3">
                        <h3 class="text-capitalize">${user.role}</h3>
                        <p class="mb-0 small">Account Role</p>
                    </div>
                </div>
            </div>
        </div>

        <!-- Update Email -->
        <div class="card shadow mb-4">
            <div class="card-header">
                <h5 class="mb-0"><i class="bi bi-envelope me-2"></i>Account Details</h5>
            </div>
            <div class="card-body">
                <div class="mb-3">
                    <label class="form-label">Username</label>
                    <input type="text" class="form-control" value="${user.username}" readonly>
                </div>
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="action" value="updateEmail">
                    <div class="mb-3">
                        <label for="email" class="form-label">Email Address</label>
                        <input type="email" class="form-control" id="email" name="email"
                               value="${user.email}" required>
                    </div>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-save me-2"></i>Update Email
                    </button>
                </form>
            </div>
        </div>

        <!-- Change Password -->
        <div class="card shadow mb-4">
            <div class="card-header">
                <h5 class="mb-0"><i class="bi bi-lock me-2"></i>Change Password</h5>
            </div>
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="action" value="changePassword">
                    <div class="mb-3">
                        <label for="currentPassword" class="form-label">Current Password</label>
                        <input type="password" class="form-control" id="currentPassword"
                               name="currentPassword" required>
                    </div>
                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <input type="password" class="form-control" id="newPassword"
                               name="newPassword" required minlength="6">
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword"
                               name="confirmPassword" required>
                    </div>
                    <button type="submit" class="btn btn-warning">
                        <i class="bi bi-lock me-2"></i>Change Password
                    </button>
                </form>
            </div>
        </div>

        <div class="text-muted small">
            <i class="bi bi-calendar me-1"></i>
            Member since:
            <c:if test="${not empty user.createdAt}">
                ${user.createdAt.toString().substring(0, 10)}
            </c:if>
        </div>
    </div>
</div>

<%@ include file="includes/footer.jsp" %>
