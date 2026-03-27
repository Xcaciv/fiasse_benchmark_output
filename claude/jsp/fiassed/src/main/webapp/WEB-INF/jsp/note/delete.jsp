<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/layout/header.jsp" %>

<div class="page-header">
    <h1>Delete Note</h1>
</div>

<div class="card">
    <h2>Are you sure you want to delete this note?</h2>

    <p class="note-title-display">
        <strong>Note:</strong> <c:out value="${note.title}"/>
    </p>

    <div class="alert alert-error">
        <strong>Warning:</strong> This action is permanent and will also delete all attachments, ratings, and share links.
    </div>

    <div class="form-actions">
        <form method="post" action="${pageContext.request.contextPath}/notes/${note.id}/delete">
            <input type="hidden" name="_csrf" value="${csrfToken}"/>
            <button type="submit" class="btn btn-danger">Yes, Delete Permanently</button>
        </form>
        <a href="${pageContext.request.contextPath}/notes/${note.id}" class="btn btn-secondary">Cancel</a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jsp" %>
