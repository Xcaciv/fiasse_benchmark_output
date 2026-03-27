<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Search Notes - Loose Notes"/>
</jsp:include>
<div class="search-page">
    <h2>Search Public Notes</h2>
    <form method="get" action="${pageContext.request.contextPath}/notes/search" class="search-form">
        <input type="text" name="q" value="<c:out value='${query}'/>" placeholder="Search notes..." maxlength="200" required>
        <button type="submit" class="btn btn-primary">Search</button>
    </form>
    <c:if test="${not empty query}">
        <p>Results for: <strong><c:out value="${query}"/></strong></p>
        <c:choose>
            <c:when test="${empty results}">
                <div class="empty-state"><p>No public notes found matching your query.</p></div>
            </c:when>
            <c:otherwise>
            <div class="notes-grid">
                <c:forEach var="note" items="${results}">
                <div class="note-card">
                    <h3><a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"><c:out value="${note.title}"/></a></h3>
                    <p>By <c:out value="${note.ownerUsername}"/></p>
                    <p class="note-preview"><c:out value="${note.content.length() > 150 ? note.content.substring(0, 150).concat('...') : note.content}"/></p>
                </div>
                </c:forEach>
            </div>
            </c:otherwise>
        </c:choose>
    </c:if>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
