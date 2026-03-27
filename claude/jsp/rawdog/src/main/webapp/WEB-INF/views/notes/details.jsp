<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${note.title}" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<c:set var="isOwner" value="${sessionScope.currentUser.id == note.userId}"/>
<c:set var="isAdmin" value="${sessionScope.currentUser.admin}"/>

<div class="row">
    <div class="col-lg-8">
        <!-- Note header -->
        <div class="d-flex justify-content-between align-items-start mb-3">
            <div>
                <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary btn-sm mb-2">
                    <i class="bi bi-arrow-left"></i> My Notes
                </a>
                <h2 class="mb-1">${note.title}</h2>
                <div class="text-muted small">
                    By <strong>${note.ownerUsername}</strong> &middot;
                    <c:if test="${not empty note.createdAtDisplay}">
                        Created ${note.createdAtDisplay}
                    </c:if>
                    <c:if test="${note.public}">
                        &middot; <span class="badge bg-success"><i class="bi bi-globe"></i> Public</span>
                    </c:if>
                    <c:if test="${not note.public}">
                        &middot; <span class="badge bg-secondary"><i class="bi bi-lock"></i> Private</span>
                    </c:if>
                </div>
            </div>
            <c:if test="${isOwner or isAdmin}">
                <div class="btn-group">
                    <a href="${pageContext.request.contextPath}/notes/${note.id}/edit"
                       class="btn btn-outline-secondary btn-sm">
                        <i class="bi bi-pencil"></i> Edit
                    </a>
                    <a href="${pageContext.request.contextPath}/notes/${note.id}/delete"
                       class="btn btn-outline-danger btn-sm">
                        <i class="bi bi-trash"></i> Delete
                    </a>
                </div>
            </c:if>
        </div>

        <!-- Note content -->
        <div class="card shadow-sm mb-4">
            <div class="card-body">
                <pre class="note-content">${note.content}</pre>
            </div>
        </div>

        <!-- Attachments -->
        <c:if test="${not empty attachments}">
            <div class="card shadow-sm mb-4">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-paperclip"></i> Attachments</h6>
                </div>
                <div class="list-group list-group-flush">
                    <c:forEach var="att" items="${attachments}">
                        <div class="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <i class="bi bi-file-earmark"></i>
                                <a href="${pageContext.request.contextPath}/attachments/${att.id}/download"
                                   class="ms-1">${att.originalFilename}</a>
                                <small class="text-muted ms-2">${att.fileSizeDisplay}</small>
                            </div>
                            <c:if test="${isOwner or isAdmin}">
                                <form method="post" action="${pageContext.request.contextPath}/attachments/${att.id}/delete"
                                      onsubmit="return confirm('Delete this attachment?')">
                                    <button type="submit" class="btn btn-sm btn-outline-danger">
                                        <i class="bi bi-x"></i>
                                    </button>
                                </form>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:if>

        <!-- Share link section (owner only) -->
        <c:if test="${isOwner or isAdmin}">
            <div class="card shadow-sm mb-4">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-share"></i> Share Link</h6>
                </div>
                <div class="card-body">
                    <%-- existingShareLink is set by NoteServlet --%>
                    <c:if test="${not empty existingShareLink}">
                        <div class="mb-3">
                            <label class="form-label text-muted small">Share URL</label>
                            <div class="input-group">
                                <input type="text" class="form-control form-control-sm" readonly
                                       id="shareUrl"
                                       value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share/${existingShareLink.token}">
                                <button class="btn btn-outline-secondary btn-sm" onclick="copyShareUrl()" type="button">
                                    <i class="bi bi-clipboard"></i> Copy
                                </button>
                            </div>
                        </div>
                    </c:if>
                    <div class="d-flex gap-2">
                        <form method="post" action="${pageContext.request.contextPath}/share/generate">
                            <input type="hidden" name="noteId" value="${note.id}">
                            <button type="submit" class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-link-45deg"></i>
                                ${not empty existingShareLink ? 'Regenerate Link' : 'Generate Share Link'}
                            </button>
                        </form>
                        <c:if test="${not empty existingShareLink}">
                            <form method="post" action="${pageContext.request.contextPath}/share/revoke"
                                  onsubmit="return confirm('Revoke this share link?')">
                                <input type="hidden" name="noteId" value="${note.id}">
                                <button type="submit" class="btn btn-sm btn-outline-danger">
                                    <i class="bi bi-x-circle"></i> Revoke
                                </button>
                            </form>
                        </c:if>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- Ratings -->
        <div class="card shadow-sm mb-4">
            <div class="card-header d-flex justify-content-between align-items-center">
                <h6 class="mb-0">
                    <i class="bi bi-star"></i> Ratings
                    <c:if test="${note.ratingCount > 0}">
                        <span class="badge bg-warning text-dark ms-1">
                            ${note.averageRating} / 5 (${note.ratingCount})
                        </span>
                    </c:if>
                </h6>
            </div>
            <div class="card-body">
                <!-- Rate this note (if not owner) -->
                <c:if test="${not isOwner and not empty sessionScope.currentUser}">
                    <c:if test="${param.error == 'own'}">
                        <div class="alert alert-warning small">You cannot rate your own note.</div>
                    </c:if>
                    <form method="post" action="${pageContext.request.contextPath}/ratings" class="mb-4">
                        <input type="hidden" name="noteId" value="${note.id}">
                        <h6 class="mb-2">${not empty userRating ? 'Update your rating' : 'Rate this note'}</h6>
                        <div class="mb-2">
                            <div class="star-rating">
                                <c:forEach begin="1" end="5" var="i">
                                    <input type="radio" name="ratingValue" id="star${i}" value="${i}"
                                           ${not empty userRating and userRating.ratingValue == i ? 'checked' : ''} required>
                                    <label for="star${i}" title="${i} star"><i class="bi bi-star-fill"></i></label>
                                </c:forEach>
                            </div>
                        </div>
                        <div class="mb-2">
                            <textarea class="form-control form-control-sm" name="comment" rows="2"
                                      placeholder="Optional comment...">${not empty userRating ? userRating.comment : ''}</textarea>
                        </div>
                        <button type="submit" class="btn btn-sm btn-primary">
                            <i class="bi bi-star"></i> ${not empty userRating ? 'Update Rating' : 'Submit Rating'}
                        </button>
                    </form>
                </c:if>

                <!-- Ratings list -->
                <c:choose>
                    <c:when test="${empty ratings}">
                        <p class="text-muted mb-0">No ratings yet. Be the first to rate this note!</p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="rating" items="${ratings}">
                            <div class="border-bottom pb-3 mb-3">
                                <div class="d-flex justify-content-between align-items-center mb-1">
                                    <strong>${rating.raterUsername}</strong>
                                    <span class="text-warning">
                                        <c:forEach begin="1" end="${rating.ratingValue}" var="s">
                                            <i class="bi bi-star-fill"></i>
                                        </c:forEach>
                                        <c:forEach begin="${rating.ratingValue + 1}" end="5" var="s">
                                            <i class="bi bi-star text-muted"></i>
                                        </c:forEach>
                                    </span>
                                </div>
                                <c:if test="${not empty rating.comment}">
                                    <p class="mb-0 text-muted small">${rating.comment}</p>
                                </c:if>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<script>
function copyShareUrl() {
    const input = document.getElementById('shareUrl');
    input.select();
    document.execCommand('copy');
    alert('Share URL copied to clipboard!');
}
</script>

<%@ include file="../shared/footer.jsp" %>
