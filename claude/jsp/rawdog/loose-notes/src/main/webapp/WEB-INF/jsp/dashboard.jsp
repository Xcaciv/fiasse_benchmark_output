<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Dashboard - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h1 class="h3 mb-0">
        <i class="bi bi-house me-2"></i>My Notes
    </h1>
    <a href="${pageContext.request.contextPath}/notes?action=create" class="btn btn-primary">
        <i class="bi bi-plus-circle me-2"></i>New Note
    </a>
</div>

<c:if test="${not empty param.success}">
    <div class="alert alert-success alert-dismissible fade show" role="alert">
        <i class="bi bi-check-circle me-2"></i>${param.success}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<c:if test="${not empty param.error}">
    <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle me-2"></i>${param.error}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<c:choose>
    <c:when test="${empty notes}">
        <div class="text-center py-5">
            <i class="bi bi-journal-x display-1 text-muted"></i>
            <h4 class="mt-3 text-muted">No notes yet</h4>
            <p class="text-muted">Create your first note to get started!</p>
            <a href="${pageContext.request.contextPath}/notes?action=create" class="btn btn-primary btn-lg">
                <i class="bi bi-plus-circle me-2"></i>Create Note
            </a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
            <c:forEach var="note" items="${notes}">
                <div class="col">
                    <div class="card h-100 note-card">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <h5 class="card-title mb-0">
                                    <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}"
                                       class="text-decoration-none text-dark">${note.title}</a>
                                </h5>
                                <c:choose>
                                    <c:when test="${note.public}">
                                        <span class="badge bg-success ms-2">Public</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary ms-2">Private</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <p class="card-text text-muted small note-preview">
                                <c:choose>
                                    <c:when test="${note.content.length() > 150}">
                                        ${note.content.substring(0, 150)}...
                                    </c:when>
                                    <c:otherwise>
                                        ${note.content}
                                    </c:otherwise>
                                </c:choose>
                            </p>
                        </div>
                        <div class="card-footer bg-transparent">
                            <div class="d-flex justify-content-between align-items-center">
                                <small class="text-muted">
                                    <i class="bi bi-clock me-1"></i>
                                    <c:if test="${not empty note.updatedAt}">
                                        ${note.updatedAt.toString().substring(0, 10)}
                                    </c:if>
                                </small>
                                <div class="btn-group btn-group-sm">
                                    <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}"
                                       class="btn btn-outline-primary" title="View">
                                        <i class="bi bi-eye"></i>
                                    </a>
                                    <a href="${pageContext.request.contextPath}/notes?action=edit&id=${note.id}"
                                       class="btn btn-outline-secondary" title="Edit">
                                        <i class="bi bi-pencil"></i>
                                    </a>
                                    <button type="button" class="btn btn-outline-danger"
                                            title="Delete"
                                            onclick="confirmDelete(${note.id}, '${note.title.replace("'", "\\'")}')">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>

        <div class="mt-3 text-muted small">
            Showing ${notes.size()} note<c:if test="${notes.size() != 1}">s</c:if>
        </div>
    </c:otherwise>
</c:choose>

<!-- Delete Confirmation Modal -->
<div class="modal fade" id="deleteModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Confirm Delete</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p>Are you sure you want to delete the note "<strong id="deleteNoteTitle"></strong>"?</p>
                <p class="text-danger small">This will also delete all attachments, ratings, and share links.</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <form id="deleteForm" action="${pageContext.request.contextPath}/notes" method="post">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" id="deleteNoteId">
                    <button type="submit" class="btn btn-danger">
                        <i class="bi bi-trash me-2"></i>Delete
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
function confirmDelete(noteId, noteTitle) {
    document.getElementById('deleteNoteId').value = noteId;
    document.getElementById('deleteNoteTitle').textContent = noteTitle;
    new bootstrap.Modal(document.getElementById('deleteModal')).show();
}
</script>

<%@ include file="includes/footer.jsp" %>
