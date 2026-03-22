<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Set New Password - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5 col-lg-4">
        <div class="card shadow">
            <div class="card-body p-4">
                <h2 class="card-title text-center mb-4">
                    <i class="bi bi-lock me-2"></i>Set New Password
                </h2>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger alert-dismissible fade show" role="alert">
                        <i class="bi bi-exclamation-triangle me-2"></i>${error}
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/password-reset" method="post">
                    <input type="hidden" name="action" value="reset">
                    <input type="hidden" name="token" value="${token}">

                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <input type="password" class="form-control" id="newPassword" name="newPassword"
                               required minlength="6" autofocus placeholder="Minimum 6 characters">
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                               required placeholder="Confirm new password">
                    </div>
                    <div class="d-grid">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-check-circle me-2"></i>Reset Password
                        </button>
                    </div>
                </form>

                <hr>
                <div class="text-center">
                    <a href="${pageContext.request.contextPath}/login">Back to Login</a>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="includes/footer.jsp" %>
