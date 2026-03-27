<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Login - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow">
            <div class="card-header bg-dark text-white">
                <h4 class="mb-0"><i class="bi bi-box-arrow-in-right"></i> Login</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i> ${error}</div>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/login">
                    <input type="hidden" name="returnUrl" value="${param.returnUrl}">
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               value="${not empty username ? username : ''}" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password" required>
                    </div>
                    <div class="d-grid mb-3">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-box-arrow-in-right"></i> Login
                        </button>
                    </div>
                    <div class="text-center">
                        <a href="${pageContext.request.contextPath}/forgot-password" class="text-muted small">
                            Forgot password?
                        </a>
                    </div>
                </form>
            </div>
            <div class="card-footer text-center text-muted">
                Don't have an account?
                <a href="${pageContext.request.contextPath}/register">Register here</a>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
