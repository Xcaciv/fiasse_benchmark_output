<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Create Note" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="d-flex align-items-center mb-4">
            <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary btn-sm me-3">
                <i class="bi bi-arrow-left"></i> Back
            </a>
            <h2 class="mb-0"><i class="bi bi-plus-circle text-primary"></i> New Note</h2>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> ${error}
            </div>
        </c:if>

        <div class="card shadow-sm">
            <div class="card-body p-4">
                <form method="post" action="${pageContext.request.contextPath}/notes/create"
                      enctype="multipart/form-data">
                    <div class="mb-3">
                        <label for="title" class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="title" name="title"
                               value="${param.title}" required maxlength="255" placeholder="Note title...">
                    </div>
                    <div class="mb-3">
                        <label for="content" class="form-label fw-semibold">Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content" rows="12"
                                  required placeholder="Write your note here...">${param.content}</textarea>
                    </div>
                    <div class="mb-3">
                        <label for="attachment" class="form-label fw-semibold">Attachment (optional)</label>
                        <input type="file" class="form-control" id="attachment" name="attachment"
                               accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg">
                        <div class="form-text">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Max 10 MB.</div>
                    </div>
                    <div class="mb-4">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="isPublic" name="isPublic">
                            <label class="form-check-label" for="isPublic">
                                <i class="bi bi-globe"></i> Make this note public
                            </label>
                        </div>
                        <div class="form-text">Public notes appear in search results and can be viewed by anyone.</div>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-save"></i> Create Note
                        </button>
                        <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
