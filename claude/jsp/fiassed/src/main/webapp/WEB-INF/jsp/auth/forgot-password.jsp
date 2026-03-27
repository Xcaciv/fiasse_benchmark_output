<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-container">
    <h1>Forgot Password</h1>

    <c:if test="${not empty message}">
        <div class="alert alert-info">
            <c:out value="${message}"/>
        </div>
    </c:if>

    <p>Enter the email address associated with your account and we will send you a password reset link.</p>

    <form method="post" action="${pageContext.request.contextPath}/auth/forgot-password" novalidate>
        <input type="hidden" name="_csrf" value="${csrfToken}"/>

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

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Send Reset Link</button>
        </div>
    </form>

    <div class="auth-links">
        <a href="${pageContext.request.contextPath}/auth/login">Back to Sign In</a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
