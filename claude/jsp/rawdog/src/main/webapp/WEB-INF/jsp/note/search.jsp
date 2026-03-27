<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Search Notes - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<h2 class="mb-4"><i class="bi bi-search"></i> Search Notes</h2>

<form method="get" action="${pageContext.request.contextPath}/search" class="mb-4">
    <div class="input-group">
        <input type="text" class="form-control form-control-lg" name="q"
               value="${not empty keyword ? keyword : ''}"
               placeholder="Search by title or content..." autofocus>
        <button type="submit" class="btn btn-primary">
            <i class="bi bi-search"></i> Search
        </button>
    </div>
</form>

<c:if test="${not empty keyword}">
    <p class="text-muted">
        Found <strong>${results.size()}</strong> result${results.size() != 1 ? 's' : ''} for
        "<strong><c:out value="${keyword}"/></strong>"
    </p>

    <c:choose>
        <c:when test="${empty results}">
            <div class="text-center py-4">
                <i class="bi bi-search display-4 text-muted"></i>
                <p class="text-muted mt-3">No notes found matching your search.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="list-group">
                <c:forEach var="note" items="${results}">
                    <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"
                       class="list-group-item list-group-item-action">
                        <div class="d-flex justify-content-between align-items-start">
                            <h5 class="mb-1">${note.title}</h5>
                            <div>
                                <span class="badge ${note.public ? 'bg-success' : 'bg-secondary'} me-1">
                                    ${note.public ? 'Public' : 'Private'}
                                </span>
                                <c:if test="${note.ratingCount > 0}">
                                    <span class="badge bg-warning text-dark">
                                        <i class="bi bi-star-fill"></i>
                                        <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1" minFractionDigits="1"/>
                                    </span>
                                </c:if>
                            </div>
                        </div>
                        <p class="mb-1 text-muted small">
                            <c:out value="${note.content.length() > 150 ? note.content.substring(0, 150).concat('...') : note.content}"/>
                        </p>
                        <small class="text-muted">
                            <i class="bi bi-person"></i> ${note.ownerUsername} &bull;
                            <i class="bi bi-calendar"></i> ${note.updatedAt}
                        </small>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<%@ include file="../layout/footer.jsp" %>
