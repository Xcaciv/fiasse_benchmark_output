<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Profile - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-7">
        <h1 class="h3 mb-4">Edit Profile</h1>

        <c:if test="${success != null}">
            <div class="alert alert-success"><c:out value="${success}"/></div>
        </c:if>
        <c:if test="${error != null}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <%-- Update profile form --%>
        <div class="card mb-4">
            <div class="card-header">Profile Information</div>
            <div class="card-body">
                <form method="post"
                      action="${pageContext.request.contextPath}/profile/update">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               required minlength="3" maxlength="50"
                               pattern="[a-zA-Z0-9_\-]{3,50}"
                               value="<c:out value='${currentUser.username}'/>">
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">Email Address</label>
                        <input type="email" class="form-control" id="email" name="email"
                               required maxlength="255"
                               value="<c:out value='${currentUser.email}'/>">
                    </div>
                    <button type="submit" class="btn btn-primary">Update Profile</button>
                </form>
            </div>
        </div>

        <%-- Change password form --%>
        <div class="card">
            <div class="card-header">Change Password</div>
            <div class="card-body">
                <form method="post"
                      action="${pageContext.request.contextPath}/profile/change-password">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <div class="mb-3">
                        <label for="currentPassword" class="form-label">Current Password</label>
                        <input type="password" class="form-control" id="currentPassword"
                               name="currentPassword" required maxlength="128"
                               autocomplete="current-password">
                    </div>
                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <input type="password" class="form-control" id="newPassword"
                               name="newPassword" required minlength="8" maxlength="128"
                               autocomplete="new-password">
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword"
                               name="confirmPassword" required maxlength="128"
                               autocomplete="new-password">
                    </div>
                    <button type="submit" class="btn btn-warning">Change Password</button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
