<%--
  Login page.
  SSEM: Integrity - CSRF token on form.
  SSEM: Confidentiality - no enumeration info in error messages.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Login - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h2 class="card-title mb-4">Sign In</h2>

                <c:if test="${param.registered eq 'true'}">
                    <div class="alert alert-success">Account created. Please sign in.</div>
                </c:if>
                <c:if test="${param.passwordReset eq 'true'}">
                    <div class="alert alert-success">Password reset successfully. Please sign in.</div>
                </c:if>
                <c:if test="${error != null}">
                    <div class="alert alert-danger"><c:out value="${error}"/></div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/auth/login"
                      autocomplete="off">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               required maxlength="50" autocomplete="username"
                               pattern="[a-zA-Z0-9_\-]{3,50}">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password"
                               required maxlength="128" autocomplete="current-password">
                    </div>
                    <button type="submit" class="btn btn-primary w-100">Sign In</button>
                </form>

                <hr>
                <p class="text-center mb-1">
                    <a href="${pageContext.request.contextPath}/auth/forgot-password">
                        Forgot password?
                    </a>
                </p>
                <p class="text-center">
                    Don't have an account?
                    <a href="${pageContext.request.contextPath}/auth/register">Register</a>
                </p>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
