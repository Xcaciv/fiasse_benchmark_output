<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Edit Note - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card shadow">
            <div class="card-header bg-dark text-white">
                <h4 class="mb-0"><i class="bi bi-pencil-square"></i> Edit Note</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i> ${error}</div>
                </c:if>
                <form method="post"
                      action="${pageContext.request.contextPath}/notes/edit?id=${note.id}"
                      enctype="multipart/form-data">
                    <div class="mb-3">
                        <label for="title" class="form-label">Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="title" name="title"
                               value="${note.title}" required>
                    </div>
                    <div class="mb-3">
                        <label for="content" class="form-label">Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content" rows="10" required><c:out value="${note.content}"/></textarea>
                    </div>
                    <div class="mb-3 form-check">
                        <input type="checkbox" class="form-check-input" id="isPublic" name="isPublic"
                               ${note.public ? 'checked' : ''}>
                        <label class="form-check-label" for="isPublic">Make note public</label>
                    </div>

                    <c:if test="${not empty attachments}">
                        <div class="mb-3">
                            <label class="form-label">Current Attachments</label>
                            <ul class="list-group">
                                <c:forEach var="att" items="${attachments}">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span>
                                            <i class="bi bi-paperclip"></i>
                                            <a href="${pageContext.request.contextPath}/files/${att.id}">${att.originalFilename}</a>
                                            <small class="text-muted ms-2">${att.fileSizeDisplay}</small>
                                        </span>
                                        <div class="form-check">
                                            <input class="form-check-input" type="checkbox"
                                                   name="deleteAttachment" value="${att.id}"
                                                   id="del_${att.id}">
                                            <label class="form-check-label text-danger" for="del_${att.id}">Delete</label>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>

                    <div class="mb-3">
                        <label for="attachment" class="form-label">Add Attachments</label>
                        <input type="file" class="form-control" id="attachment" name="attachment"
                               accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg" multiple>
                        <div class="form-text">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Max 10MB each.</div>
                    </div>

                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-save"></i> Save Changes
                        </button>
                        <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"
                           class="btn btn-outline-secondary">
                            <i class="bi bi-x-lg"></i> Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
