<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container mt-4">
    <h1 class="h2 mb-4">Search Notes</h1>

    <%-- Search Form --%>
    <form method="get" action="${pageContext.request.contextPath}/notes/search" class="mb-4">
        <div class="input-group">
            <input type="text"
                   name="query"
                   class="form-control form-control-lg"
                   placeholder="Search notes by title or content..."
                   value="<c:out value="${query}"/>"
                   autofocus>
            <button type="submit" class="btn btn-primary btn-lg">
                Search
            </button>
        </div>
    </form>

    <%-- Results --%>
    <c:if test="${not empty query}">
        <c:choose>
            <c:when test="${empty results}">
                <div class="text-center py-5">
                    <p class="text-muted fs-5">
                        No notes found for &ldquo;<strong><c:out value="${query}"/></strong>&rdquo;.
                    </p>
                    <p class="text-muted">Try different keywords or check your spelling.</p>
                </div>
            </c:when>
            <c:otherwise>
                <p class="text-muted mb-3">
                    Found <strong>${fn:length(results)}</strong> result<c:if test="${fn:length(results) != 1}">s</c:if>
                    for &ldquo;<strong><c:out value="${query}"/></strong>&rdquo;
                </p>
                <div class="list-group">
                    <c:forEach var="note" items="${results}">
                        <a href="${pageContext.request.contextPath}/notes/${note.id}"
                           class="list-group-item list-group-item-action py-3">
                            <div class="d-flex justify-content-between align-items-start mb-1">
                                <h5 class="mb-0 fw-semibold"><c:out value="${note.title}"/></h5>
                                <small class="text-muted ms-2 text-nowrap">
                                    <fmt:formatDate value="${note.createdAt}" pattern="MMM dd, yyyy"/>
                                </small>
                            </div>
                            <p class="mb-1 text-muted small">
                                By <c:out value="${note.authorUsername}"/>
                            </p>
                            <c:if test="${not empty note.content}">
                                <p class="mb-0 text-muted small text-truncate" style="max-width: 100%;">
                                    <c:set var="excerpt" value="${note.content}"/>
                                    <c:choose>
                                        <c:when test="${fn:length(excerpt) > 200}">
                                            <c:out value="${fn:substring(excerpt, 0, 200)}"/>...
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${excerpt}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </p>
                            </c:if>
                        </a>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
