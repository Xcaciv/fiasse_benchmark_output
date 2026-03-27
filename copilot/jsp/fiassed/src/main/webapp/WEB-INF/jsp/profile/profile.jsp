<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Profile - Loose Notes"/>
</jsp:include>
<div class="profile-page">
    <h2>Profile</h2>
    <c:if test="${not empty error}"><div class="alert alert-error"><c:out value="${error}"/></div></c:if>
    <c:if test="${not empty success}"><div class="alert alert-success"><c:out value="${success}"/></div></c:if>
    <div class="profile-info">
        <p><strong>Username:</strong> <c:out value="${user.username}"/></p>
        <p><strong>Email:</strong> <c:out value="${user.email}"/></p>
        <p><strong>Role:</strong> <c:out value="${user.role}"/></p>
        <p><strong>Member since:</strong> ${user.createdAt}</p>
    </div>
    <div class="change-password">
        <h3>Change Password</h3>
        <form method="post" action="${pageContext.request.contextPath}/profile">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="changePassword">
            <div class="form-group">
                <label for="oldPassword">Current Password</label>
                <input type="password" id="oldPassword" name="oldPassword" required autocomplete="current-password">
            </div>
            <div class="form-group">
                <label for="newPassword">New Password (12-64 chars)</label>
                <input type="password" id="newPassword" name="newPassword" required minlength="12" maxlength="64" autocomplete="new-password">
            </div>
            <div class="form-group">
                <label for="confirmPassword">Confirm New Password</label>
                <input type="password" id="confirmPassword" name="confirmPassword" required minlength="12" maxlength="64" autocomplete="new-password">
            </div>
            <button type="submit" class="btn btn-primary">Change Password</button>
        </form>
    </div>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
