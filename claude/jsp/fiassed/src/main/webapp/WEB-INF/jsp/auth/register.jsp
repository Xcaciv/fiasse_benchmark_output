<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-container">
    <h1>Create Account</h1>

    <c:if test="${not empty error}">
        <div class="alert alert-error">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/auth/register" novalidate id="registerForm">
        <input type="hidden" name="_csrf" value="${csrfToken}"/>

        <div class="form-group">
            <label for="username">Username</label>
            <input type="text"
                   id="username"
                   name="username"
                   required
                   minlength="3"
                   maxlength="50"
                   autocomplete="username"
                   value="${fn:escapeXml(param.username)}"/>
            <small class="form-hint">3 to 50 characters.</small>
        </div>

        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email"
                   id="email"
                   name="email"
                   required
                   maxlength="255"
                   autocomplete="email"
                   value="${fn:escapeXml(param.email)}"/>
        </div>

        <div class="form-group">
            <label for="password">Password</label>
            <input type="password"
                   id="password"
                   name="password"
                   required
                   minlength="12"
                   autocomplete="new-password"/>
            <small class="form-hint">Minimum 12 characters.</small>
        </div>

        <div class="form-group">
            <label for="confirmPassword">Confirm Password</label>
            <input type="password"
                   id="confirmPassword"
                   name="confirmPassword"
                   required
                   minlength="12"
                   autocomplete="new-password"/>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Create Account</button>
        </div>
    </form>

    <div class="auth-links">
        Already have an account? <a href="${pageContext.request.contextPath}/auth/login">Sign in</a>
    </div>
</div>

<script>
    (function () {
        var form = document.getElementById('registerForm');
        if (form) {
            form.addEventListener('submit', function (e) {
                var pw = document.getElementById('password').value;
                var cpw = document.getElementById('confirmPassword').value;
                if (pw !== cpw) {
                    e.preventDefault();
                    alert('Passwords do not match. Please try again.');
                }
            });
        }
    }());
</script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
