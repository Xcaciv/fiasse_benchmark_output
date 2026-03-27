<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="Top Rated Notes - Loose Notes"/>
</jsp:include>
<div class="top-rated-page">
    <h2>Top Rated Notes</h2>
    <c:choose>
        <c:when test="${empty notes}">
            <div class="empty-state"><p>No rated notes yet.</p></div>
        </c:when>
        <c:otherwise>
        <div class="notes-grid">
            <c:forEach var="note" items="${notes}">
            <div class="note-card">
                <h3><a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"><c:out value="${note.title}"/></a></h3>
                <p>By <c:out value="${note.ownerUsername}"/></p>
                <div class="note-rating">
                    <span class="stars">&#9733; <c:out value="${String.format('%.1f', note.averageRating)}"/></span>
                    <span class="rating-count">(<c:out value="${note.ratingCount}"/> reviews)</span>
                </div>
                <p class="note-preview"><c:out value="${note.content.length() > 100 ? note.content.substring(0, 100).concat('...') : note.content}"/></p>
            </div>
            </c:forEach>
        </div>
        </c:otherwise>
    </c:choose>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
