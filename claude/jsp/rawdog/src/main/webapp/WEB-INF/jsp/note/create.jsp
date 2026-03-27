<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Create Note - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card shadow">
            <div class="card-header bg-dark text-white">
                <h4 class="mb-0"><i class="bi bi-plus-circle"></i> Create New Note</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i> ${error}</div>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/notes/create"
                      enctype="multipart/form-data">
                    <div class="mb-3">
                        <label for="title" class="form-label">Title <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="title" name="title"
                               value="${not empty title ? title : ''}" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label for="content" class="form-label">Content <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="content" name="content" rows="10" required>${not empty content ? content : ''}</textarea>
                    </div>
                    <div class="mb-3 form-check">
                        <input type="checkbox" class="form-check-input" id="isPublic" name="isPublic">
                        <label class="form-check-label" for="isPublic">Make note public</label>
                        <div class="form-text">Public notes can be seen by all users and appear in search results.</div>
                    </div>
                    <div class="mb-3">
                        <label for="attachment" class="form-label">Attachments</label>
                        <input type="file" class="form-control" id="attachment" name="attachment"
                               accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg" multiple>
                        <div class="form-text">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Max 10MB each.</div>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-save"></i> Create Note
                        </button>
                        <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary">
                            <i class="bi bi-x-lg"></i> Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
