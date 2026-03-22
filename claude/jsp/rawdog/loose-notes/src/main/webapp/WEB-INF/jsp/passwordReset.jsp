<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Password Reset - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <div class="card shadow">
            <div class="card-body p-4">
                <h2 class="card-title text-center mb-4">
                    <i class="bi bi-key me-2"></i>Password Reset
                </h2>

                <c:if test="${not empty success}">
                    <div class="alert alert-success" role="alert">
                        <i class="bi bi-check-circle me-2"></i>${success}
                    </div>
                </c:if>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger alert-dismissible fade show" role="alert">
                        <i class="bi bi-exclamation-triangle me-2"></i>${error}
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                </c:if>

                <c:if test="${not empty resetToken}">
                    <div class="alert alert-warning">
                        <h6><i class="bi bi-info-circle me-2"></i>Demo Mode - Reset Token</h6>
                        <p class="small mb-2">In a real application, this would be sent by email. For demo purposes, here is your reset link:</p>
                        <div class="input-group">
                            <input type="text" class="form-control form-control-sm" id="resetLink" readonly
                                   value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/password-reset?action=reset&token=${resetToken}">
                            <button class="btn btn-sm btn-outline-secondary" onclick="copyResetLink()">
                                <i class="bi bi-clipboard"></i>
                            </button>
                        </div>
                        <div class="mt-2">
                            <a href="${pageContext.request.contextPath}/password-reset?action=reset&token=${resetToken}"
                               class="btn btn-sm btn-warning">
                                <i class="bi bi-arrow-right me-1"></i>Use Reset Link
                            </a>
                        </div>
                    </div>
                </c:if>

                <c:if test="${empty resetToken}">
                    <p class="text-muted small mb-3">
                        Enter your username to receive a password reset token.
                    </p>
                    <form action="${pageContext.request.contextPath}/password-reset" method="post">
                        <div class="mb-3">
                            <label for="username" class="form-label">Username</label>
                            <input type="text" class="form-control" id="username" name="username"
                                   required autofocus placeholder="Enter your username">
                        </div>
                        <div class="d-grid">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-key me-2"></i>Request Reset
                            </button>
                        </div>
                    </form>
                </c:if>

                <hr>
                <div class="text-center">
                    <a href="${pageContext.request.contextPath}/login">Back to Login</a>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
function copyResetLink() {
    const linkInput = document.getElementById('resetLink');
    linkInput.select();
    navigator.clipboard.writeText(linkInput.value).catch(() => {
        document.execCommand('copy');
    });
}
</script>

<%@ include file="includes/footer.jsp" %>
