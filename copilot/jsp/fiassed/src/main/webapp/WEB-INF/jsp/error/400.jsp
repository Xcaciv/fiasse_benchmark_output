<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Bad Request - Loose Notes"/>
</jsp:include>
<div class="error-page">
    <h2>400 - Bad Request</h2>
    <p>The request was invalid or malformed.</p>
    <p class="correlation-id">Reference ID: <%=java.util.UUID.randomUUID().toString().substring(0, 8)%></p>
    <a href="${pageContext.request.contextPath}/notes/list" class="btn">Go Home</a>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
