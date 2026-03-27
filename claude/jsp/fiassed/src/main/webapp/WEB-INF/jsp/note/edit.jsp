<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Edit Note</h1>
</div>

<form method="post" action="${pageContext.request.contextPath}/notes/${note.id}/edit" novalidate>
    <input type="hidden" name="_csrf" value="${csrfToken}"/>

    <div class="form-group">
        <label for="title">Title</label>
        <input type="text"
               id="title"
               name="title"
               required
               maxlength="500"
               value="${fn:escapeXml(note.title)}"/>
    </div>

    <div class="form-group">
        <label for="content">Content</label>
        <textarea id="content"
                  name="content"
                  rows="16">${fn:escapeXml(note.content)}</textarea>
    </div>

    <div class="form-group">
        <label for="visibility">Visibility</label>
        <select id="visibility" name="visibility">
            <option value="PRIVATE" ${note.visibility == 'PRIVATE' ? 'selected' : ''}>Private</option>
            <option value="PUBLIC"  ${note.visibility == 'PUBLIC'  ? 'selected' : ''}>Public</option>
        </select>
    </div>

    <div class="form-actions">
        <button type="submit" class="btn btn-primary">Save Changes</button>
        <a href="${pageContext.request.contextPath}/notes/${note.id}" class="btn btn-secondary">Cancel</a>
    </div>
</form>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
