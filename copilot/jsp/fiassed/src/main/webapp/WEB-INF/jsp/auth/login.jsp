<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Login - Loose Notes"/>
</jsp:include>
<div class="auth-form">
    <h2>Login</h2>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${not empty success}">
        <div class="alert alert-success"><c:out value="${success}"/></div>
    </c:if>
    <c:if test="${param.passwordChanged == 'true'}">
        <div class="alert alert-success">Password changed successfully. Please log in again.</div>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/auth/login">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" name="username" required autocomplete="username" maxlength="50">
        </div>
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password" required autocomplete="current-password" maxlength="64">
        </div>
        <button type="submit" class="btn btn-primary">Login</button>
    </form>
    <p><a href="${pageContext.request.contextPath}/auth/forgot-password">Forgot password?</a></p>
    <p>Don't have an account? <a href="${pageContext.request.contextPath}/auth/register">Register</a></p>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
