<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card" style="max-width:700px; margin:0 auto; text-align:center;">
    <h1>Something went wrong</h1>
    <p class="muted">The request could not be completed securely.</p>
    <p>Status code: <strong>${requestScope['javax.servlet.error.status_code']}</strong></p>
    <p class="muted">Request: <c:out value="${requestScope['javax.servlet.error.request_uri']}" /></p>
    <div class="actions" style="justify-content:center;">
        <a class="btn primary" href="${pageContext.request.contextPath}/">Return home</a>
    </div>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
