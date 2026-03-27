<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Forgot Password" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h3 class="card-title text-center mb-4">
                    <i class="bi bi-key text-primary"></i> Reset Password
                </h3>

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

                <p class="text-muted">Enter your email address and we'll send you a password reset link.</p>

                <form method="post" action="${pageContext.request.contextPath}/forgot-password">
                    <div class="mb-3">
                        <label for="email" class="form-label">Email address</label>
                        <input type="email" class="form-control" id="email" name="email" required autofocus>
                    </div>
                    <div class="d-grid">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-envelope"></i> Send Reset Link
                        </button>
                    </div>
                </form>

                <hr>
                <p class="text-center mb-0">
                    <a href="${pageContext.request.contextPath}/login">Back to Sign In</a>
                </p>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
