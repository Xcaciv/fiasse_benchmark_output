<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card">
    <h1>Admin dashboard</h1>
    <div class="grid two">
        <div class="stat">
            <div class="muted">Total users</div>
            <div style="font-size:2rem; font-weight:700;">${stats.totalUsers}</div>
        </div>
        <div class="stat">
            <div class="muted">Total notes</div>
            <div style="font-size:2rem; font-weight:700;">${stats.totalNotes}</div>
        </div>
    </div>
</section>

<div class="grid two">
    <section class="card">
        <h2>User management</h2>
        <form method="get" action="${pageContext.request.contextPath}/admin">
            <label>
                Search users
                        <input type="search" name="userQuery" value="${fn:escapeXml(userQuery)}" placeholder="Username or email">
            </label>
                    <input type="hidden" name="noteQuery" value="${fn:escapeXml(noteQuery)}">
            <button class="btn primary" type="submit">Search users</button>
        </form>
        <table>
            <thead>
            <tr>
                <th>User</th>
                <th>Role</th>
                <th>Registered</th>
                <th>Notes</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="userRow" items="${users}">
                <tr>
                    <td>
                        <strong><c:out value="${userRow.username}" /></strong><br>
                        <span class="muted"><c:out value="${userRow.email}" /></span>
                    </td>
                    <td><c:out value="${userRow.role}" /></td>
                    <td><c:out value="${userRow.createdAt}" /></td>
                    <td>${userRow.noteCount}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </section>

    <section class="card">
        <h2>Recent activity</h2>
        <c:choose>
            <c:when test="${empty activityLogs}">
                <div class="empty-state">No activity has been recorded yet.</div>
            </c:when>
            <c:otherwise>
                <c:forEach var="entry" items="${activityLogs}">
                    <article style="margin-bottom:0.9rem; border-bottom:1px solid var(--border); padding-bottom:0.9rem;">
                        <div class="meta">
                            <span><strong><c:out value="${entry.actorUsername}" /></strong></span>
                            <span><c:out value="${entry.actionType}" /></span>
                            <span><c:out value="${entry.createdAt}" /></span>
                        </div>
                        <div class="note-preview"><c:out value="${entry.details}" /></div>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<section class="card">
    <h2>Note ownership reassignment</h2>
    <form method="get" action="${pageContext.request.contextPath}/admin">
        <label>
            Search notes
                    <input type="search" name="noteQuery" value="${fn:escapeXml(noteQuery)}" placeholder="Title, content, or owner">
        </label>
                <input type="hidden" name="userQuery" value="${fn:escapeXml(userQuery)}">
        <button class="btn primary" type="submit">Search notes</button>
    </form>
    <c:choose>
        <c:when test="${empty notes}">
            <div class="empty-state">No notes matched your note search.</div>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th>Note</th>
                    <th>Current owner</th>
                    <th>Visibility</th>
                    <th>Transfer</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="note" items="${notes}">
                    <tr>
                        <td>
                            <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"><c:out value="${note.title}" /></a>
                            <div class="note-preview"><c:out value="${note.excerpt}" /></div>
                        </td>
                        <td><c:out value="${note.ownerUsername}" /></td>
                        <td><span class="badge ${note.publicNote ? 'public' : 'private'}">${note.publicNote ? 'Public' : 'Private'}</span></td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/reassign">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="noteId" value="${note.id}">
                                <label>
                                    <select name="newOwnerId" required>
                                        <option value="">Select user</option>
                                        <c:forEach var="candidate" items="${assignableUsers}">
                                            <option value="${candidate.id}" ${candidate.id == note.ownerId ? 'selected' : ''}><c:out value="${candidate.username}" /></option>
                                        </c:forEach>
                                    </select>
                                </label>
                                <button class="btn secondary" type="submit">Reassign</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
