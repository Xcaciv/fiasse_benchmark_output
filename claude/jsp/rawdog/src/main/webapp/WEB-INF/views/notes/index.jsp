<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="My Notes" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="bi bi-files text-primary"></i> My Notes</h2>
    <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">
        <i class="bi bi-plus-circle"></i> New Note
    </a>
</div>

<c:if test="${not empty param.success}">
    <div class="alert alert-success alert-dismissible fade show">
        <i class="bi bi-check-circle"></i> Note created successfully.
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<c:choose>
    <c:when test="${empty notes}">
        <div class="text-center py-5">
            <i class="bi bi-journal-x text-muted" style="font-size: 3rem;"></i>
            <h5 class="mt-3 text-muted">No notes yet</h5>
            <p class="text-muted">Create your first note to get started!</p>
            <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">
                <i class="bi bi-plus-circle"></i> Create Note
            </a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row g-3">
            <c:forEach var="note" items="${notes}">
                <div class="col-md-6 col-lg-4">
                    <div class="card h-100 shadow-sm note-card">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <h5 class="card-title mb-0">
                                    <a href="${pageContext.request.contextPath}/notes/${note.id}"
                                       class="text-decoration-none">${note.title}</a>
                                </h5>
                                <c:choose>
                                    <c:when test="${note.public}">
                                        <span class="badge bg-success"><i class="bi bi-globe"></i> Public</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary"><i class="bi bi-lock"></i> Private</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <p class="card-text text-muted small">${note.excerpt}</p>
                        </div>
                        <div class="card-footer bg-transparent d-flex justify-content-between align-items-center">
                            <small class="text-muted">
                                <c:if test="${note.ratingCount > 0}">
                                    <i class="bi bi-star-fill text-warning"></i>
                                    ${note.averageRating}
                                    (${note.ratingCount})
                                </c:if>
                            </small>
                            <div class="btn-group btn-group-sm">
                                <a href="${pageContext.request.contextPath}/notes/${note.id}"
                                   class="btn btn-outline-primary" title="View">
                                    <i class="bi bi-eye"></i>
                                </a>
                                <a href="${pageContext.request.contextPath}/notes/${note.id}/edit"
                                   class="btn btn-outline-secondary" title="Edit">
                                    <i class="bi bi-pencil"></i>
                                </a>
                                <a href="${pageContext.request.contextPath}/notes/${note.id}/delete"
                                   class="btn btn-outline-danger" title="Delete">
                                    <i class="bi bi-trash"></i>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../shared/footer.jsp" %>
