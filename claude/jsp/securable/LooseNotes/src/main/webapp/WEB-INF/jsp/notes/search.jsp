<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Search Notes - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h1 class="h3 mb-4">Search Notes</h1>

<form method="get" action="${pageContext.request.contextPath}/notes/search" class="mb-4">
    <div class="input-group">
        <input type="text" class="form-control" name="q" placeholder="Search by title or content..."
               value="<c:out value='${query}'/>" maxlength="100" required>
        <button type="submit" class="btn btn-primary">Search</button>
    </div>
</form>

<c:if test="${error != null}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<c:if test="${query != null}">
    <c:choose>
        <c:when test="${empty results}">
            <p class="text-muted">No notes found for "<c:out value='${query}'/>". </p>
        </c:when>
        <c:otherwise>
            <p class="text-muted mb-3">
                <c:out value="${results.size()}"/> result(s) for "<c:out value='${query}'/>":
            </p>
            <div class="list-group">
                <c:forEach var="note" items="${results}">
                    <a href="${pageContext.request.contextPath}/notes/view/<c:out value='${note.id}'/>"
                       class="list-group-item list-group-item-action">
                        <div class="d-flex justify-content-between align-items-start">
                            <h6 class="mb-1"><c:out value="${note.title}"/></h6>
                            <small class="text-muted ms-2">
                                <c:out value="${note.public ? 'Public' : 'Private'}"/>
                            </small>
                        </div>
                        <p class="mb-1 small text-muted">
                            <c:out value="${note.getExcerpt(200)}"/>
                        </p>
                        <small class="text-muted">
                            By <c:out value="${note.ownerUsername}"/> &bull;
                            <fmt:formatDate value="${note.createdAt}" pattern="MMM d, yyyy"/>
                        </small>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
