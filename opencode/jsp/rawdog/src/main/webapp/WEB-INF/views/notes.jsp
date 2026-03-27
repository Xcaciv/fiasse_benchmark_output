<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h2><i class="fas fa-file-alt"></i> My Notes</h2>
    <a href="${pageContext.request.contextPath}/notes?action=create" class="btn btn-primary">
        <i class="fas fa-plus"></i> Create Note
    </a>
</div>

<c:if test="${empty notes}">
    <div class="alert alert-info text-center">
        <i class="fas fa-info-circle fa-2x mb-3"></i>
        <h4>No notes yet</h4>
        <p>Start by creating your first note!</p>
        <a href="${pageContext.request.contextPath}/notes?action=create" class="btn btn-primary">
            <i class="fas fa-plus"></i> Create Note
        </a>
    </div>
</c:if>

<c:if test="${not empty notes}">
    <div class="row">
        <c:forEach var="note" items="${notes}">
            <div class="col-md-6 mb-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start">
                            <h5 class="card-title">${note.title}</h5>
                            <span class="badge badge-${note.isPublic ? 'success' : 'secondary'}">
                                ${note.isPublic ? 'Public' : 'Private'}
                            </span>
                        </div>
                        <p class="card-text text-muted">${note.excerpt}</p>
                        <div class="d-flex justify-content-between align-items-center">
                            <small class="text-muted">
                                <c:if test="${note.ratingCount > 0}">
                                    <span class="text-warning">
                                        <i class="fas fa-star"></i> ${String.format("%.1f", note.averageRating)} (${note.ratingCount})
                                    </span>
                                </c:if>
                            </small>
                            <small class="text-muted">
                                <i class="fas fa-paperclip"></i> ${note.attachments.size()}
                            </small>
                        </div>
                    </div>
                    <div class="card-footer bg-white">
                        <div class="btn-group btn-group-sm w-100">
                            <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-outline-primary">
                                <i class="fas fa-eye"></i> View
                            </a>
                            <a href="${pageContext.request.contextPath}/notes?action=edit&id=${note.id}" class="btn btn-outline-secondary">
                                <i class="fas fa-edit"></i> Edit
                            </a>
                            <a href="${pageContext.request.contextPath}/notes?action=delete&id=${note.id}" class="btn btn-outline-danger">
                                <i class="fas fa-trash"></i> Delete
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
</c:if>

<jsp:include page="footer.jsp"/>
