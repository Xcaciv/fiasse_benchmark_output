<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Top Rated Notes" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<h2 class="mb-4"><i class="bi bi-trophy text-warning"></i> Top Rated Notes</h2>
<p class="text-muted mb-4">Public notes with at least 3 ratings, sorted by average rating.</p>

<c:choose>
    <c:when test="${empty notes}">
        <div class="text-center py-5 text-muted">
            <i class="bi bi-star" style="font-size: 3rem;"></i>
            <h5 class="mt-3">No top rated notes yet</h5>
            <p>Notes need at least 3 ratings to appear here. Start rating!</p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="list-group shadow-sm">
            <c:forEach var="note" items="${notes}" varStatus="status">
                <a href="${pageContext.request.contextPath}/notes/${note.id}"
                   class="list-group-item list-group-item-action">
                    <div class="d-flex align-items-center">
                        <div class="me-3 text-center" style="min-width: 40px;">
                            <span class="fw-bold text-muted fs-5">#${status.index + 1}</span>
                        </div>
                        <div class="flex-grow-1">
                            <div class="d-flex justify-content-between align-items-start">
                                <div>
                                    <h6 class="mb-1">${note.title}</h6>
                                    <p class="mb-1 text-muted small">${note.excerpt}</p>
                                    <small class="text-muted">By ${note.ownerUsername}</small>
                                </div>
                                <div class="ms-3 text-end">
                                    <div class="text-warning fs-5">
                                        <c:choose>
                                            <c:when test="${note.averageRating >= 4.5}"><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i></c:when>
                                            <c:when test="${note.averageRating >= 3.5}"><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star text-muted"></i></c:when>
                                            <c:when test="${note.averageRating >= 2.5}"><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star text-muted"></i><i class="bi bi-star text-muted"></i></c:when>
                                            <c:when test="${note.averageRating >= 1.5}"><i class="bi bi-star-fill"></i><i class="bi bi-star-fill"></i><i class="bi bi-star text-muted"></i><i class="bi bi-star text-muted"></i><i class="bi bi-star text-muted"></i></c:when>
                                            <c:otherwise><i class="bi bi-star-fill"></i><i class="bi bi-star text-muted"></i><i class="bi bi-star text-muted"></i><i class="bi bi-star text-muted"></i><i class="bi bi-star text-muted"></i></c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="fw-bold text-warning">
                                        ${note.averageRating}
                                        <small class="text-muted fw-normal">/ 5</small>
                                    </div>
                                    <small class="text-muted">${note.ratingCount} ratings</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </a>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../shared/footer.jsp" %>
