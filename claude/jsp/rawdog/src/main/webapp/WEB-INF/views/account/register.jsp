<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Register" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h3 class="card-title text-center mb-4">
                    <i class="bi bi-person-plus text-primary"></i> Create Account
                </h3>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger">
                        <i class="bi bi-exclamation-triangle"></i> ${error}
                    </div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/register">
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               value="${param.username}" required autofocus minlength="3" maxlength="50">
                        <div class="form-text">3-50 characters.</div>
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">Email address</label>
                        <input type="email" class="form-control" id="email" name="email"
                               value="${param.email}" required maxlength="255">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password"
                               required minlength="6">
                        <div class="form-text">At least 6 characters.</div>
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm Password</label>
                        <input type="password" class="form-control" id="confirmPassword"
                               name="confirmPassword" required minlength="6">
                    </div>
                    <div class="d-grid">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-person-plus"></i> Create Account
                        </button>
                    </div>
                </form>

                <hr>
                <p class="text-center mb-0">
                    Already have an account?
                    <a href="${pageContext.request.contextPath}/login">Sign in</a>
                </p>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
