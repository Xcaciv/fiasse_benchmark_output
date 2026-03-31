<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:include page="../layout.jsp">
    <jsp:param name="pageTitle" value="${note.title}"/>
</jsp:include>
<jsp:body>
<div class="row">
    <div class="col-lg-8">
        <div class="card mb-4">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-3">
                    <h2>${note.title}</h2>
                    <c:if test="${note.public}">
                        <span class="badge bg-success">Public</span>
                    </c:if>
                </div>
                
                <div class="text-muted mb-4">
                    <c:choose>
                        <c:when test="${sharedNote}">
                            <span>By ${note.owner.username}</span>
                        </c:when>
                        <c:otherwise>
                            <span>By <a href="#">${note.owner.username}</a></span>
                        </c:otherwise>
                    </c:choose>
                    &bull; 
                    <fmt:formatDate value="${note.createdAt}" pattern="MMMM d, yyyy"/>
                    <c:if test="${note.updatedAt ne note.createdAt}">
                        &bull; Updated <fmt:formatDate value="${note.updatedAt}" pattern="MMMM d, yyyy"/>
                    </c:if>
                </div>
                
                <div class="note-content mb-4" style="white-space: pre-wrap;">${note.content}</div>
                
                <c:if test="${not empty note.attachments}">
                    <h5 class="mt-4">Attachments</h5>
                    <div class="row">
                        <c:forEach var="attachment" items="${note.attachments}">
                            <div class="col-md-6 col-lg-4 mb-3">
                                <div class="card">
                                    <div class="card-body d-flex align-items-center">
                                        <i class="bi bi-file-earmark attachment-icon me-3"></i>
                                        <div class="flex-grow-1 overflow-hidden">
                                            <div class="text-truncate">${attachment.originalFilename}</div>
                                            <small class="text-muted">
                                                <fmt:formatNumber value="${attachment.fileSize / 1024}" pattern="#,##0"/> KB
                                            </small>
                                        </div>
                                        <a href="${pageContext.request.contextPath}/download?file=${attachment.storedFilename}" 
                                           class="btn btn-sm btn-outline-primary">
                                            <i class="bi bi-download"></i>
                                        </a>
                                        <c:if test="${not sharedNote and (note.userId eq sessionScope.user.id or sessionScope.user.admin)}">
                                            <a href="${pageContext.request.contextPath}/notes?action=deleteAttachment&attachmentId=${attachment.id}&id=${note.id}" 
                                               class="btn btn-sm btn-outline-danger ms-1"
                                               onclick="return confirm('Delete this attachment?')">
                                                <i class="bi bi-trash"></i>
                                            </a>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>
                
                <c:if test="${not sharedNote and (note.userId eq sessionScope.user.id or sessionScope.user.admin)}">
                    <div class="mt-4">
                        <h5>Upload Attachment</h5>
                        <form method="post" action="${pageContext.request.contextPath}/notes" 
                              enctype="multipart/form-data" class="d-flex gap-2 align-items-center">
                            <input type="hidden" name="action" value="uploadAttachment">
                            <input type="hidden" name="id" value="${note.id}">
                            <input type="file" class="form-control" name="file" required>
                            <button type="submit" class="btn btn-outline-primary">Upload</button>
                        </form>
                        <small class="text-muted">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG (max 10MB)</small>
                    </div>
                </c:if>
            </div>
        </div>
        
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0">Ratings</h5>
            </div>
            <div class="card-body">
                <c:if test="${note.ratingCount > 0}">
                    <div class="mb-3">
                        <span class="star-rating" style="font-size: 1.5rem;">
                            <i class="bi bi-star-fill"></i> 
                            <c:choose>
                                <c:when test="${note.averageRating >= 4.5}">Excellent</c:when>
                                <c:when test="${note.averageRating >= 3.5}">Good</c:when>
                                <c:when test="${note.averageRating >= 2.5}">Average</c:when>
                                <c:otherwise>Below Average</c:otherwise>
                            </c:choose>
                        </span>
                        <span class="ms-2 text-muted">
                            ${String.format("%.1f", note.averageRating)} / 5.0 (${note.ratingCount} ratings)
                        </span>
                    </div>
                </c:if>
                
                <c:forEach var="rating" items="${note.ratings}">
                    <div class="border-bottom py-3">
                        <div class="d-flex justify-content-between">
                            <strong>${rating.user.username}</strong>
                            <span class="star-rating">
                                <c:forEach begin="1" end="5" var="i">
                                    <i class="bi ${i <= rating.value ? 'bi-star-fill' : 'bi-star'}"></i>
                                </c:forEach>
                            </span>
                        </div>
                        <c:if test="${not empty rating.comment}">
                            <p class="mb-0 mt-2">${rating.comment}</p>
                        </c:if>
                        <small class="text-muted">
                            <fmt:formatDate value="${rating.createdAt}" pattern="MMM d, yyyy"/>
                        </small>
                    </div>
                </c:forEach>
                
                <c:if test="${empty note.ratings}">
                    <p class="text-muted">No ratings yet.</p>
                </c:if>
                
                <c:if test="${not sharedNote and not empty sessionScope.user}">
                    <hr>
                    <c:set var="userRating" value="${null}"/>
                    <c:forEach var="rating" items="${note.ratings}">
                        <c:if test="${rating.user.id eq sessionScope.user.id}">
                            <c:set var="userRating" value="${rating}"/>
                        </c:if>
                    </c:forEach>
                    
                    <h5>${userRating ne null ? 'Update Your Rating' : 'Rate This Note'}</h5>
                    <form method="post" action="${pageContext.request.contextPath}/ratings">
                        <input type="hidden" name="action" value="${userRating ne null ? 'update' : 'create'}">
                        <input type="hidden" name="noteId" value="${note.id}">
                        
                        <div class="mb-3">
                            <label class="form-label">Rating</label>
                            <select class="form-select" name="value" required>
                                <option value="">Select rating...</option>
                                <option value="1" ${userRating.value == 1 ? 'selected' : ''}>1 - Poor</option>
                                <option value="2" ${userRating.value == 2 ? 'selected' : ''}>2 - Fair</option>
                                <option value="3" ${userRating.value == 3 ? 'selected' : ''}>3 - Good</option>
                                <option value="4" ${userRating.value == 4 ? 'selected' : ''}>4 - Very Good</option>
                                <option value="5" ${userRating.value == 5 ? 'selected' : ''}>5 - Excellent</option>
                            </select>
                        </div>
                        
                        <div class="mb-3">
                            <label class="form-label">Comment (optional)</label>
                            <textarea class="form-control" name="comment" rows="2">${userRating.comment}</textarea>
                        </div>
                        
                        <button type="submit" class="btn btn-primary">${userRating ne null ? 'Update Rating' : 'Submit Rating'}</button>
                        <c:if test="${userRating ne null}">
                            <button type="submit" formaction="${pageContext.request.contextPath}/ratings" 
                                    formmethod="post" name="action" value="delete"
                                    class="btn btn-outline-danger">Remove Rating</button>
                        </c:if>
                    </form>
                </c:if>
            </div>
        </div>
    </div>
    
    <div class="col-lg-4">
        <c:if test="${not sharedNote and (note.userId eq sessionScope.user.id or sessionScope.user.admin)}">
            <div class="card mb-3">
                <div class="card-header">
                    <h5 class="mb-0">Actions</h5>
                </div>
                <div class="card-body">
                    <div class="d-grid gap-2">
                        <a href="${pageContext.request.contextPath}/notes?action=edit&id=${note.id}" 
                           class="btn btn-outline-primary">
                            <i class="bi bi-pencil"></i> Edit Note
                        </a>
                        <a href="${pageContext.request.contextPath}/share?id=${note.id}" 
                           class="btn btn-outline-success">
                            <i class="bi bi-share"></i> Share Note
                        </a>
                        <a href="${pageContext.request.contextPath}/notes?action=delete&id=${note.id}" 
                           class="btn btn-outline-danger"
                           onclick="return confirm('Are you sure you want to delete this note?')">
                            <i class="bi bi-trash"></i> Delete Note
                        </a>
                    </div>
                </div>
            </div>
        </c:if>
        
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0">Share</h5>
            </div>
            <div class="card-body">
                <p class="text-muted">Share this note with others using a link:</p>
                <div class="input-group mb-3">
                    <input type="text" class="form-control" id="shareLink" 
                           value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share?token=${shareLink.token}" 
                           readonly>
                    <button class="btn btn-outline-secondary" onclick="copyShareLink()">
                        <i class="bi bi-clipboard"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
function copyShareLink() {
    var copyText = document.getElementById("shareLink");
    copyText.select();
    copyText.setSelectionRange(0, 99999);
    navigator.clipboard.writeText(copyText.value);
    alert("Link copied to clipboard!");
}
</script>
</jsp:body>
