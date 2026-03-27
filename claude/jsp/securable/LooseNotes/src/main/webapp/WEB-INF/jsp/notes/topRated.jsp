<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Top Rated Notes - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h1 class="h3 mb-4">Top Rated Notes</h1>
<p class="text-muted">Public notes with at least 3 ratings, sorted by average rating.</p>

<c:choose>
    <c:when test="${empty notes}">
        <p class="text-muted">No top-rated notes yet. Be the first to rate some notes!</p>
    </c:when>
    <c:otherwise>
        <div class="list-group">
            <c:forEach var="note" items="${notes}" varStatus="loop">
                <a href="${pageContext.request.contextPath}/notes/view/<c:out value='${note.id}'/>"
                   class="list-group-item list-group-item-action">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <span class="badge bg-secondary me-2">#${loop.index + 1}</span>
                            <strong><c:out value="${note.title}"/></strong>
                            <small class="text-muted ms-2">by <c:out value="${note.ownerUsername}"/></small>
                        </div>
                        <div class="text-warning">
                            &#9733; <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1"/>
                            <small class="text-muted">(<c:out value="${note.ratingCount}"/>)</small>
                        </div>
                    </div>
                    <p class="mb-0 mt-1 small text-muted">
                        <c:out value="${note.getExcerpt(150)}"/>
                    </p>
                </a>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
