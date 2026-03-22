<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Edit Note - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h1 class="h3 mb-0">
                <i class="bi bi-pencil me-2"></i>Edit Note
            </h1>
            <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-1"></i>Back to Note
            </a>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="bi bi-exclamation-triangle me-2"></i>${error}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <div class="card shadow">
            <div class="card-body p-4">
                <form action="${pageContext.request.contextPath}/notes" method="post">
                    <input type="hidden" name="action" value="edit">
                    <input type="hidden" name="id" value="${note.id}">

                    <div class="mb-3">
                        <label for="title" class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control form-control-lg" id="title" name="title"
                               value="${note.title}"
                               required maxlength="255" autofocus>
                    </div>

                    <div class="mb-3">
                        <label for="content" class="form-label fw-semibold">Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content"
                                  rows="12" required>${note.content}</textarea>
                    </div>

                    <div class="mb-4">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="isPublic" name="isPublic"
                                   ${note.public ? 'checked' : ''}>
                            <label class="form-check-label" for="isPublic">
                                <i class="bi bi-globe me-1"></i>Make this note public
                                <small class="text-muted">(visible to all users and searchable)</small>
                            </label>
                        </div>
                    </div>

                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-save me-2"></i>Save Changes
                        </button>
                        <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}"
                           class="btn btn-outline-secondary">Cancel</a>
                    </div>
                </form>
            </div>
        </div>

        <div class="text-muted small mt-2">
            <i class="bi bi-clock me-1"></i>
            Last updated:
            <c:if test="${not empty note.updatedAt}">${note.updatedAt}</c:if>
        </div>
    </div>
</div>

<%@ include file="includes/footer.jsp" %>
