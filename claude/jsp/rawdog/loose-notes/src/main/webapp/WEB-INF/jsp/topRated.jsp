<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Top Rated Notes - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<h1 class="h3 mb-4">
    <i class="bi bi-trophy me-2 text-warning"></i>Top Rated Notes
</h1>

<c:choose>
    <c:when test="${empty notes}">
        <div class="text-center py-5">
            <i class="bi bi-star display-1 text-muted"></i>
            <h4 class="mt-3 text-muted">No rated notes yet</h4>
            <p class="text-muted">Be the first to rate a public note!</p>
            <a href="${pageContext.request.contextPath}/search" class="btn btn-primary">
                Browse Notes
            </a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
            <c:forEach var="note" items="${notes}" varStatus="status">
                <div class="col">
                    <div class="card h-100 note-card">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <span class="badge bg-warning text-dark fs-6">#${status.index + 1}</span>
                                <div class="text-end">
                                    <c:forEach begin="1" end="5" var="star">
                                        <i class="bi bi-star${star <= note.averageRating ? '-fill' : ''} text-warning small"></i>
                                    </c:forEach>
                                    <br>
                                    <small class="text-muted">
                                        <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1" />/5
                                        (${note.ratingCount})
                                    </small>
                                </div>
                            </div>
                            <h5 class="card-title">
                                <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}"
                                   class="text-decoration-none text-dark">${note.title}</a>
                            </h5>
                            <p class="card-text text-muted small">
                                By <strong>${note.authorUsername}</strong>
                            </p>
                            <p class="card-text text-muted small note-preview">
                                <c:choose>
                                    <c:when test="${note.content.length() > 120}">
                                        ${note.content.substring(0, 120)}...
                                    </c:when>
                                    <c:otherwise>
                                        ${note.content}
                                    </c:otherwise>
                                </c:choose>
                            </p>
                        </div>
                        <div class="card-footer bg-transparent">
                            <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}"
                               class="btn btn-sm btn-outline-primary w-100">
                                <i class="bi bi-eye me-1"></i>View Note
                            </a>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="includes/footer.jsp" %>
