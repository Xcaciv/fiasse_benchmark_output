<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card">
    <h1>Top rated public notes</h1>
    <p class="muted">Only public notes with at least three ratings appear here.</p>
</section>
<section class="card">
    <c:choose>
        <c:when test="${empty results}">
            <div class="empty-state">No public notes have reached the top-rated threshold yet.</div>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th>Title</th>
                    <th>Author</th>
                    <th>Average</th>
                    <th>Ratings</th>
                    <th>Preview</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="note" items="${results}">
                    <tr>
                        <td><a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"><c:out value="${note.title}" /></a></td>
                        <td><c:out value="${note.ownerUsername}" /></td>
                        <td>${note.averageRating}</td>
                        <td>${note.ratingCount}</td>
                        <td><c:out value="${note.excerpt}" /></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
