<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Create Note - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h1 class="h3 mb-0">
                <i class="bi bi-plus-circle me-2"></i>Create New Note
            </h1>
            <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-1"></i>Back
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
                    <input type="hidden" name="action" value="create">

                    <div class="mb-3">
                        <label for="title" class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control form-control-lg" id="title" name="title"
                               value="${not empty title ? title : ''}"
                               required maxlength="255" placeholder="Enter note title..." autofocus>
                    </div>

                    <div class="mb-3">
                        <label for="content" class="form-label fw-semibold">Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content"
                                  rows="12" required placeholder="Write your note here...">${not empty content ? content : ''}</textarea>
                    </div>

                    <div class="mb-4">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="isPublic" name="isPublic"
                                   ${isPublic ? 'checked' : ''}>
                            <label class="form-check-label" for="isPublic">
                                <i class="bi bi-globe me-1"></i>Make this note public
                                <small class="text-muted">(visible to all users and searchable)</small>
                            </label>
                        </div>
                    </div>

                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-save me-2"></i>Create Note
                        </button>
                        <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-outline-secondary">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="includes/footer.jsp" %>
