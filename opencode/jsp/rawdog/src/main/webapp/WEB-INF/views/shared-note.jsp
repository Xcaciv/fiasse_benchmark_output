<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<div class="card shadow border-info">
    <div class="card-header bg-info text-white">
        <h3 class="mb-0"><i class="fas fa-share-alt"></i> Shared Note</h3>
        <small>
            <i class="fas fa-user"></i> ${note.owner.username} &bull;
            <span class="badge badge-light text-success">Public</span>
        </small>
    </div>
    <div class="card-body">
        <h2 class="card-title">${note.title}</h2>
        <hr>
        <div class="note-content mb-4">
            <p style="white-space: pre-wrap;">${note.content}</p>
        </div>
        
        <hr>
        
        <h5><i class="fas fa-paperclip"></i> Attachments (${attachments.size()})</h5>
        <c:if test="${empty attachments}">
            <p class="text-muted">No attachments.</p>
        </c:if>
        <c:forEach var="attachment" items="${attachments}">
            <div class="d-flex justify-content-between align-items-center border rounded p-2 mb-2">
                <div>
                    <i class="fas fa-file"></i> ${attachment.originalFilename}
                    <small class="text-muted">(${attachment.fileSizeFormatted})</small>
                </div>
                <a href="${pageContext.request.contextPath}${attachment.filePath}" class="btn btn-sm btn-outline-primary" download>
                    <i class="fas fa-download"></i> Download
                </a>
            </div>
        </c:forEach>
        
        <hr>
        
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
    <div class="card-footer text-muted">
        <small>Created: ${note.createdAt}</small>
    </div>
</div>

<div class="mt-3 text-center">
    <a href="${pageContext.request.contextPath}/" class="btn btn-primary">
        <i class="fas fa-home"></i> Go to Home
    </a>
    <a href="${pageContext.request.contextPath}/search" class="btn btn-outline-primary">
        <i class="fas fa-search"></i> Search Notes
    </a>
</div>

<jsp:include page="footer.jsp"/>
