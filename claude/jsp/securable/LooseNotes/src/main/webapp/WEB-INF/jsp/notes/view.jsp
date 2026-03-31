<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container mt-4">

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <c:out value="${successMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <c:out value="${errorMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <%-- Note Header --%>
    <div class="d-flex justify-content-between align-items-start mb-3">
        <div>
            <h1 class="h2 mb-1"><c:out value="${note.title}"/></h1>
            <div class="d-flex align-items-center gap-2 text-muted small">
                <c:choose>
                    <c:when test="${note.visibility == 'PUBLIC'}">
                        <span class="badge bg-success">PUBLIC</span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge bg-secondary">PRIVATE</span>
                    </c:otherwise>
                </c:choose>
                <span>By <strong><c:out value="${note.authorUsername}"/></strong></span>
                <span>&middot;</span>
                <span>Created <fmt:formatDate value="${note.createdAt}" pattern="MMM dd, yyyy"/></span>
                <c:if test="${note.updatedAt != note.createdAt}">
                    <span>&middot;</span>
                    <span>Updated <fmt:formatDate value="${note.updatedAt}" pattern="MMM dd, yyyy"/></span>
                </c:if>
            </div>
        </div>
        <c:if test="${note.userId == currentUser.id}">
            <div class="d-flex gap-2">
                <a href="${pageContext.request.contextPath}/notes/${note.id}/edit" class="btn btn-outline-primary btn-sm">Edit</a>
                <a href="${pageContext.request.contextPath}/notes/${note.id}/delete" class="btn btn-outline-danger btn-sm">Delete</a>
            </div>
        </c:if>
    </div>

    <div class="row g-4">
        <%-- Main Content --%>
        <div class="col-lg-8">
            <div class="card shadow-sm mb-4">
                <div class="card-body">
                    <pre class="mb-0" style="white-space: pre-wrap; word-wrap: break-word; font-family: inherit;"><c:out value="${note.content}"/></pre>
                </div>
            </div>

            <%-- Attachments --%>
            <c:if test="${not empty attachments}">
                <div class="card shadow-sm mb-4">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Attachments</h5>
                    </div>
                    <ul class="list-group list-group-flush">
                        <c:forEach var="attachment" items="${attachments}">
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                <span><c:out value="${attachment.originalFilename}"/></span>
                                <a href="${pageContext.request.contextPath}/attachments/${attachment.id}"
                                   class="btn btn-sm btn-outline-secondary">Download</a>
                            </li>
                        </c:forEach>
                    </ul>
                </div>
            </c:if>

            <%-- Ratings Section --%>
            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0">Ratings</h5>
                    <span class="text-muted small">
                        <c:choose>
                            <c:when test="${ratingCount > 0}">
                                Avg: <strong><fmt:formatNumber value="${averageRating}" maxFractionDigits="1"/></strong>/5
                                (<c:out value="${ratingCount}"/> rating<c:if test="${ratingCount != 1}">s</c:if>)
                            </c:when>
                            <c:otherwise>No ratings yet</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <div class="card-body">
                    <%-- Rating Form --%>
                    <c:if test="${not empty currentUser}">
                        <c:choose>
                            <c:when test="${empty userRating}">
                                <h6 class="mb-3">Rate this note</h6>
                                <form method="post" action="${pageContext.request.contextPath}/notes/${note.id}/rate" class="mb-4">
                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                    <div class="row g-3 align-items-end">
                                        <div class="col-auto">
                                            <label for="stars" class="form-label">Stars</label>
                                            <select id="stars" name="stars" class="form-select form-select-sm" style="width: auto;">
                                                <option value="1">1 &#9733;</option>
                                                <option value="2">2 &#9733;&#9733;</option>
                                                <option value="3" selected>3 &#9733;&#9733;&#9733;</option>
                                                <option value="4">4 &#9733;&#9733;&#9733;&#9733;</option>
                                                <option value="5">5 &#9733;&#9733;&#9733;&#9733;&#9733;</option>
                                            </select>
                                        </div>
                                        <div class="col">
                                            <label for="ratingComment" class="form-label">Comment <span class="text-muted">(optional)</span></label>
                                            <textarea id="ratingComment" name="comment" class="form-control form-control-sm" rows="2" maxlength="500"></textarea>
                                        </div>
                                        <div class="col-auto">
                                            <button type="submit" class="btn btn-primary btn-sm">Submit Rating</button>
                                        </div>
                                    </div>
                                </form>
                                <hr>
                            </c:when>
                            <c:otherwise>
                                <div class="alert alert-info d-flex align-items-center mb-4" role="alert">
                                    <span>Your rating: <strong><c:out value="${userRating.stars}"/>/5</strong>
                                    <c:if test="${not empty userRating.comment}">
                                        &mdash; <em><c:out value="${userRating.comment}"/></em>
                                    </c:if></span>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </c:if>

                    <%-- Ratings List --%>
                    <c:choose>
                        <c:when test="${empty ratings}">
                            <p class="text-muted mb-0">No ratings yet. Be the first to rate!</p>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="rating" items="${ratings}">
                                <div class="d-flex gap-3 mb-3 pb-3 border-bottom">
                                    <div class="flex-grow-1">
                                        <div class="d-flex align-items-center gap-2 mb-1">
                                            <strong><c:out value="${rating.username}"/></strong>
                                            <span class="text-warning">
                                                <c:forEach begin="1" end="${rating.stars}" var="i">&#9733;</c:forEach><c:forEach begin="${rating.stars + 1}" end="5" var="i">&#9734;</c:forEach>
                                            </span>
                                            <span class="text-muted small">
                                                <fmt:formatDate value="${rating.createdAt}" pattern="MMM dd, yyyy"/>
                                            </span>
                                        </div>
                                        <c:if test="${not empty rating.comment}">
                                            <p class="mb-0 text-muted"><c:out value="${rating.comment}"/></p>
                                        </c:if>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <%-- Sidebar --%>
        <div class="col-lg-4">
            <%-- Share Link --%>
            <c:if test="${note.userId == currentUser.id}">
                <div class="card shadow-sm mb-4">
                    <div class="card-header">
                        <h5 class="card-title mb-0">Share Link</h5>
                    </div>
                    <div class="card-body">
                        <c:choose>
                            <c:when test="${not empty shareLink}">
                                <p class="text-muted small mb-2">Anyone with this link can view the note:</p>
                                <div class="input-group mb-3">
                                    <input type="text"
                                           class="form-control form-control-sm"
                                           id="shareUrl"
                                           value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share/${shareLink.token}"
                                           readonly>
                                    <button class="btn btn-outline-secondary btn-sm"
                                            type="button"
                                            onclick="navigator.clipboard.writeText(document.getElementById('shareUrl').value)">Copy</button>
                                </div>
                                <form method="post" action="${pageContext.request.contextPath}/notes/${note.id}/share/revoke">
                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                    <button type="submit" class="btn btn-outline-danger btn-sm w-100">Revoke Share Link</button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <p class="text-muted small mb-3">No active share link. Generate one to share this note publicly.</p>
                                <form method="post" action="${pageContext.request.contextPath}/notes/${note.id}/share/generate">
                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                    <button type="submit" class="btn btn-outline-primary btn-sm w-100">Generate Share Link</button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </c:if>

            <%-- Note Info --%>
            <div class="card shadow-sm">
                <div class="card-header">
                    <h5 class="card-title mb-0">Note Info</h5>
                </div>
                <ul class="list-group list-group-flush">
                    <li class="list-group-item d-flex justify-content-between">
                        <span class="text-muted">Author</span>
                        <strong><c:out value="${note.authorUsername}"/></strong>
                    </li>
                    <li class="list-group-item d-flex justify-content-between">
                        <span class="text-muted">Visibility</span>
                        <c:choose>
                            <c:when test="${note.visibility == 'PUBLIC'}">
                                <span class="badge bg-success align-self-center">PUBLIC</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge bg-secondary align-self-center">PRIVATE</span>
                            </c:otherwise>
                        </c:choose>
                    </li>
                    <li class="list-group-item d-flex justify-content-between">
                        <span class="text-muted">Created</span>
                        <span><fmt:formatDate value="${note.createdAt}" pattern="MMM dd, yyyy"/></span>
                    </li>
                    <c:if test="${note.updatedAt != note.createdAt}">
                        <li class="list-group-item d-flex justify-content-between">
                            <span class="text-muted">Updated</span>
                            <span><fmt:formatDate value="${note.updatedAt}" pattern="MMM dd, yyyy"/></span>
                        </li>
                    </c:if>
                    <li class="list-group-item d-flex justify-content-between">
                        <span class="text-muted">Avg Rating</span>
                        <span>
                            <c:choose>
                                <c:when test="${ratingCount > 0}">
                                    <fmt:formatNumber value="${averageRating}" maxFractionDigits="1"/>/5
                                </c:when>
                                <c:otherwise>N/A</c:otherwise>
                            </c:choose>
                        </span>
                    </li>
                </ul>
            </div>
        </div>
    </div>

    <div class="mt-3">
        <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary btn-sm">&larr; Back to Notes</a>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
