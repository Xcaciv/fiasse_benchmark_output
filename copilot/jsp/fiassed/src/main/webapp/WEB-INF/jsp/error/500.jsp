<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ page import="java.util.UUID" %>
<%
    String correlationId = UUID.randomUUID().toString().substring(0, 8);
    // Log full error server-side (never expose stack trace to user)
    if (exception != null) {
        org.slf4j.LoggerFactory.getLogger("ErrorPage").error("Unhandled error [correlationId={}]", correlationId, exception);
    }
%>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Server Error - Loose Notes"/>
</jsp:include>
<div class="error-page">
    <h2>500 - Internal Server Error</h2>
    <p>Something went wrong on our end. Please try again later.</p>
    <p>Reference ID: <strong><%= correlationId %></strong></p>
    <a href="${pageContext.request.contextPath}/notes/list" class="btn">Go to My Notes</a>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
