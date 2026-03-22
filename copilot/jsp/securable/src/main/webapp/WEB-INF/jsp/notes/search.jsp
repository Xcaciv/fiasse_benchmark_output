<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card">
    <h1>Search notes</h1>
    <form method="get" action="${pageContext.request.contextPath}/notes/search">
        <label>
            Keywords
                    <input type="search" name="q" value="${fn:escapeXml(query)}" placeholder="Search title or content">
        </label>
        <button class="btn primary" type="submit">Search</button>
    </form>
    <p class="muted">Results include your own notes of any visibility and public notes from other users.</p>
</section>
<section class="card">
    <c:choose>
        <c:when test="${empty results}">
            <div class="empty-state">No notes matched your search.</div>
        </c:when>
        <c:otherwise>
            <c:forEach var="note" items="${results}">
                <article class="card" style="margin-bottom:0.9rem;">
                    <div class="meta">
                        <span>By <strong><c:out value="${note.ownerUsername}" /></strong></span>
                        <span><c:out value="${note.createdAt}" /></span>
                        <span class="badge ${note.publicNote ? 'public' : 'private'}">${note.publicNote ? 'Public' : 'Private'}</span>
                    </div>
                    <h2><a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"><c:out value="${note.title}" /></a></h2>
                    <p class="note-preview"><c:out value="${note.excerpt}" /></p>
                </article>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
