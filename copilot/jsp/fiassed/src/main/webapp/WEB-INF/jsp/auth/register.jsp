<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Register - Loose Notes"/>
</jsp:include>
<div class="auth-form">
    <h2>Create Account</h2>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/auth/register">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <div class="form-group">
            <label for="username">Username (3-50 chars)</label>
            <input type="text" id="username" name="username" required minlength="3" maxlength="50" autocomplete="username">
        </div>
        <div class="form-group">
            <label for="email">Email</label>
            <input type="email" id="email" name="email" required maxlength="255" autocomplete="email">
        </div>
        <div class="form-group">
            <label for="password">Password (12-64 chars)</label>
            <input type="password" id="password" name="password" required minlength="12" maxlength="64" autocomplete="new-password">
        </div>
        <div class="form-group">
            <label for="confirmPassword">Confirm Password</label>
            <input type="password" id="confirmPassword" name="confirmPassword" required minlength="12" maxlength="64" autocomplete="new-password">
        </div>
        <button type="submit" class="btn btn-primary">Register</button>
    </form>
    <p>Already have an account? <a href="${pageContext.request.contextPath}/auth/login">Login</a></p>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
