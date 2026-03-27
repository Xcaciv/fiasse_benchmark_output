<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-container">
    <h1>Sign In</h1>

    <c:if test="${param.registered == 'true'}">
        <div class="alert alert-success">
            Registration successful! You can now sign in.
        </div>
    </c:if>

    <c:if test="${param.reset == 'true'}">
        <div class="alert alert-success">
            Your password has been reset. You can now sign in with your new password.
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/auth/login" novalidate>
        <input type="hidden" name="_csrf" value="${csrfToken}"/>

        <div class="form-group">
            <label for="username">Username</label>
            <input type="text"
                   id="username"
                   name="username"
                   required
                   autocomplete="username"
                   maxlength="50"
                   value="${fn:escapeXml(param.username)}"/>
        </div>

        <div class="form-group">
            <label for="password">Password</label>
            <input type="password"
                   id="password"
                   name="password"
                   required
                   autocomplete="current-password"/>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Sign In</button>
        </div>
    </form>

    <div class="auth-links">
        <a href="${pageContext.request.contextPath}/auth/forgot-password">Forgot your password?</a>
        <span class="separator">|</span>
        <a href="${pageContext.request.contextPath}/auth/register">Create an account</a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
