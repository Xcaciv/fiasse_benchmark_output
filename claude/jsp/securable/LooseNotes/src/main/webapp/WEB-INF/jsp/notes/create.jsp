<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container mt-4">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="d-flex align-items-center mb-4">
                <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary btn-sm me-3">
                    &larr; Back
                </a>
                <h1 class="h2 mb-0">Create Note</h1>
            </div>

            <c:if test="${not empty error}">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <c:out value="${error}"/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            </c:if>

            <div class="card shadow-sm">
                <div class="card-body p-4">
                    <form method="post"
                          action="${pageContext.request.contextPath}/notes/create"
                          enctype="multipart/form-data"
                          novalidate>
                        <input type="hidden" name="_csrf" value="${csrfToken}">

                        <div class="mb-3">
                            <label for="title" class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                            <input type="text"
                                   id="title"
                                   name="title"
                                   class="form-control"
                                   value="<c:out value="${param.title}"/>"
                                   maxlength="255"
                                   required
                                   autofocus>
                            <div class="invalid-feedback">Title is required.</div>
                        </div>

                        <div class="mb-3">
                            <label for="content" class="form-label fw-semibold">Content <span class="text-danger">*</span></label>
                            <textarea id="content"
                                      name="content"
                                      class="form-control"
                                      rows="10"
                                      required><c:out value="${param.content}"/></textarea>
                            <div class="invalid-feedback">Content is required.</div>
                        </div>

                        <div class="mb-3">
                            <label for="visibility" class="form-label fw-semibold">Visibility</label>
                            <select id="visibility" name="visibility" class="form-select">
                                <option value="PRIVATE" ${param.visibility == 'PRIVATE' || empty param.visibility ? 'selected' : ''}>Private</option>
                                <option value="PUBLIC" ${param.visibility == 'PUBLIC' ? 'selected' : ''}>Public</option>
                            </select>
                            <div class="form-text">Private notes are only visible to you. Public notes can be seen by others.</div>
                        </div>

                        <div class="mb-4">
                            <label for="attachment" class="form-label fw-semibold">Attachment <span class="text-muted fw-normal">(optional)</span></label>
                            <input type="file"
                                   id="attachment"
                                   name="attachment"
                                   class="form-control"
                                   accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg">
                            <div class="form-text">Allowed file types: PDF, DOC, DOCX, TXT, PNG, JPG</div>
                        </div>

                        <div class="d-flex gap-2">
                            <button type="submit" class="btn btn-primary">Create Note</button>
                            <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary">Cancel</a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
