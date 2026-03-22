<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card" style="max-width:640px; margin:0 auto;">
    <h1>Create account</h1>
    <form method="post" action="${pageContext.request.contextPath}/auth/register">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <label>
            Username
                    <input type="text" name="username" maxlength="30" required value="${fn:escapeXml(formUsername)}">
        </label>
        <label>
            Email address
                    <input type="email" name="email" maxlength="255" required value="${fn:escapeXml(formEmail)}">
        </label>
        <label>
            Password
            <input type="password" name="password" required autocomplete="new-password">
        </label>
        <label>
            Confirm password
            <input type="password" name="confirmPassword" required autocomplete="new-password">
        </label>
        <p class="muted">Passwords must be at least 8 characters and include uppercase, lowercase, digit, and special characters.</p>
        <button class="btn primary" type="submit">Register</button>
    </form>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
