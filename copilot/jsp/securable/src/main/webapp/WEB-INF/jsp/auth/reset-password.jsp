<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card" style="max-width:560px; margin:0 auto;">
    <h1>Choose a new password</h1>
    <c:choose>
        <c:when test="${tokenValid}">
            <form method="post" action="${pageContext.request.contextPath}/auth/reset-password">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="token" value="${fn:escapeXml(token)}">
                <label>
                    New password
                    <input type="password" name="password" required autocomplete="new-password">
                </label>
                <label>
                    Confirm new password
                    <input type="password" name="confirmPassword" required autocomplete="new-password">
                </label>
                <button class="btn primary" type="submit">Update password</button>
            </form>
        </c:when>
        <c:otherwise>
            <div class="empty-state">This reset link is invalid, already used, or expired.</div>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
