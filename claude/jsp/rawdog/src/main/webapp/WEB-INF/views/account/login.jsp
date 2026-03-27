<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Login" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h3 class="card-title text-center mb-4">
                    <i class="bi bi-box-arrow-in-right text-primary"></i> Sign In
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

                <form method="post" action="${pageContext.request.contextPath}/login">
                    <c:if test="${not empty param.returnUrl}">
                        <input type="hidden" name="returnUrl" value="${param.returnUrl}">
                    </c:if>
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               value="${param.username}" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password" required>
                    </div>
                    <div class="d-grid">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-box-arrow-in-right"></i> Sign In
                        </button>
                    </div>
                </form>

                <hr>
                <p class="text-center mb-1">
                    <a href="${pageContext.request.contextPath}/forgot-password">Forgot password?</a>
                </p>
                <p class="text-center mb-0">
                    Don't have an account?
                    <a href="${pageContext.request.contextPath}/register">Register</a>
                </p>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
