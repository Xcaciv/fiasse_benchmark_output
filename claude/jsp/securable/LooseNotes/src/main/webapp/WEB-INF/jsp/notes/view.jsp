<%--
  Note detail view with ratings, attachments, and share link management.
  SSEM: Integrity - all user content output via c:out (XSS prevention).
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="${note.title} - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row">
    <div class="col-md-9">
        <%-- Note content --%>
        <div class="d-flex justify-content-between align-items-start mb-3">
            <div>
                <h1 class="h2"><c:out value="${note.title}"/></h1>
                <p class="text-muted mb-0">
                    By <strong><c:out value="${note.ownerUsername}"/></strong> &bull;
                    <c:out value="${note.public ? 'Public' : 'Private'}"/>
                    <c:if test="${note.ratingCount > 0}">
                        &bull; &#9733; <fmt:formatNumber value="${note.averageRating}" maxFractionDigits="1"/>
                        (<c:out value="${note.ratingCount}"/> ratings)
                    </c:if>
                </p>
            </div>
            <c:if test="${currentUser.id == note.userId}">
                <div class="btn-group">
                    <a href="${pageContext.request.contextPath}/notes/edit/<c:out value='${note.id}'/>"
                       class="btn btn-outline-secondary btn-sm">Edit</a>
                    <a href="${pageContext.request.contextPath}/notes/delete/<c:out value='${note.id}'/>"
                       class="btn btn-outline-danger btn-sm">Delete</a>
                </div>
            </c:if>
        </div>

        <c:if test="${param.ratingError eq 'true'}">
            <div class="alert alert-warning">Could not submit rating. Please try again.</div>
        </c:if>

        <div class="card mb-4">
            <div class="card-body note-content">
                <pre class="mb-0" style="white-space:pre-wrap;font-family:inherit;"><c:out value="${note.content}"/></pre>
            </div>
        </div>

        <%-- Attachments --%>
        <c:if test="${not empty note.attachments}">
            <h4 class="h5 mb-3">Attachments</h4>
            <ul class="list-group mb-4">
                <c:forEach var="att" items="${note.attachments}">
                    <li class="list-group-item d-flex justify-content-between align-items-center">
                        <span>
                            <c:out value="${att.originalFilename}"/>
                            <small class="text-muted ms-2">(<c:out value="${att.formattedFileSize}"/>)</small>
                        </span>
                        <a href="${pageContext.request.contextPath}/attachments/download/<c:out value='${att.id}'/>"
                           class="btn btn-sm btn-outline-secondary">Download</a>
                    </li>
                </c:forEach>
            </ul>
        </c:if>

        <%-- Upload attachment (owner only) --%>
        <c:if test="${currentUser.id == note.userId}">
            <div class="card mb-4">
                <div class="card-header">Upload Attachment</div>
                <div class="card-body">
                    <form method="post" action="${pageContext.request.contextPath}/attachments/upload"
                          enctype="multipart/form-data">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                        <input type="hidden" name="noteId" value="<c:out value='${note.id}'/>">
                        <div class="input-group">
                            <input type="file" class="form-control" name="file" required
                                   accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg">
                            <button type="submit" class="btn btn-outline-primary">Upload</button>
                        </div>
                        <div class="form-text">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG. Max 10 MB.</div>
                    </form>
                </div>
            </div>
        </c:if>

        <%-- Ratings section --%>
        <h4 class="h5 mb-3" id="ratings">Ratings & Comments</h4>

        <%-- Submit rating form --%>
        <c:if test="${currentUser.id != note.userId}">
            <div class="card mb-3">
                <div class="card-header">
                    <c:out value="${userRating != null ? 'Update Your Rating' : 'Rate This Note'}"/>
                </div>
                <div class="card-body">
                    <form method="post" action="${pageContext.request.contextPath}/ratings">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                        <input type="hidden" name="noteId" value="<c:out value='${note.id}'/>">
                        <div class="mb-2">
                            <label class="form-label">Rating</label>
                            <div class="d-flex gap-2">
                                <c:forEach begin="1" end="5" var="i">
                                    <div class="form-check form-check-inline">
                                        <input class="form-check-input" type="radio" name="value"
                                               id="star${i}" value="${i}"
                                               ${userRating != null && userRating.value == i ? 'checked' : ''} required>
                                        <label class="form-check-label" for="star${i}">${i} &#9733;</label>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                        <div class="mb-2">
                            <label for="comment" class="form-label">Comment (optional)</label>
                            <textarea class="form-control" id="comment" name="comment"
                                      rows="2" maxlength="1000"><c:out value="${userRating != null ? userRating.comment : ''}"/></textarea>
                        </div>
                        <button type="submit" class="btn btn-sm btn-primary">Submit Rating</button>
                    </form>
                </div>
            </div>
        </c:if>

        <%-- Existing ratings --%>
        <c:choose>
            <c:when test="${empty ratings}">
                <p class="text-muted">No ratings yet. Be the first to rate this note.</p>
            </c:when>
            <c:otherwise>
                <c:forEach var="r" items="${ratings}">
                    <div class="card mb-2">
                        <div class="card-body py-2">
                            <div class="d-flex justify-content-between">
                                <strong><c:out value="${r.raterUsername}"/></strong>
                                <span>
                                    <c:forEach begin="1" end="${r.value}">&#9733;</c:forEach>
                                    <c:forEach begin="${r.value + 1}" end="5">&#9734;</c:forEach>
                                </span>
                            </div>
                            <c:if test="${not empty r.comment}">
                                <p class="mb-0 mt-1 small"><c:out value="${r.comment}"/></p>
                            </c:if>
                        </div>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- Sidebar: Share link management --%>
    <c:if test="${currentUser.id == note.userId}">
        <div class="col-md-3">
            <div class="card">
                <div class="card-header">Share Link</div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${shareLink != null}">
                            <c:set var="baseUrl" value="${appConfig['app.baseUrl']}"/>
                            <p class="small">Anyone with this link can view this note:</p>
                            <%-- Display share URL - composed of trusted base URL + safe token --%>
                            <div class="input-group input-group-sm mb-2">
                                <input type="text" class="form-control form-control-sm"
                                       id="shareUrl" readonly
                                       value="${pageContext.request.contextPath}/share/${shareLink.token}">
                                <button class="btn btn-outline-secondary" onclick="copyShareUrl()">Copy</button>
                            </div>
                            <form method="post"
                                  action="${pageContext.request.contextPath}/notes/revoke-share/<c:out value='${note.id}'/>">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                <button type="submit" class="btn btn-sm btn-outline-danger w-100">Revoke Link</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p class="small text-muted">No active share link.</p>
                            <form method="post"
                                  action="${pageContext.request.contextPath}/notes/share/<c:out value='${note.id}'/>">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                <button type="submit" class="btn btn-sm btn-outline-primary w-100">Generate Share Link</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </c:if>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
