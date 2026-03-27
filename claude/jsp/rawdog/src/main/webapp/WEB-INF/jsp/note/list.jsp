<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="My Notes - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="bi bi-journals"></i> My Notes</h2>
    <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">
        <i class="bi bi-plus-lg"></i> New Note
    </a>
</div>

<c:choose>
    <c:when test="${empty notes}">
        <div class="text-center py-5">
            <i class="bi bi-journal-x display-1 text-muted"></i>
            <h4 class="text-muted mt-3">No notes yet</h4>
            <p class="text-muted">Create your first note to get started!</p>
            <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">
                <i class="bi bi-plus-lg"></i> Create Note
            </a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
            <c:forEach var="note" items="${notes}">
                <div class="col">
                    <div class="card h-100 shadow-sm">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <h5 class="card-title mb-0">
                                    <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"
                                       class="text-decoration-none text-dark">
                                        ${note.title}
                                    </a>
                                </h5>
                                <span class="badge ${note.public ? 'bg-success' : 'bg-secondary'} ms-1">
                                    ${note.public ? 'Public' : 'Private'}
                                </span>
                            </div>
                            <p class="card-text text-muted small">
                                <c:out value="${note.content.length() > 100 ? note.content.substring(0, 100).concat('...') : note.content}"/>
                            </p>
                        </div>
                        <div class="card-footer text-muted small d-flex justify-content-between align-items-center">
                            <span>
                                <i class="bi bi-star-fill text-warning"></i>
                                <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1" minFractionDigits="1"/>
                                (${note.ratingCount})
                            </span>
                            <span>${note.updatedAt}</span>
                        </div>
                        <div class="card-footer bg-transparent d-flex gap-2">
                            <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"
                               class="btn btn-sm btn-outline-primary flex-fill">
                                <i class="bi bi-eye"></i> View
                            </a>
                            <a href="${pageContext.request.contextPath}/notes/edit?id=${note.id}"
                               class="btn btn-sm btn-outline-secondary flex-fill">
                                <i class="bi bi-pencil"></i> Edit
                            </a>
                            <a href="${pageContext.request.contextPath}/notes/delete?id=${note.id}"
                               class="btn btn-sm btn-outline-danger flex-fill">
                                <i class="bi bi-trash"></i> Delete
                            </a>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../layout/footer.jsp" %>
