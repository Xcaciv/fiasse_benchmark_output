<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Register - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-dark text-white">
                <h4 class="mb-0"><i class="bi bi-person-plus"></i> Create Account</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i> ${error}</div>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/register">
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               value="${not empty username ? username : ''}" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">Email</label>
                        <input type="email" class="form-control" id="email" name="email"
                               value="${not empty email ? email : ''}" required>
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password"
                               minlength="6" required>
                        <div class="form-text">At least 6 characters.</div>
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" required>
                    </div>
                    <div class="d-grid">
                        <button type="submit" class="btn btn-success">
                            <i class="bi bi-person-check"></i> Register
                        </button>
                    </div>
                </form>
            </div>
            <div class="card-footer text-center text-muted">
                Already have an account?
                <a href="${pageContext.request.contextPath}/login">Login here</a>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
