<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card" style="max-width:560px; margin:0 auto;">
    <h1>Reset your password</h1>
    <p class="muted">For this demo, reset emails are written to <code>data/outbox</code> instead of being sent through SMTP.</p>
    <form method="post" action="${pageContext.request.contextPath}/auth/forgot-password">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <label>
            Email address
                    <input type="email" name="email" maxlength="255" required value="${fn:escapeXml(formEmail)}">
        </label>
        <button class="btn primary" type="submit">Request password reset</button>
    </form>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
