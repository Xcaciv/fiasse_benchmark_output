<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card" style="max-width:560px; margin:0 auto;">
    <h1>Login</h1>
    <form method="post" action="${pageContext.request.contextPath}/auth/login">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <label>
            Username
                    <input type="text" name="username" maxlength="30" required value="${fn:escapeXml(formUsername)}">
        </label>
        <label>
            Password
            <input type="password" name="password" required autocomplete="current-password">
        </label>
        <div class="actions">
            <button class="btn primary" type="submit">Login</button>
            <a class="btn ghost" href="${pageContext.request.contextPath}/auth/forgot-password">Forgot password?</a>
        </div>
    </form>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
