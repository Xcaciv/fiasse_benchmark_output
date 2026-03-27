<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Profile Settings</h1>
</div>

<c:if test="${not empty successMessage}">
    <div class="alert alert-success">
        <c:out value="${successMessage}"/>
    </div>
</c:if>

<c:if test="${not empty errorMessage}">
    <div class="alert alert-error">
        <c:out value="${errorMessage}"/>
    </div>
</c:if>

<%-- Change Password --%>
<section class="profile-section card">
    <h2>Change Password</h2>

    <c:if test="${not empty passwordError}">
        <div class="alert alert-error">
            <c:out value="${passwordError}"/>
        </div>
    </c:if>
    <c:if test="${not empty passwordSuccess}">
        <div class="alert alert-success">
            <c:out value="${passwordSuccess}"/>
        </div>
    </c:if>

    <form method="post"
          action="${pageContext.request.contextPath}/profile/change-password"
          novalidate
          id="changePasswordForm">
        <input type="hidden" name="_csrf" value="${csrfToken}"/>

        <div class="form-group">
            <label for="currentPasswordPw">Current Password</label>
            <input type="password"
                   id="currentPasswordPw"
                   name="currentPassword"
                   required
                   autocomplete="current-password"/>
        </div>

        <div class="form-group">
            <label for="newPassword">New Password</label>
            <input type="password"
                   id="newPassword"
                   name="newPassword"
                   required
                   minlength="12"
                   autocomplete="new-password"/>
            <small class="form-hint">Minimum 12 characters.</small>
        </div>

        <div class="form-group">
            <label for="confirmNewPassword">Confirm New Password</label>
            <input type="password"
                   id="confirmNewPassword"
                   name="confirmNewPassword"
                   required
                   minlength="12"
                   autocomplete="new-password"/>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Update Password</button>
        </div>
    </form>
</section>

<%-- Change Username --%>
<section class="profile-section card">
    <h2>Change Username</h2>

    <c:if test="${not empty usernameError}">
        <div class="alert alert-error">
            <c:out value="${usernameError}"/>
        </div>
    </c:if>
    <c:if test="${not empty usernameSuccess}">
        <div class="alert alert-success">
            <c:out value="${usernameSuccess}"/>
        </div>
    </c:if>

    <form method="post"
          action="${pageContext.request.contextPath}/profile/change-username"
          novalidate>
        <input type="hidden" name="_csrf" value="${csrfToken}"/>

        <div class="form-group">
            <label for="currentPasswordUn">Current Password</label>
            <input type="password"
                   id="currentPasswordUn"
                   name="currentPassword"
                   required
                   autocomplete="current-password"/>
        </div>

        <div class="form-group">
            <label for="newUsername">New Username</label>
            <input type="text"
                   id="newUsername"
                   name="newUsername"
                   required
                   minlength="3"
                   maxlength="50"
                   autocomplete="username"/>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Update Username</button>
        </div>
    </form>
</section>

<%-- Change Email --%>
<section class="profile-section card">
    <h2>Change Email</h2>

    <c:if test="${not empty emailError}">
        <div class="alert alert-error">
            <c:out value="${emailError}"/>
        </div>
    </c:if>
    <c:if test="${not empty emailSuccess}">
        <div class="alert alert-success">
            <c:out value="${emailSuccess}"/>
        </div>
    </c:if>

    <form method="post"
          action="${pageContext.request.contextPath}/profile/change-email"
          novalidate>
        <input type="hidden" name="_csrf" value="${csrfToken}"/>

        <div class="form-group">
            <label for="currentPasswordEm">Current Password</label>
            <input type="password"
                   id="currentPasswordEm"
                   name="currentPassword"
                   required
                   autocomplete="current-password"/>
        </div>

        <div class="form-group">
            <label for="newEmail">New Email Address</label>
            <input type="email"
                   id="newEmail"
                   name="newEmail"
                   required
                   maxlength="255"
                   autocomplete="email"/>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Update Email</button>
        </div>
    </form>
</section>

<script>
    (function () {
        var form = document.getElementById('changePasswordForm');
        if (form) {
            form.addEventListener('submit', function (e) {
                var pw = document.getElementById('newPassword').value;
                var cpw = document.getElementById('confirmNewPassword').value;
                if (pw !== cpw) {
                    e.preventDefault();
                    alert('New passwords do not match. Please try again.');
                }
            });
        }
    }());
</script>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
