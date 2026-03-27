<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="View Note - Loose Notes"/>
</jsp:include>
<div class="note-view">
    <div class="note-view-header">
        <h2><c:out value="${note.title}"/></h2>
        <div class="note-meta">
            <span>By <c:out value="${note.ownerUsername}"/></span>
            <span class="badge badge-${note.visibility == 'PUBLIC' ? 'success' : 'secondary'}"><c:out value="${note.visibility}"/></span>
            <span>Created: ${note.createdAt}</span>
        </div>
        <c:if test="${isOwner}">
        <div class="note-actions">
            <a href="${pageContext.request.contextPath}/notes/edit?id=${note.id}" class="btn btn-sm">Edit</a>
            <form method="post" action="${pageContext.request.contextPath}/notes/delete" style="display:inline" onsubmit="return confirmDelete()">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="id" value="${note.id}">
                <button type="submit" class="btn btn-sm btn-danger">Delete</button>
            </form>
        </div>
        </c:if>
    </div>

    <div class="note-content"><c:out value="${note.content}"/></div>

    <c:if test="${isOwner}">
    <div class="share-section">
        <h3>Share</h3>
        <c:choose>
            <c:when test="${shareLink != null}">
                <p>Share link: <code>${pageContext.request.contextPath}/share/<c:out value="${shareLink.token}"/></code></p>
                <form method="post" action="${pageContext.request.contextPath}/notes/share" style="display:inline">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="noteId" value="${note.id}">
                    <input type="hidden" name="action" value="generate">
                    <button type="submit" class="btn btn-sm">Regenerate Link</button>
                </form>
                <form method="post" action="${pageContext.request.contextPath}/notes/share" style="display:inline">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="noteId" value="${note.id}">
                    <input type="hidden" name="action" value="revoke">
                    <button type="submit" class="btn btn-sm btn-danger">Revoke Link</button>
                </form>
            </c:when>
            <c:otherwise>
                <form method="post" action="${pageContext.request.contextPath}/notes/share">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="noteId" value="${note.id}">
                    <input type="hidden" name="action" value="generate">
                    <button type="submit" class="btn btn-sm">Generate Share Link</button>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
    </c:if>

    <c:if test="${not empty attachments}">
    <div class="attachments-section">
        <h3>Attachments</h3>
        <ul class="attachment-list">
            <c:forEach var="att" items="${attachments}">
            <li>
                <a href="${pageContext.request.contextPath}/attachment/download?id=${att.id}">
                    <c:out value="${att.originalFilename}"/>
                </a>
                <span class="text-muted">(${att.fileSize / 1024} KB)</span>
            </li>
            </c:forEach>
        </ul>
    </div>
    </c:if>

    <c:if test="${isOwner}">
    <div class="upload-section">
        <h3>Upload Attachment</h3>
        <form method="post" action="${pageContext.request.contextPath}/attachment/upload" enctype="multipart/form-data">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="noteId" value="${note.id}">
            <input type="file" name="file" accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg">
            <button type="submit" class="btn btn-sm">Upload</button>
        </form>
    </div>
    </c:if>

    <c:if test="${note.visibility == 'PUBLIC'}">
    <div class="ratings-section">
        <h3>Ratings (<c:out value="${ratings.size()}"/> reviews)</h3>
        <c:if test="${sessionScope.userId != null && !isOwner}">
        <div class="rate-form">
            <h4>${userRating != null ? 'Update Your Rating' : 'Rate This Note'}</h4>
            <form method="post" action="${pageContext.request.contextPath}/notes/rate">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="noteId" value="${note.id}">
                <div class="form-group">
                    <label>Rating</label>
                    <select name="rating" required>
                        <c:forEach begin="1" end="5" var="i">
                            <option value="${i}" ${userRating != null && userRating.rating == i ? 'selected' : ''}>${i} Star${i > 1 ? 's' : ''}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-group">
                    <label>Comment (optional)</label>
                    <textarea name="comment" maxlength="1000" rows="3"><c:out value="${userRating != null ? userRating.comment : ''}"/></textarea>
                </div>
                <button type="submit" class="btn btn-sm btn-primary">Submit Rating</button>
            </form>
        </div>
        </c:if>
        <c:forEach var="r" items="${ratings}">
        <div class="rating-item">
            <strong><c:out value="${r.raterUsername}"/></strong>
            <span class="stars">${r.rating}/5 &#9733;</span>
            <c:if test="${not empty r.comment}">
                <p><c:out value="${r.comment}"/></p>
            </c:if>
        </div>
        </c:forEach>
    </div>
    </c:if>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
