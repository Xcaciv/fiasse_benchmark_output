<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="${empty note ? 'New Note' : 'Edit Note'}"/>
</jsp:include>

<h1 class="h3 mb-4">${empty note ? 'New Note' : 'Edit Note'}</h1>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>
<c:if test="${param.shared == '1'}">
    <div class="alert alert-success">Share link generated!</div>
</c:if>
<c:if test="${param.revoked == '1'}">
    <div class="alert alert-info">Share link revoked.</div>
</c:if>

<form method="post"
      action="${pageContext.request.contextPath}/notes/${empty note ? 'new' : note.id.concat('/edit')}">
    <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
    <div class="mb-3">
        <label for="title" class="form-label fw-bold">Title <span class="text-danger">*</span></label>
        <input type="text" id="title" name="title" class="form-control"
               maxlength="200" required
               value="<c:out value='${empty note ? param.title : note.title}'/>">
    </div>
    <div class="mb-3">
        <label for="content" class="form-label fw-bold">Content <span class="text-danger">*</span></label>
        <textarea id="content" name="content" class="form-control" rows="12" required><c:out value="${empty note ? param.content : note.content}"/></textarea>
    </div>
    <div class="mb-3">
        <label class="form-label fw-bold">Visibility</label>
        <div>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="visibility" id="visPrivate"
                       value="PRIVATE"
                       ${empty note || note.visibility == 'PRIVATE' ? 'checked' : ''}>
                <label class="form-check-label" for="visPrivate">🔒 Private</label>
            </div>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="visibility" id="visPublic"
                       value="PUBLIC"
                       ${not empty note && note.visibility == 'PUBLIC' ? 'checked' : ''}>
                <label class="form-check-label" for="visPublic">🌍 Public</label>
            </div>
        </div>
    </div>
    <div class="d-flex gap-2">
        <button type="submit" class="btn btn-primary">${empty note ? 'Create Note' : 'Save Changes'}</button>
        <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary">Cancel</a>
        <c:if test="${not empty note}">
            <button type="button" class="btn btn-danger ms-auto"
                    onclick="document.getElementById('deleteForm').submit()">Delete Note</button>
        </c:if>
    </div>
</form>

<c:if test="${not empty note}">
<form id="deleteForm" method="post"
      action="${pageContext.request.contextPath}/notes/${note.id}/delete"
      onsubmit="return confirm('Delete this note and all attachments permanently?')">
    <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
</form>

<!-- Attachments -->
<hr class="mt-5">
<h4>Attachments</h4>
<c:if test="${not empty attachments}">
<ul class="list-group mb-3">
    <c:forEach var="a" items="${attachments}">
        <li class="list-group-item d-flex justify-content-between align-items-center">
            <a href="${pageContext.request.contextPath}/attachments/${a.id}/download">
                <c:out value="${a.originalFilename}"/>
            </a>
            <form method="post"
                  action="${pageContext.request.contextPath}/attachments/${a.id}/delete"
                  class="d-inline"
                  onsubmit="return confirm('Delete this attachment?')">
                <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
                <button type="submit" class="btn btn-sm btn-outline-danger">Remove</button>
            </form>
        </li>
    </c:forEach>
</ul>
</c:if>
<form method="post" enctype="multipart/form-data"
      action="${pageContext.request.contextPath}/attachments/upload">
    <input type="hidden" name="noteId" value="${note.id}">
    <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
    <div class="input-group">
        <input type="file" name="file" class="form-control"
               accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg" required>
        <button type="submit" class="btn btn-outline-primary">Upload</button>
    </div>
    <div class="form-text">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG (max 10 MB)</div>
</form>

<!-- Share Link -->
<hr class="mt-4">
<h4>Share Link</h4>
<c:choose>
    <c:when test="${not empty shareLink}">
        <div class="input-group mb-2">
            <input type="text" class="form-control" readonly
                   value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share/${shareLink.token}">
            <button class="btn btn-outline-secondary" onclick="navigator.clipboard.writeText(this.previousElementSibling.value)">Copy</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/share/revoke">
            <input type="hidden" name="noteId" value="${note.id}">
            <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
            <button type="submit" class="btn btn-sm btn-outline-danger">Revoke Link</button>
        </form>
    </c:when>
    <c:otherwise>
        <p class="text-muted">No share link generated yet.</p>
        <form method="post" action="${pageContext.request.contextPath}/share/generate">
            <input type="hidden" name="noteId" value="${note.id}">
            <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
            <button type="submit" class="btn btn-sm btn-outline-primary">Generate Share Link</button>
        </form>
    </c:otherwise>
</c:choose>
</c:if>

<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
