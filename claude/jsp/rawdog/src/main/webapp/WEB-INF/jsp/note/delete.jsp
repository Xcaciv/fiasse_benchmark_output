<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Delete Note - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow border-danger">
            <div class="card-header bg-danger text-white">
                <h4 class="mb-0"><i class="bi bi-trash"></i> Delete Note</h4>
            </div>
            <div class="card-body">
                <p>Are you sure you want to delete this note?</p>
                <div class="card bg-light mb-3">
                    <div class="card-body">
                        <h5>${note.title}</h5>
                        <p class="text-muted small mb-0">
                            <c:out value="${note.content.length() > 100 ? note.content.substring(0, 100).concat('...') : note.content}"/>
                        </p>
                    </div>
                </div>
                <div class="alert alert-warning">
                    <i class="bi bi-exclamation-triangle"></i>
                    This will also delete all attachments and ratings for this note. This action cannot be undone.
                </div>
                <form method="post" action="${pageContext.request.contextPath}/notes/delete">
                    <input type="hidden" name="id" value="${note.id}">
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-danger">
                            <i class="bi bi-trash"></i> Yes, Delete
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
