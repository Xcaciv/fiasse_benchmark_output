<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card page-intro">
    <h1>Multi-user notes with sharing, attachments, ratings, and admin controls</h1>
    <p class="muted">Loose Notes is a JSP/Servlet implementation of the PRD using secure password hashing, CSRF protection, ownership checks, local attachment storage, and SQLite-backed persistence.</p>
    <div class="actions">
        <c:choose>
            <c:when test="${isAuthenticated}">
                <a class="btn primary" href="${pageContext.request.contextPath}/notes/create">Create a note</a>
                <a class="btn secondary" href="${pageContext.request.contextPath}/notes">View my notes</a>
            </c:when>
            <c:otherwise>
                <a class="btn primary" href="${pageContext.request.contextPath}/auth/register">Create account</a>
                <a class="btn secondary" href="${pageContext.request.contextPath}/auth/login">Sign in</a>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<div class="grid two">
    <section class="card">
        <h2>Recent public notes</h2>
        <c:choose>
            <c:when test="${empty recentPublicNotes}">
                <div class="empty-state">No public notes yet.</div>
            </c:when>
            <c:otherwise>
                <c:forEach var="note" items="${recentPublicNotes}">
                    <article class="card" style="margin-bottom:0.85rem;">
                        <div class="meta">
                            <span>By <strong><c:out value="${note.ownerUsername}" /></strong></span>
                            <span><c:out value="${note.createdAt}" /></span>
                            <span>${note.ratingCount} ratings</span>
                        </div>
                        <h3><a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"><c:out value="${note.title}" /></a></h3>
                        <p class="note-preview"><c:out value="${note.excerpt}" /></p>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="card">
        <h2>What the app supports</h2>
        <ul>
            <li>User registration, login, logout, profile updates, and password reset emails written to the local outbox.</li>
            <li>Private-by-default notes with public visibility, share links, file attachments, and ownership-aware editing and deletion.</li>
            <li>Keyword search, public top-rated notes, note ratings with comments, and an admin dashboard for user search and ownership reassignment.</li>
        </ul>
        <c:if test="${not empty myNotesPreview}">
            <h3>Your latest notes</h3>
            <c:forEach var="note" items="${myNotesPreview}">
                <div style="margin-bottom:0.7rem;">
                    <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"><c:out value="${note.title}" /></a>
                    <span class="badge ${note.publicNote ? 'public' : 'private'}">${note.publicNote ? 'Public' : 'Private'}</span>
                </div>
            </c:forEach>
        </c:if>
    </section>
</div>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
