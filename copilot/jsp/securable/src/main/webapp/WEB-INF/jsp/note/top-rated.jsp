<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Top Rated Notes"/>
</jsp:include>

<h1 class="h3 mb-4">🏆 Top Rated Notes</h1>
<p class="text-muted">Public notes with at least 3 ratings, sorted by average score.</p>

<c:choose>
    <c:when test="${empty topNotes}">
        <div class="alert alert-info">No notes have enough ratings yet. Be the first to rate a note!</div>
    </c:when>
    <c:otherwise>
        <div class="list-group">
            <c:forEach var="note" items="${topNotes}" varStatus="status">
                <a href="${pageContext.request.contextPath}/notes/${note.id}"
                   class="list-group-item list-group-item-action">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <span class="badge bg-secondary me-2">#${status.index + 1}</span>
                            <strong><c:out value="${note.title}"/></strong>
                            <small class="text-muted ms-2">by <c:out value="${note.authorUsername}"/></small>
                        </div>
                        <span class="badge bg-warning text-dark fs-6">⭐ Top Rated</span>
                    </div>
                    <p class="mb-1 mt-1 text-muted small"><c:out value="${note.excerpt}"/></p>
                </a>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
