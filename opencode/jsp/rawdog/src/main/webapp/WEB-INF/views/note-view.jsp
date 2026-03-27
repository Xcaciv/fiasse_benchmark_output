<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<c:if test="${not empty error}">
    <c:choose>
        <c:when test="${error == 'invalidFileType'}">
            <div class="alert alert-danger">Invalid file type. Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG</div>
        </c:when>
        <c:when test="${error == 'fileTooLarge'}">
            <div class="alert alert-danger">File too large. Maximum size is 10MB.</div>
        </c:when>
        <c:when test="${error == 'uploadFailed'}">
            <div class="alert alert-danger">Failed to upload file. Please try again.</div>
        </c:when>
    </c:choose>
</c:if>

<div class="card shadow">
    <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <div>
            <h3 class="mb-0"><i class="fas fa-file-alt"></i> ${note.title}</h3>
            <small>
                <i class="fas fa-user"></i> ${note.owner.username} &bull;
                <span class="badge badge-${note.isPublic ? 'light text-success' : 'secondary'}">
                    ${note.isPublic ? 'Public' : 'Private'}
                </span>
            </small>
        </div>
        <div>
            <c:if test="${isOwner}">
                <a href="${pageContext.request.contextPath}/notes?action=edit&id=${note.id}" class="btn btn-sm btn-light">
                    <i class="fas fa-edit"></i> Edit
                </a>
                <a href="${pageContext.request.contextPath}/notes?action=delete&id=${note.id}" class="btn btn-sm btn-danger">
                    <i class="fas fa-trash"></i> Delete
                </a>
            </c:if>
        </div>
    </div>
    <div class="card-body">
        <div class="note-content mb-4">
            <p style="white-space: pre-wrap;">${note.content}</p>
        </div>
        
        <hr>
        
        <div class="row">
            <div class="col-md-6">
                <h5><i class="fas fa-paperclip"></i> Attachments (${attachments.size()})</h5>
                <c:if test="${isOwner}">
                    <form action="${pageContext.request.contextPath}/upload" method="post" enctype="multipart/form-data" class="mb-3">
                        <input type="hidden" name="noteId" value="${note.id}">
                        <div class="input-group">
                            <input type="file" class="form-control" name="file" required>
                            <div class="input-group-append">
                                <button type="submit" class="btn btn-outline-primary">
                                    <i class="fas fa-upload"></i> Upload
                                </button>
                            </div>
                        </div>
                        <small class="form-text text-muted">PDF, DOC, DOCX, TXT, PNG, JPG, JPEG (max 10MB)</small>
                    </form>
                </c:if>
                <c:if test="${empty attachments}">
                    <p class="text-muted">No attachments yet.</p>
                </c:if>
                <c:forEach var="attachment" items="${attachments}">
                    <div class="d-flex justify-content-between align-items-center border rounded p-2 mb-2">
                        <div>
                            <i class="fas fa-file"></i> ${attachment.originalFilename}
                            <small class="text-muted">(${attachment.fileSizeFormatted})</small>
                        </div>
                        <div>
                            <a href="${pageContext.request.contextPath}${attachment.filePath}" class="btn btn-sm btn-outline-primary" download>
                                <i class="fas fa-download"></i>
                            </a>
                            <c:if test="${isOwner}">
                                <a href="${pageContext.request.contextPath}/upload?action=delete&noteId=${note.id}&attachmentId=${attachment.id}" class="btn btn-sm btn-outline-danger">
                                    <i class="fas fa-trash"></i>
                                </a>
                            </c:if>
                        </div>
                    </div>
                </c:forEach>
            </div>
            <div class="col-md-6">
                <h5><i class="fas fa-star text-warning"></i> Ratings (${note.ratingCount})</h5>
                <c:if test="${note.ratingCount > 0}">
                    <div class="mb-2">
                        <span class="h4 text-warning">
                            <i class="fas fa-star"></i> ${String.format("%.1f", note.averageRating)}
                        </span>
                        <small class="text-muted">/ 5</small>
                    </div>
                </c:if>
                
                <c:if test="${not empty sessionScope.loggedInUser && !isOwner}">
                    <form action="${pageContext.request.contextPath}/ratings" method="post" class="mb-3">
                        <input type="hidden" name="noteId" value="${note.id}">
                        <input type="hidden" name="action" value="${empty userRating ? 'submit' : 'update'}">
                        <c:if test="${not empty userRating}">
                            <input type="hidden" name="ratingId" value="${userRating.id}">
                        </c:if>
                        <div class="form-group">
                            <label>Your Rating:</label>
                            <select name="rating" class="form-control">
                                <option value="1" ${not empty userRating && userRating.rating == 1 ? 'selected' : ''}>1 Star</option>
                                <option value="2" ${not empty userRating && userRating.rating == 2 ? 'selected' : ''}>2 Stars</option>
                                <option value="3" ${not empty userRating && userRating.rating == 3 ? 'selected' : ''}>3 Stars</option>
                                <option value="4" ${not empty userRating && userRating.rating == 4 ? 'selected' : ''}>4 Stars</option>
                                <option value="5" ${not empty userRating && userRating.rating == 5 ? 'selected' : ''}>5 Stars</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Comment:</label>
                            <textarea name="comment" class="form-control" rows="2" placeholder="Optional comment">${userRating.comment}</textarea>
                        </div>
                        <button type="submit" class="btn btn-${empty userRating ? 'primary' : 'secondary'}">
                            <i class="fas fa-${empty userRating ? 'star' : 'edit'}"></i> ${empty userRating ? 'Submit Rating' : 'Update Rating'}
                        </button>
                    </form>
                </c:if>
                
                <c:forEach var="rating" items="${ratings}">
                    <div class="border rounded p-2 mb-2">
                        <div class="d-flex justify-content-between">
                            <strong class="text-warning">${rating.starDisplay}</strong>
                            <small class="text-muted">${rating.createdAt}</small>
                        </div>
                        <div class="small text-muted">
                            <i class="fas fa-user"></i> ${rating.rater.username}
                        </div>
                        <c:if test="${not empty rating.comment}">
                            <p class="mb-0 mt-1">${rating.comment}</p>
                        </c:if>
                    </div>
                </c:forEach>
            </div>
        </div>
    </div>
    <div class="card-footer text-muted">
        <small>
            Created: ${note.createdAt}
            <c:if test="${not empty note.updatedAt}">
                &bull; Updated: ${note.updatedAt}
            </c:if>
        </small>
    </div>
</div>

<div class="mt-3">
    <a href="${pageContext.request.contextPath}/notes" class="btn btn-secondary">
        <i class="fas fa-arrow-left"></i> Back to Notes
    </a>
    
    <c:if test="${isOwner}">
        <div class="float-right">
            <form action="${pageContext.request.contextPath}/notes" method="post" class="d-inline">
                <input type="hidden" name="action" value="share">
                <input type="hidden" name="id" value="${note.id}">
                <input type="hidden" name="enableShare" value="${note.shareEnabled ? 'false' : 'true'}">
                <button type="submit" class="btn btn-${note.shareEnabled ? 'warning' : 'success'}">
                    <i class="fas fa-share-alt"></i> ${note.shareEnabled ? 'Disable Sharing' : 'Enable Sharing'}
                </button>
            </form>
            <c:if test="${note.shareEnabled && not empty note.shareToken}">
                <button class="btn btn-info" onclick="copyShareLink()">
                    <i class="fas fa-link"></i> Copy Share Link
                </button>
                <input type="text" id="shareLink" value="${pageContext.request.contextPath}/share?token=${note.shareToken}" readonly class="d-none">
            </c:if>
        </div>
    </c:if>
</div>

<script>
function copyShareLink() {
    var copyText = document.getElementById("shareLink");
    copyText.classList.remove("d-none");
    copyText.select();
    document.execCommand("copy");
    copyText.classList.add("d-none");
    alert("Share link copied to clipboard!");
}
</script>

<jsp:include page="footer.jsp"/>
