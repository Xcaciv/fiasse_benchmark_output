<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card" style="max-width:700px; margin:0 auto;">
    <h1>Your profile</h1>
    <form method="post" action="${pageContext.request.contextPath}/profile">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <label>
            Username
                    <input type="text" name="username" maxlength="30" required value="${fn:escapeXml(currentUser.username)}">
        </label>
        <label>
            Email address
                    <input type="email" name="email" maxlength="255" required value="${fn:escapeXml(currentUser.email)}">
        </label>
        <div class="grid two">
            <label>
                Current password
                <input type="password" name="currentPassword" autocomplete="current-password">
            </label>
            <label>
                New password
                <input type="password" name="newPassword" autocomplete="new-password">
            </label>
        </div>
        <label>
            Confirm new password
            <input type="password" name="confirmPassword" autocomplete="new-password">
        </label>
        <p class="muted">Leave the password fields blank if you only want to update your username or email address.</p>
        <button class="btn primary" type="submit">Save profile</button>
    </form>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
