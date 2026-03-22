<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Search Notes"/>
</jsp:include>

<h1 class="h3 mb-4">Search Notes</h1>

<form method="get" action="${pageContext.request.contextPath}/search" class="mb-4">
    <div class="input-group">
        <input type="text" name="q" class="form-control form-control-lg"
               placeholder="Search by title or content…"
               value="<c:out value='${query}'/>" maxlength="200">
        <button class="btn btn-primary" type="submit">Search</button>
    </div>
    <div class="form-text">Searches your notes and all public notes.</div>
</form>

<c:if test="${not empty query}">
    <p class="text-muted">Results for "<c:out value='${query}'/>": ${fn:length(results)} found</p>
    <c:choose>
        <c:when test="${empty results}">
            <div class="alert alert-info">No notes matched your search.</div>
        </c:when>
        <c:otherwise>
            <div class="list-group">
                <c:forEach var="note" items="${results}">
                    <a href="${pageContext.request.contextPath}/notes/${note.id}"
                       class="list-group-item list-group-item-action">
                        <div class="d-flex justify-content-between">
                            <h6 class="mb-1"><c:out value="${note.title}"/></h6>
                            <small class="text-muted">
                                by <c:out value="${note.authorUsername}"/>
                                &bull; <c:out value="${note.createdAt}"/>
                            </small>
                        </div>
                        <small><c:out value="${note.excerpt}"/></small>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
