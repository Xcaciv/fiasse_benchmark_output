<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Search - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <h1 class="h3 mb-4"><i class="bi bi-search me-2"></i>Search Notes</h1>

        <form action="${pageContext.request.contextPath}/search" method="get" class="mb-4">
            <div class="input-group input-group-lg">
                <input type="text" class="form-control" name="q"
                       value="${not empty query ? query : ''}"
                       placeholder="Search by title or content..." autofocus>
                <button class="btn btn-primary" type="submit">
                    <i class="bi bi-search me-1"></i>Search
                </button>
            </div>
        </form>

        <c:if test="${not empty error}">
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle me-2"></i>${error}
            </div>
        </c:if>

        <c:if test="${not empty query}">
            <c:choose>
                <c:when test="${empty results}">
                    <div class="text-center py-5">
                        <i class="bi bi-search display-3 text-muted"></i>
                        <h4 class="mt-3 text-muted">No results found</h4>
                        <p class="text-muted">No notes match "<strong>${query}</strong>"</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <p class="text-muted mb-3">
                        Found ${results.size()} result<c:if test="${results.size() != 1}">s</c:if>
                        for "<strong>${query}</strong>"
                    </p>
                    <c:forEach var="note" items="${results}">
                        <div class="card mb-3 search-result">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-start">
                                    <div>
                                        <h5 class="card-title mb-1">
                                            <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}"
                                               class="text-decoration-none">${note.title}</a>
                                        </h5>
                                        <p class="text-muted small mb-2">
                                            By <strong>${note.authorUsername}</strong>
                                            <c:if test="${not empty note.updatedAt}">
                                                &bull; ${note.updatedAt.toString().substring(0, 10)}
                                            </c:if>
                                        </p>
                                        <p class="card-text text-muted">
                                            <c:choose>
                                                <c:when test="${note.content.length() > 200}">
                                                    ${note.content.substring(0, 200)}...
                                                </c:when>
                                                <c:otherwise>
                                                    ${note.content}
                                                </c:otherwise>
                                            </c:choose>
                                        </p>
                                    </div>
                                    <c:choose>
                                        <c:when test="${note.public}">
                                            <span class="badge bg-success ms-3">Public</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary ms-3">Your Private</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</div>

<%@ include file="includes/footer.jsp" %>
