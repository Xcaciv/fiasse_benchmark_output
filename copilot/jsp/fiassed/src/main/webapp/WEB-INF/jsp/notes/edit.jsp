<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Edit Note - Loose Notes"/>
</jsp:include>
<div class="note-form">
    <h2>Edit Note</h2>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/notes/edit">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="id" value="${note.id}">
        <div class="form-group">
            <label for="title">Title (required, max 200 chars)</label>
            <input type="text" id="title" name="title" required maxlength="200" value="<c:out value='${note.title}'/>">
        </div>
        <div class="form-group">
            <label for="content">Content (required, max 50,000 chars)</label>
            <textarea id="content" name="content" required maxlength="50000" rows="15"><c:out value="${note.content}"/></textarea>
        </div>
        <div class="form-group">
            <label for="visibility">Visibility</label>
            <select id="visibility" name="visibility">
                <option value="PRIVATE" ${note.visibility == 'PRIVATE' ? 'selected' : ''}>Private</option>
                <option value="PUBLIC" ${note.visibility == 'PUBLIC' ? 'selected' : ''}>Public</option>
            </select>
        </div>
        <button type="submit" class="btn btn-primary">Save Changes</button>
        <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}" class="btn">Cancel</a>
    </form>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
