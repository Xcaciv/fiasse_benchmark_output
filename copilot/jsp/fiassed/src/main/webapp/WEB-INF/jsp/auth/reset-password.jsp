<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Reset Password - Loose Notes"/>
</jsp:include>
<div class="auth-form">
    <h2>Reset Password</h2>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/auth/reset-password">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="token" value="<c:out value='${token}'/>">
        <div class="form-group">
            <label for="newPassword">New Password (12-64 chars)</label>
            <input type="password" id="newPassword" name="newPassword" required minlength="12" maxlength="64">
        </div>
        <div class="form-group">
            <label for="confirmPassword">Confirm New Password</label>
            <input type="password" id="confirmPassword" name="confirmPassword" required minlength="12" maxlength="64">
        </div>
        <button type="submit" class="btn btn-primary">Reset Password</button>
    </form>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
