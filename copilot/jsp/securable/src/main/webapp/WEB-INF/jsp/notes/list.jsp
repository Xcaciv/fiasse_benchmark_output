<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card">
    <div class="actions" style="justify-content:space-between;">
        <div>
            <h1 style="margin:0;">My notes</h1>
            <p class="muted">Private notes stay visible only to you unless you make them public or generate a share link.</p>
        </div>
        <a class="btn primary" href="${pageContext.request.contextPath}/notes/create">Create note</a>
    </div>
</section>
<section class="card">
    <c:choose>
        <c:when test="${empty notes}">
            <div class="empty-state">You have not created any notes yet.</div>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th>Title</th>
                    <th>Visibility</th>
                    <th>Updated</th>
                    <th>Ratings</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="note" items="${notes}">
                    <tr>
                        <td>
                            <strong><c:out value="${note.title}" /></strong>
                            <div class="note-preview"><c:out value="${note.excerpt}" /></div>
                        </td>
                        <td><span class="badge ${note.publicNote ? 'public' : 'private'}">${note.publicNote ? 'Public' : 'Private'}</span></td>
                        <td><c:out value="${note.updatedAt}" /></td>
                        <td>${note.ratingCount} (${note.averageRating})</td>
                        <td class="table-actions">
                            <a class="btn secondary" href="${pageContext.request.contextPath}/notes/view?id=${note.id}">View</a>
                            <a class="btn ghost" href="${pageContext.request.contextPath}/notes/edit?id=${note.id}">Edit</a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
