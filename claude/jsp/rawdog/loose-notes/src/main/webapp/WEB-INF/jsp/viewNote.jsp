<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="${note.title} - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<c:if test="${not empty success}">
    <div class="alert alert-success alert-dismissible fade show" role="alert">
        <i class="bi bi-check-circle me-2"></i>${success}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<c:if test="${not empty param.error}">
    <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle me-2"></i>${param.error}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<div class="row">
    <div class="col-lg-8">
        <!-- Note Card -->
        <div class="card shadow mb-4">
            <div class="card-header d-flex justify-content-between align-items-center py-3">
                <div>
                    <h1 class="h3 mb-1">${note.title}</h1>
                    <small class="text-muted">
                        By <strong>${note.authorUsername}</strong> &bull;
                        <c:if test="${not empty note.createdAt}">
                            Created ${note.createdAt.toString().substring(0, 10)}
                        </c:if>
                        <c:if test="${not empty note.updatedAt}">
                            &bull; Updated ${note.updatedAt.toString().substring(0, 10)}
                        </c:if>
                    </small>
                </div>
                <div class="d-flex gap-2 align-items-center">
                    <c:choose>
                        <c:when test="${note.public}">
                            <span class="badge bg-success">Public</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-secondary">Private</span>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${sessionScope.userId == note.userId || sessionScope.userRole == 'ADMIN'}">
                        <a href="${pageContext.request.contextPath}/notes?action=edit&id=${note.id}"
                           class="btn btn-sm btn-outline-secondary">
                            <i class="bi bi-pencil me-1"></i>Edit
                        </a>
                        <button class="btn btn-sm btn-outline-danger"
                                onclick="confirmDelete(${note.id}, '${note.title.replace("'", "\\'")}')">
                            <i class="bi bi-trash me-1"></i>Delete
                        </button>
                    </c:if>
                </div>
            </div>
            <div class="card-body">
                <div class="note-content">${note.content}</div>
            </div>
        </div>

        <!-- Ratings Section -->
        <div class="card shadow mb-4">
            <div class="card-header">
                <h5 class="mb-0">
                    <i class="bi bi-star me-2"></i>Ratings &amp; Comments
                    <c:if test="${note.ratingCount > 0}">
                        <span class="badge bg-warning text-dark ms-2">
                            <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1" /> / 5
                            (${note.ratingCount} rating<c:if test="${note.ratingCount != 1}">s</c:if>)
                        </span>
                    </c:if>
                </h5>
            </div>
            <div class="card-body">
                <!-- Rating form (if logged in and not own note) -->
                <c:if test="${not empty sessionScope.userId && sessionScope.userId != note.userId}">
                    <div class="mb-4">
                        <h6>${not empty userRating ? 'Update Your Rating' : 'Rate This Note'}</h6>
                        <form action="${pageContext.request.contextPath}/ratings" method="post">
                            <input type="hidden" name="action" value="${not empty userRating ? 'edit' : 'add'}">
                            <input type="hidden" name="noteId" value="${note.id}">
                            <c:if test="${not empty userRating}">
                                <input type="hidden" name="ratingId" value="${userRating.id}">
                            </c:if>
                            <div class="mb-2">
                                <label class="form-label">Rating (1-5 stars)</label>
                                <div class="d-flex gap-2">
                                    <c:forEach begin="1" end="5" var="star">
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="radio" name="rating"
                                                   id="star${star}" value="${star}"
                                                   ${not empty userRating && userRating.rating == star ? 'checked' : ''}>
                                            <label class="form-check-label" for="star${star}">
                                                ${star} <i class="bi bi-star-fill text-warning"></i>
                                            </label>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                            <div class="mb-2">
                                <textarea class="form-control" name="comment" rows="2"
                                          placeholder="Optional comment...">${not empty userRating ? userRating.comment : ''}</textarea>
                            </div>
                            <button type="submit" class="btn btn-sm btn-warning">
                                <i class="bi bi-star me-1"></i>${not empty userRating ? 'Update Rating' : 'Submit Rating'}
                            </button>
                        </form>
                    </div>
                    <hr>
                </c:if>

                <!-- Existing ratings -->
                <c:choose>
                    <c:when test="${empty ratings}">
                        <p class="text-muted">No ratings yet. Be the first to rate this note!</p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="rating" items="${ratings}">
                            <div class="rating-item mb-3 p-3 bg-light rounded">
                                <div class="d-flex justify-content-between">
                                    <strong>${rating.username}</strong>
                                    <div>
                                        <c:forEach begin="1" end="5" var="star">
                                            <i class="bi bi-star${star <= rating.rating ? '-fill' : ''} text-warning"></i>
                                        </c:forEach>
                                    </div>
                                </div>
                                <c:if test="${not empty rating.comment}">
                                    <p class="mt-2 mb-0 small">${rating.comment}</p>
                                </c:if>
                                <small class="text-muted">
                                    <c:if test="${not empty rating.createdAt}">
                                        ${rating.createdAt.toString().substring(0, 10)}
                                    </c:if>
                                </small>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <!-- Sidebar -->
    <div class="col-lg-4">
        <!-- Attachments -->
        <div class="card shadow mb-4">
            <div class="card-header">
                <h6 class="mb-0"><i class="bi bi-paperclip me-2"></i>Attachments</h6>
            </div>
            <div class="card-body">
                <c:if test="${sessionScope.userId == note.userId || sessionScope.userRole == 'ADMIN'}">
                    <form action="${pageContext.request.contextPath}/attachments" method="post"
                          enctype="multipart/form-data" class="mb-3">
                        <input type="hidden" name="action" value="upload">
                        <input type="hidden" name="noteId" value="${note.id}">
                        <div class="input-group">
                            <input type="file" class="form-control form-control-sm" name="file" required>
                            <button class="btn btn-sm btn-outline-primary" type="submit">
                                <i class="bi bi-upload"></i>
                            </button>
                        </div>
                    </form>
                </c:if>

                <c:choose>
                    <c:when test="${empty attachments}">
                        <p class="text-muted small">No attachments</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-unstyled mb-0">
                            <c:forEach var="attachment" items="${attachments}">
                                <li class="d-flex justify-content-between align-items-center mb-2">
                                    <div class="d-flex align-items-center overflow-hidden">
                                        <i class="bi bi-file-earmark me-2 text-muted flex-shrink-0"></i>
                                        <div class="overflow-hidden">
                                            <a href="${pageContext.request.contextPath}/attachments?action=download&id=${attachment.id}"
                                               class="small d-block text-truncate" title="${attachment.originalFilename}">
                                                ${attachment.originalFilename}
                                            </a>
                                            <small class="text-muted">${attachment.formattedFileSize}</small>
                                        </div>
                                    </div>
                                    <c:if test="${sessionScope.userId == note.userId || sessionScope.userRole == 'ADMIN'}">
                                        <form action="${pageContext.request.contextPath}/attachments" method="post"
                                              class="ms-2 flex-shrink-0">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="id" value="${attachment.id}">
                                            <input type="hidden" name="noteId" value="${note.id}">
                                            <button type="submit" class="btn btn-sm btn-outline-danger"
                                                    onclick="return confirm('Delete this attachment?')"
                                                    title="Delete">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </form>
                                    </c:if>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Share Link -->
        <c:if test="${sessionScope.userId == note.userId || sessionScope.userRole == 'ADMIN'}">
            <div class="card shadow mb-4">
                <div class="card-header">
                    <h6 class="mb-0"><i class="bi bi-share me-2"></i>Share Link</h6>
                </div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${not empty shareLink}">
                            <div class="mb-2">
                                <label class="form-label small text-muted">Public share link:</label>
                                <div class="input-group input-group-sm">
                                    <input type="text" class="form-control form-control-sm"
                                           id="shareLinkUrl" readonly
                                           value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share?token=${shareLink.token}">
                                    <button class="btn btn-outline-secondary" type="button"
                                            onclick="copyShareLink()">
                                        <i class="bi bi-clipboard"></i>
                                    </button>
                                </div>
                            </div>
                            <form action="${pageContext.request.contextPath}/share" method="post">
                                <input type="hidden" name="action" value="revoke">
                                <input type="hidden" name="noteId" value="${note.id}">
                                <button type="submit" class="btn btn-sm btn-outline-danger w-100"
                                        onclick="return confirm('Revoke this share link?')">
                                    <i class="bi bi-x-circle me-1"></i>Revoke Link
                                </button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p class="text-muted small">No share link yet.</p>
                            <form action="${pageContext.request.contextPath}/share" method="post">
                                <input type="hidden" name="action" value="generate">
                                <input type="hidden" name="noteId" value="${note.id}">
                                <button type="submit" class="btn btn-sm btn-outline-primary w-100">
                                    <i class="bi bi-link-45deg me-1"></i>Generate Share Link
                                </button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:if>
    </div>
</div>

<!-- Delete Confirmation Modal -->
<div class="modal fade" id="deleteModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Confirm Delete</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p>Are you sure you want to delete "<strong id="deleteNoteTitle"></strong>"?</p>
                <p class="text-danger small">This will also delete all attachments, ratings, and share links.</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <form id="deleteForm" action="${pageContext.request.contextPath}/notes" method="post">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" id="deleteNoteId">
                    <button type="submit" class="btn btn-danger">Delete</button>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
function confirmDelete(noteId, noteTitle) {
    document.getElementById('deleteNoteId').value = noteId;
    document.getElementById('deleteNoteTitle').textContent = noteTitle;
    new bootstrap.Modal(document.getElementById('deleteModal')).show();
}

function copyShareLink() {
    const linkInput = document.getElementById('shareLinkUrl');
    linkInput.select();
    navigator.clipboard.writeText(linkInput.value).catch(() => {
        document.execCommand('copy');
    });
}
</script>

<%@ include file="includes/footer.jsp" %>
