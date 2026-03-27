<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="My Notes - Loose Notes"/>
</jsp:include>
<div class="page-header">
    <h2>My Notes</h2>
    <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">+ New Note</a>
</div>
<c:if test="${empty notes}">
    <div class="empty-state">
        <p>You don't have any notes yet. <a href="${pageContext.request.contextPath}/notes/create">Create your first note!</a></p>
    </div>
</c:if>
<c:if test="${not empty notes}">
<div class="notes-grid">
    <c:forEach var="note" items="${notes}">
    <div class="note-card">
        <div class="note-card-header">
            <h3><a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"><c:out value="${note.title}"/></a></h3>
            <span class="badge badge-${note.visibility == 'PUBLIC' ? 'success' : 'secondary'}">
                <c:out value="${note.visibility}"/>
            </span>
        </div>
        <p class="note-preview"><c:out value="${note.content.length() > 150 ? note.content.substring(0, 150).concat('...') : note.content}"/></p>
        <div class="note-meta">
            <span>Updated: ${note.updatedAt}</span>
            <div class="note-actions">
                <a href="${pageContext.request.contextPath}/notes/edit?id=${note.id}" class="btn btn-sm">Edit</a>
                <form method="post" action="${pageContext.request.contextPath}/notes/delete" style="display:inline"
                      onsubmit="return confirmDelete()">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="id" value="${note.id}">
                    <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                </form>
            </div>
        </div>
    </div>
    </c:forEach>
</div>
<div class="pagination">
    <c:if test="${currentPage > 1}">
        <a href="?page=${currentPage - 1}" class="btn btn-sm">&laquo; Prev</a>
    </c:if>
    <span>Page ${currentPage} of ${totalPages}</span>
    <c:if test="${currentPage < totalPages}">
        <a href="?page=${currentPage + 1}" class="btn btn-sm">Next &raquo;</a>
    </c:if>
</div>
</c:if>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
