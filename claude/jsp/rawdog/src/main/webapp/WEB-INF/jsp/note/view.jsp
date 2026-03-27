<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="${note.title} - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row">
    <div class="col-lg-8">
        <div class="card shadow mb-4">
            <div class="card-header d-flex justify-content-between align-items-center">
                <h3 class="mb-0">${note.title}</h3>
                <span class="badge ${note.public ? 'bg-success' : 'bg-secondary'}">
                    ${note.public ? 'Public' : 'Private'}
                </span>
            </div>
            <div class="card-body">
                <div class="note-content mb-3" style="white-space: pre-wrap;"><c:out value="${note.content}"/></div>

                <div class="text-muted small mb-3">
                    <span><i class="bi bi-person"></i> ${note.ownerUsername}</span> &bull;
                    <span><i class="bi bi-calendar"></i> Created: ${note.createdAt}</span> &bull;
                    <span><i class="bi bi-pencil"></i> Updated: ${note.updatedAt}</span>
                </div>

                <c:if test="${not empty attachments}">
                    <h6><i class="bi bi-paperclip"></i> Attachments</h6>
                    <ul class="list-group list-group-flush mb-3">
                        <c:forEach var="att" items="${attachments}">
                            <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                                <a href="${pageContext.request.contextPath}/files/${att.id}" target="_blank">
                                    <i class="bi bi-file-earmark"></i> ${att.originalFilename}
                                </a>
                                <small class="text-muted">${att.fileSizeDisplay}</small>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </div>

            <c:if test="${isOwner}">
                <div class="card-footer bg-transparent d-flex flex-wrap gap-2">
                    <a href="${pageContext.request.contextPath}/notes/edit?id=${note.id}"
                       class="btn btn-outline-secondary btn-sm">
                        <i class="bi bi-pencil"></i> Edit
                    </a>
                    <a href="${pageContext.request.contextPath}/notes/delete?id=${note.id}"
                       class="btn btn-outline-danger btn-sm">
                        <i class="bi bi-trash"></i> Delete
                    </a>
                    <c:choose>
                        <c:when test="${not empty shareLink}">
                            <button type="button" class="btn btn-outline-info btn-sm"
                                    onclick="copyShareLink('${pageContext.request.contextPath}/share/${shareLink.token}')">
                                <i class="bi bi-link-45deg"></i> Copy Share Link
                            </button>
                            <form method="post" action="${pageContext.request.contextPath}/notes/share" class="d-inline">
                                <input type="hidden" name="id" value="${note.id}">
                                <input type="hidden" name="action" value="revoke">
                                <button type="submit" class="btn btn-outline-warning btn-sm">
                                    <i class="bi bi-link-break"></i> Revoke Link
                                </button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <form method="post" action="${pageContext.request.contextPath}/notes/share" class="d-inline">
                                <input type="hidden" name="id" value="${note.id}">
                                <input type="hidden" name="action" value="create">
                                <button type="submit" class="btn btn-outline-success btn-sm">
                                    <i class="bi bi-share"></i> Generate Share Link
                                </button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
                <c:if test="${not empty shareLink}">
                    <div class="card-footer bg-light">
                        <small class="text-muted">Share URL:</small>
                        <input type="text" class="form-control form-control-sm mt-1" readonly
                               value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share/${shareLink.token}"
                               id="shareUrlInput" onclick="this.select()">
                    </div>
                </c:if>
            </c:if>
        </div>

        <!-- Ratings section -->
        <div class="card shadow mb-4">
            <div class="card-header">
                <h5 class="mb-0">
                    <i class="bi bi-star"></i> Ratings
                    <c:if test="${note.ratingCount > 0}">
                        <span class="ms-2 text-warning">
                            <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1" minFractionDigits="1"/>
                            / 5.0
                        </span>
                        <small class="text-muted">(${note.ratingCount} rating${note.ratingCount != 1 ? 's' : ''})</small>
                    </c:if>
                </h5>
            </div>
            <div class="card-body">
                <c:if test="${not isOwner}">
                    <div class="mb-4">
                        <h6>${not empty userRating ? 'Update Your Rating' : 'Rate This Note'}</h6>
                        <form method="post" action="${pageContext.request.contextPath}/notes/rate">
                            <input type="hidden" name="noteId" value="${note.id}">
                            <div class="mb-2">
                                <label class="form-label">Rating</label>
                                <div class="d-flex gap-2">
                                    <c:forEach var="i" begin="1" end="5">
                                        <div class="form-check">
                                            <input class="form-check-input" type="radio" name="rating"
                                                   id="star${i}" value="${i}"
                                                   ${not empty userRating && userRating.rating == i ? 'checked' : ''} required>
                                            <label class="form-check-label" for="star${i}">${i} &#9733;</label>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                            <div class="mb-2">
                                <label for="comment" class="form-label">Comment (optional)</label>
                                <textarea class="form-control" id="comment" name="comment" rows="2"><c:if test="${not empty userRating}">${userRating.comment}</c:if></textarea>
                            </div>
                            <button type="submit" class="btn btn-warning btn-sm">
                                <i class="bi bi-star"></i> ${not empty userRating ? 'Update Rating' : 'Submit Rating'}
                            </button>
                        </form>
                    </div>
                </c:if>

                <c:choose>
                    <c:when test="${empty ratings}">
                        <p class="text-muted">No ratings yet.</p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="r" items="${ratings}">
                            <div class="border-bottom pb-2 mb-2">
                                <div class="d-flex justify-content-between">
                                    <strong>${r.raterUsername}</strong>
                                    <span class="text-warning">
                                        <c:forEach var="s" begin="1" end="${r.rating}">&#9733;</c:forEach>
                                        <c:forEach var="s" begin="${r.rating + 1}" end="5">&#9734;</c:forEach>
                                    </span>
                                </div>
                                <c:if test="${not empty r.comment}">
                                    <p class="mb-0 text-muted small mt-1"><c:out value="${r.comment}"/></p>
                                </c:if>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <div class="col-lg-4">
        <div class="card shadow">
            <div class="card-body">
                <h6><i class="bi bi-info-circle"></i> Note Info</h6>
                <ul class="list-unstyled small text-muted">
                    <li><i class="bi bi-person"></i> Owner: ${note.ownerUsername}</li>
                    <li><i class="bi bi-calendar-plus"></i> Created: ${note.createdAt}</li>
                    <li><i class="bi bi-calendar-check"></i> Updated: ${note.updatedAt}</li>
                    <li><i class="bi bi-eye"></i> Visibility: ${note.public ? 'Public' : 'Private'}</li>
                    <li><i class="bi bi-paperclip"></i> Attachments: ${not empty attachments ? attachments.size() : 0}</li>
                    <li><i class="bi bi-star"></i> Ratings: ${note.ratingCount}</li>
                </ul>
                <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary btn-sm w-100">
                    <i class="bi bi-arrow-left"></i> Back to My Notes
                </a>
            </div>
        </div>
    </div>
</div>

<script>
function copyShareLink(url) {
    const fullUrl = window.location.protocol + '//' + window.location.host + url;
    navigator.clipboard.writeText(fullUrl).then(function() {
        alert('Share link copied to clipboard!');
    }).catch(function() {
        document.getElementById('shareUrlInput').select();
        document.execCommand('copy');
        alert('Share link copied!');
    });
}
</script>

<%@ include file="../layout/footer.jsp" %>
