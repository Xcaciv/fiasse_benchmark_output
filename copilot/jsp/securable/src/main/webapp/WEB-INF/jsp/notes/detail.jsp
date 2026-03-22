<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>
<section class="card">
    <div class="actions" style="justify-content:space-between; align-items:flex-start;">
        <div>
            <h1 style="margin-top:0;"><c:out value="${note.title}" /></h1>
            <div class="meta">
                <span>Owner: <strong><c:out value="${note.ownerUsername}" /></strong></span>
                <span>Created: <c:out value="${note.createdAt}" /></span>
                <span>Updated: <c:out value="${note.updatedAt}" /></span>
                <span>${note.ratingCount} ratings, average ${note.averageRating}</span>
                <span class="badge ${note.publicNote ? 'public' : 'private'}">${note.publicNote ? 'Public' : 'Private'}</span>
            </div>
        </div>
        <div class="actions">
            <c:if test="${isAuthenticated and currentUser.id == note.ownerId}">
                <a class="btn ghost" href="${pageContext.request.contextPath}/notes/edit?id=${note.id}">Edit</a>
            </c:if>
            <c:if test="${isAuthenticated and (currentUser.id == note.ownerId or isAdmin)}">
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/notes/delete" data-confirm="Delete this note and all associated data permanently?">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${note.id}">
                    <button class="btn danger" type="submit">Delete</button>
                </form>
            </c:if>
        </div>
    </div>
    <pre class="note-content"><c:out value="${note.content}" /></pre>
</section>

<div class="grid two">
    <section class="card">
        <h2>Attachments</h2>
        <c:choose>
            <c:when test="${empty attachments}">
                <div class="empty-state">No attachments uploaded for this note.</div>
            </c:when>
            <c:otherwise>
                <ul>
                            <c:forEach var="attachment" items="${attachments}">
                                <li>
                                    <c:choose>
                                        <c:when test="${shareMode}">
                                            <a href="${pageContext.request.contextPath}/attachments/download?id=${attachment.id}&amp;share=${fn:escapeXml(shareToken)}"><c:out value="${attachment.originalName}" /></a>
                                        </c:when>
                                        <c:otherwise>
                                            <a href="${pageContext.request.contextPath}/attachments/download?id=${attachment.id}"><c:out value="${attachment.originalName}" /></a>
                                        </c:otherwise>
                                    </c:choose>
                                    <span class="muted">(${attachment.sizeBytes} bytes)</span>
                                </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="card">
        <h2>Ratings</h2>
        <c:choose>
            <c:when test="${empty ratings}">
                <div class="empty-state">No ratings yet.</div>
            </c:when>
            <c:otherwise>
                <c:forEach var="rating" items="${ratings}">
                    <article style="margin-bottom:0.9rem; border-bottom:1px solid var(--border); padding-bottom:0.9rem;">
                        <div class="meta">
                            <span><strong><c:out value="${rating.username}" /></strong></span>
                            <span>${rating.ratingValue}/5</span>
                            <span><c:out value="${rating.updatedAt}" /></span>
                        </div>
                        <p class="note-preview"><c:out value="${rating.comment}" /></p>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<c:if test="${isAuthenticated}">
    <section class="card">
        <h2>${myRating != null ? 'Update your rating' : 'Rate this note'}</h2>
                <form method="post" action="${pageContext.request.contextPath}/notes/rate">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${note.id}">
                    <c:if test="${shareMode}">
                        <input type="hidden" name="shareToken" value="${fn:escapeXml(shareToken)}">
                    </c:if>
                    <label>
                        Rating (1-5)
                <select name="ratingValue" required>
                    <option value="">Select</option>
                    <c:forEach begin="1" end="5" var="value">
                        <option value="${value}" ${myRating != null and myRating.ratingValue == value ? 'selected' : ''}>${value}</option>
                    </c:forEach>
                </select>
            </label>
            <label>
                Comment
                <textarea name="comment" maxlength="500"><c:out value="${myRating != null ? myRating.comment : ''}" /></textarea>
            </label>
            <button class="btn primary" type="submit">Save rating</button>
        </form>
    </section>
</c:if>

<c:if test="${isAuthenticated and currentUser.id == note.ownerId and not shareMode}">
    <section class="card">
        <h2>Share link management</h2>
        <c:choose>
            <c:when test="${activeShareLink != null}">
                <p class="muted">An active share link exists and was generated on <c:out value="${activeShareLink.createdAt}" />. You can regenerate it to replace the old link or revoke it entirely.</p>
            </c:when>
            <c:otherwise>
                <p class="muted">Generate a unique link to let anyone view this note and its attachments without authentication.</p>
            </c:otherwise>
        </c:choose>
        <div class="actions">
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/notes/share/generate">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="id" value="${note.id}">
                <button class="btn secondary" type="submit">${activeShareLink != null ? 'Regenerate share link' : 'Generate share link'}</button>
            </form>
            <c:if test="${activeShareLink != null}">
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/notes/share/revoke" data-confirm="Revoke the active share link? Anyone using it will lose access.">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${note.id}">
                    <button class="btn danger" type="submit">Revoke share link</button>
                </form>
            </c:if>
        </div>
    </section>
</c:if>

<c:if test="${isAdmin and assignableUsers != null}">
    <section class="card">
        <h2>Admin ownership reassignment</h2>
        <form method="post" action="${pageContext.request.contextPath}/admin/reassign">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="noteId" value="${note.id}">
            <label>
                Transfer this note to
                <select name="newOwnerId" required>
                    <option value="">Select a user</option>
                    <c:forEach var="candidate" items="${assignableUsers}">
                        <option value="${candidate.id}" ${candidate.id == note.ownerId ? 'selected' : ''}><c:out value="${candidate.username}" /> (${candidate.email})</option>
                    </c:forEach>
                </select>
            </label>
            <button class="btn secondary" type="submit">Reassign note</button>
        </form>
    </section>
</c:if>
<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
