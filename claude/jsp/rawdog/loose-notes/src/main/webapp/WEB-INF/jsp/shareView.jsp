<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="${note.title} - Shared Note - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="row">
    <div class="col-lg-8">
        <div class="alert alert-info">
            <i class="bi bi-share me-2"></i>
            This note was shared with you via a public link.
            <c:if test="${empty sessionScope.userId}">
                <a href="${pageContext.request.contextPath}/login" class="alert-link">Login</a>
                to rate or create your own notes.
            </c:if>
        </div>

        <!-- Note Card -->
        <div class="card shadow mb-4">
            <div class="card-header py-3">
                <h1 class="h3 mb-1">${note.title}</h1>
                <small class="text-muted">
                    By <strong>${note.authorUsername}</strong> &bull;
                    <c:if test="${not empty note.createdAt}">
                        ${note.createdAt.toString().substring(0, 10)}
                    </c:if>
                </small>
            </div>
            <div class="card-body">
                <div class="note-content">${note.content}</div>
            </div>
        </div>

        <!-- Ratings -->
        <c:if test="${not empty ratings}">
            <div class="card shadow mb-4">
                <div class="card-header">
                    <h5 class="mb-0">
                        <i class="bi bi-star me-2"></i>Ratings
                        <c:if test="${note.ratingCount > 0}">
                            <span class="badge bg-warning text-dark ms-2">
                                <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1" /> / 5
                                (${note.ratingCount})
                            </span>
                        </c:if>
                    </h5>
                </div>
                <div class="card-body">
                    <c:forEach var="rating" items="${ratings}">
                        <div class="rating-item mb-3 p-3 bg-light rounded">
                            <div class="d-flex justify-content-between">
                                <strong>${rating.username}</strong>
                                <div>
                                    <c:forEach begin="1" end="5" var="star">
                                        <i class="bi bi-star${star <= rating.rating ? '-fill' : ''} text-warning"></i>
                                    </c:forEach>
                                </div>
                            </div>
                            <c:if test="${not empty rating.comment}">
                                <p class="mt-2 mb-0 small">${rating.comment}</p>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:if>
    </div>

    <!-- Sidebar -->
    <div class="col-lg-4">
        <c:if test="${not empty attachments}">
            <div class="card shadow mb-4">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-paperclip me-2"></i>Attachments</h6>
                </div>
                <div class="card-body">
                    <ul class="list-unstyled mb-0">
                        <c:forEach var="attachment" items="${attachments}">
                            <li class="d-flex justify-content-between align-items-center mb-2">
                                <div class="d-flex align-items-center overflow-hidden">
                                    <i class="bi bi-file-earmark me-2 text-muted flex-shrink-0"></i>
                                    <div>
                                        <a href="${pageContext.request.contextPath}/attachments?action=download&id=${attachment.id}"
                                           class="small d-block text-truncate">
                                            ${attachment.originalFilename}
                                        </a>
                                        <small class="text-muted">${attachment.formattedFileSize}</small>
                                    </div>
                                </div>
                            </li>
                        </c:forEach>
                    </ul>
                </div>
            </div>
        </c:if>
    </div>
</div>

<%@ include file="includes/footer.jsp" %>
