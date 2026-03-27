<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Search Notes" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<h2 class="mb-4"><i class="bi bi-search text-primary"></i> Search Notes</h2>

<form method="get" action="${pageContext.request.contextPath}/search" class="mb-4">
    <div class="input-group">
        <input type="text" class="form-control form-control-lg" name="q"
               value="${keyword}" placeholder="Search by title or content..."
               autofocus>
        <button type="submit" class="btn btn-primary btn-lg">
            <i class="bi bi-search"></i> Search
        </button>
    </div>
</form>

<c:if test="${not empty keyword}">
    <p class="text-muted mb-3">
        <c:choose>
            <c:when test="${empty results}">No results found for "<strong>${keyword}</strong>"</c:when>
            <c:otherwise>${results.size()} result(s) for "<strong>${keyword}</strong>"</c:otherwise>
        </c:choose>
    </p>
</c:if>

<c:if test="${not empty results}">
    <div class="list-group shadow-sm">
        <c:forEach var="note" items="${results}">
            <a href="${pageContext.request.contextPath}/notes/${note.id}"
               class="list-group-item list-group-item-action">
                <div class="d-flex justify-content-between align-items-start">
                    <div class="flex-grow-1">
                        <h6 class="mb-1">${note.title}</h6>
                        <p class="mb-1 text-muted small">${note.excerpt}</p>
                        <small class="text-muted">
                            By ${note.ownerUsername} &middot;
                            <c:if test="${not empty note.createdAt}">
                                ${note.createdAtDisplay}
                            </c:if>
                        </small>
                    </div>
                    <div class="ms-3 text-end">
                        <c:if test="${note.ratingCount > 0}">
                            <span class="badge bg-warning text-dark">
                                <i class="bi bi-star-fill"></i>
                                ${note.averageRating}
                            </span>
                        </c:if>
                        <c:if test="${note.public}">
                            <span class="badge bg-success ms-1"><i class="bi bi-globe"></i></span>
                        </c:if>
                    </div>
                </div>
            </a>
        </c:forEach>
    </div>
</c:if>

<c:if test="${empty keyword}">
    <div class="text-center py-5 text-muted">
        <i class="bi bi-search" style="font-size: 3rem;"></i>
        <p class="mt-3">Enter keywords to search for notes by title or content.</p>
        <small>Results include your own notes (any visibility) and public notes from others.</small>
    </div>
</c:if>

<%@ include file="../shared/footer.jsp" %>
