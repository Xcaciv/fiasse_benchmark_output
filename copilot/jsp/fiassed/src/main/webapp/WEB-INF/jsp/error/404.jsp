<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Not Found - Loose Notes"/>
</jsp:include>
<div class="error-page">
    <h2>404 - Not Found</h2>
    <p>The page or resource you requested could not be found.</p>
    <p class="correlation-id">Correlation ID: <%=java.util.UUID.randomUUID().toString().substring(0, 8)%></p>
    <a href="${pageContext.request.contextPath}/notes/list" class="btn">Go to My Notes</a>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
