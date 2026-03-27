<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Edit Note" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="d-flex align-items-center mb-4">
            <a href="${pageContext.request.contextPath}/notes/${note.id}" class="btn btn-outline-secondary btn-sm me-3">
                <i class="bi bi-arrow-left"></i> Back
            </a>
            <h2 class="mb-0"><i class="bi bi-pencil text-primary"></i> Edit Note</h2>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> ${error}
            </div>
        </c:if>

        <div class="card shadow-sm mb-4">
            <div class="card-body p-4">
                <form method="post" action="${pageContext.request.contextPath}/notes/${note.id}/edit"
                      enctype="multipart/form-data">
                    <div class="mb-3">
                        <label for="title" class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="title" name="title"
                               value="${note.title}" required maxlength="255">
                    </div>
                    <div class="mb-3">
                        <label for="content" class="form-label fw-semibold">Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content" rows="12" required>${note.content}</textarea>
                    </div>
                    <div class="mb-3">
                        <label for="attachment" class="form-label fw-semibold">Add Attachment (optional)</label>
                        <input type="file" class="form-control" id="attachment" name="attachment"
                               accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg">
                        <div class="form-text">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Max 10 MB.</div>
                    </div>
                    <div class="mb-4">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="isPublic" name="isPublic"
                                   ${note.public ? 'checked' : ''}>
                            <label class="form-check-label" for="isPublic">
                                <i class="bi bi-globe"></i> Make this note public
                            </label>
                        </div>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-save"></i> Save Changes
                        </button>
                        <a href="${pageContext.request.contextPath}/notes/${note.id}" class="btn btn-outline-secondary">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>

        <!-- Current attachments -->
        <c:if test="${not empty attachments}">
            <div class="card shadow-sm">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-paperclip"></i> Current Attachments</h6>
                </div>
                <div class="list-group list-group-flush">
                    <c:forEach var="att" items="${attachments}">
                        <div class="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <i class="bi bi-file-earmark"></i>
                                <span class="ms-1">${att.originalFilename}</span>
                                <small class="text-muted ms-2">${att.fileSizeDisplay}</small>
                            </div>
                            <form method="post"
                                  action="${pageContext.request.contextPath}/attachments/${att.id}/delete"
                                  onsubmit="return confirm('Delete this attachment?')">
                                <button type="submit" class="btn btn-sm btn-outline-danger">
                                    <i class="bi bi-trash"></i> Remove
                                </button>
                            </form>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:if>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
