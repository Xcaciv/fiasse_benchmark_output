<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Delete Note" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow-sm border-danger">
            <div class="card-header bg-danger text-white">
                <h5 class="mb-0"><i class="bi bi-trash"></i> Delete Note</h5>
            </div>
            <div class="card-body p-4">
                <p class="mb-3">Are you sure you want to delete the following note?</p>
                <div class="alert alert-secondary">
                    <strong>${note.title}</strong>
                    <p class="mb-0 mt-1 text-muted small">${note.excerpt}</p>
                </div>
                <div class="alert alert-warning">
                    <i class="bi bi-exclamation-triangle"></i>
                    This will permanently delete the note, all attachments, ratings, and share links.
                    <strong>This action cannot be undone.</strong>
                </div>
                <div class="d-flex gap-2">
                    <form method="post" action="${pageContext.request.contextPath}/notes/${note.id}/delete">
                        <button type="submit" class="btn btn-danger">
                            <i class="bi bi-trash"></i> Yes, Delete
                        </button>
                    </form>
                    <a href="${pageContext.request.contextPath}/notes/${note.id}" class="btn btn-outline-secondary">
                        Cancel
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
