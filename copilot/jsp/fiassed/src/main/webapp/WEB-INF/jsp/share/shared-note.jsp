<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Shared Note - Loose Notes"/>
</jsp:include>
<div class="note-view shared-note">
    <div class="shared-badge">🔗 Shared Note</div>
    <h2><c:out value="${note.title}"/></h2>
    <div class="note-meta">
        <span>By <c:out value="${note.ownerUsername}"/></span>
        <span>Created: ${note.createdAt}</span>
    </div>
    <div class="note-content"><c:out value="${note.content}"/></div>
    <c:if test="${not empty attachments}">
    <div class="attachments-section">
        <h3>Attachments</h3>
        <ul class="attachment-list">
            <c:forEach var="att" items="${attachments}">
            <li><a href="${pageContext.request.contextPath}/attachment/download?id=${att.id}"><c:out value="${att.originalFilename}"/></a></li>
            </c:forEach>
        </ul>
    </div>
    </c:if>
    <div class="shared-footer">
        <a href="${pageContext.request.contextPath}/auth/register" class="btn btn-primary">Create Your Own Notes</a>
    </div>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
