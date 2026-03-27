<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Top Rated Notes - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<h2 class="mb-4"><i class="bi bi-trophy"></i> Top Rated Notes</h2>
<p class="text-muted">Public notes with at least 3 ratings, sorted by average rating.</p>

<c:choose>
    <c:when test="${empty notes}">
        <div class="text-center py-5">
            <i class="bi bi-star display-1 text-muted"></i>
            <h4 class="text-muted mt-3">No top rated notes yet</h4>
            <p class="text-muted">Notes need at least 3 ratings to appear here.</p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row row-cols-1 row-cols-md-2 g-4">
            <c:forEach var="note" items="${notes}" varStatus="status">
                <div class="col">
                    <div class="card h-100 shadow-sm">
                        <div class="card-body">
                            <div class="d-flex align-items-center mb-2">
                                <span class="badge bg-warning text-dark me-2 fs-6">#${status.index + 1}</span>
                                <h5 class="card-title mb-0">
                                    <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"
                                       class="text-decoration-none text-dark">
                                        ${note.title}
                                    </a>
                                </h5>
                            </div>
                            <p class="card-text text-muted small">
                                <c:out value="${note.content.length() > 120 ? note.content.substring(0, 120).concat('...') : note.content}"/>
                            </p>
                        </div>
                        <div class="card-footer d-flex justify-content-between align-items-center">
                            <div>
                                <c:forEach var="s" begin="1" end="5">
                                    <c:choose>
                                        <c:when test="${s <= note.averageRating}">
                                            <span class="text-warning">&#9733;</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">&#9734;</span>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                                <strong class="ms-1">
                                    <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1" minFractionDigits="1"/>
                                </strong>
                                <small class="text-muted">(${note.ratingCount} ratings)</small>
                            </div>
                            <small class="text-muted">by ${note.ownerUsername}</small>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../layout/footer.jsp" %>
