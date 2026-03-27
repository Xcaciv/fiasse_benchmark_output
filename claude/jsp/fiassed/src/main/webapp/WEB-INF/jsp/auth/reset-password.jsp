<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="auth-container">
    <h1>Reset Password</h1>

    <c:if test="${not empty error}">
        <div class="alert alert-error">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/auth/reset-password" novalidate id="resetForm">
        <input type="hidden" name="_csrf" value="${csrfToken}"/>
        <input type="hidden" name="token" value="${fn:escapeXml(token)}"/>

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
            <label for="confirmPassword">Confirm New Password</label>
            <input type="password"
                   id="confirmPassword"
                   name="confirmPassword"
                   required
                   minlength="12"
                   autocomplete="new-password"/>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Reset Password</button>
        </div>
    </form>
</div>

<script>
    (function () {
        var form = document.getElementById('resetForm');
        if (form) {
            form.addEventListener('submit', function (e) {
                var pw = document.getElementById('newPassword').value;
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
