<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Forgot Password - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow">
            <div class="card-header bg-dark text-white">
                <h4 class="mb-0"><i class="bi bi-key"></i> Forgot Password</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i> ${error}</div>
                </c:if>
                <c:if test="${not empty success}">
                    <div class="alert alert-success"><i class="bi bi-check-circle"></i> ${success}</div>
                </c:if>
                <p class="text-muted">Enter your email address and we will log a reset link to the server console.</p>
                <form method="post" action="${pageContext.request.contextPath}/forgot-password">
                    <div class="mb-3">
                        <label for="email" class="form-label">Email Address</label>
                        <input type="email" class="form-control" id="email" name="email" required autofocus>
                    </div>
                    <div class="d-grid">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-send"></i> Send Reset Link
                        </button>
                    </div>
                </form>
            </div>
            <div class="card-footer text-center text-muted">
                <a href="${pageContext.request.contextPath}/login">Back to Login</a>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
