<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container mt-4">
    <div class="row justify-content-center">
        <div class="col-lg-6">
            <div class="card shadow-sm border-danger">
                <div class="card-header bg-danger text-white">
                    <h2 class="h5 mb-0">Delete Note</h2>
                </div>
                <div class="card-body p-4">
                    <p class="mb-2">Are you sure you want to delete the following note?</p>
                    <blockquote class="blockquote border-start border-3 ps-3 my-3">
                        <p class="mb-0 fw-semibold"><c:out value="${note.title}"/></p>
                    </blockquote>
                    <p class="text-danger mb-4">
                        <strong>Warning:</strong> This action is permanent and cannot be undone.
                        All attachments and ratings associated with this note will also be deleted.
                    </p>

                    <div class="d-flex gap-3">
                        <form method="post" action="${pageContext.request.contextPath}/notes/${note.id}/delete">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <button type="submit" class="btn btn-danger">Yes, Delete Note</button>
                        </form>
                        <a href="${pageContext.request.contextPath}/notes/${note.id}" class="btn btn-outline-secondary">
                            Cancel
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
