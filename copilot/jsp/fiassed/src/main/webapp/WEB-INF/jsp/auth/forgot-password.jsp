<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Forgot Password - Loose Notes"/>
</jsp:include>
<div class="auth-form">
    <h2>Forgot Password</h2>
    <c:if test="${not empty success}">
        <div class="alert alert-success"><c:out value="${success}"/></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${empty success}">
    <form method="post" action="${pageContext.request.contextPath}/auth/forgot-password">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" id="email" name="email" required maxlength="255">
        </div>
        <button type="submit" class="btn btn-primary">Send Reset Link</button>
    </form>
    </c:if>
    <p><a href="${pageContext.request.contextPath}/auth/login">Back to Login</a></p>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
