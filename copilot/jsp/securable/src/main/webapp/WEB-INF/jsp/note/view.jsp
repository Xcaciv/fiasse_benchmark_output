<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="${note.title}"/>
</jsp:include>

<div class="d-flex justify-content-between align-items-start mb-3">
    <div>
        <h1 class="h2"><c:out value="${note.title}"/></h1>
        <small class="text-muted">
            By <strong><c:out value="${note.authorUsername}"/></strong> &bull;
            <c:out value="${note.createdAt}"/>
            <c:if test="${note.public}">
                &bull; <span class="badge bg-success">Public</span>
            </c:if>
        </small>
    </div>
    <c:if test="${sessionScope.userId == note.userId}">
        <a href="${pageContext.request.contextPath}/notes/${note.id}/edit" class="btn btn-outline-secondary btn-sm">Edit</a>
    </c:if>
</div>

<!-- Note content (output-encoded by c:out) -->
<div class="card mb-4">
    <div class="card-body">
        <pre class="note-content"><c:out value="${note.content}"/></pre>
    </div>
</div>

<!-- Attachments -->
<c:if test="${not empty attachments}">
<div class="mb-4">
    <h5>Attachments</h5>
    <ul class="list-group">
        <c:forEach var="a" items="${attachments}">
            <li class="list-group-item">
                <a href="${pageContext.request.contextPath}/attachments/${a.id}/download">
                    📎 <c:out value="${a.originalFilename}"/>
                </a>
                <small class="text-muted ms-2">(<c:out value="${a.mimeType}"/>)</small>
            </li>
        </c:forEach>
    </ul>
</div>
</c:if>

<!-- Average Rating -->
<div class="mb-4">
    <h5>Rating</h5>
    <c:choose>
        <c:when test="${ratingCount > 0}">
            <span class="fs-4">⭐ <c:out value="${String.format('%.1f', avgRating)}"/></span>
            <span class="text-muted">(<c:out value="${ratingCount}"/> ratings)</span>
        </c:when>
        <c:otherwise><span class="text-muted">No ratings yet.</span></c:otherwise>
    </c:choose>
</div>

<!-- Rate this note (only if not owner) -->
<c:if test="${sessionScope.userId != note.userId}">
<div class="card mb-4">
    <div class="card-body">
        <h5 class="card-title">${empty userRating ? 'Rate this note' : 'Update your rating'}</h5>
        <form method="post" action="${pageContext.request.contextPath}/ratings/submit">
            <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
            <input type="hidden" name="noteId" value="${note.id}">
            <div class="mb-2">
                <label class="form-label">Stars</label>
                <select name="stars" class="form-select w-auto d-inline-block">
                    <c:forEach begin="1" end="5" var="s">
                        <option value="${s}" ${not empty userRating && userRating.stars == s ? 'selected' : ''}>
                            ${s} ⭐
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="mb-2">
                <textarea name="comment" class="form-control" rows="2"
                          placeholder="Optional comment..." maxlength="1000"><c:out value="${userRating.comment}"/></textarea>
            </div>
            <button type="submit" class="btn btn-primary btn-sm">Submit Rating</button>
        </form>
    </div>
</div>
</c:if>

<!-- Ratings list -->
<c:if test="${not empty ratings}">
<h5>Reviews</h5>
<div class="list-group mb-4">
    <c:forEach var="r" items="${ratings}">
        <div class="list-group-item">
            <div class="d-flex justify-content-between">
                <strong><c:out value="${r.raterUsername}"/></strong>
                <span>
                    <c:forEach begin="1" end="${r.stars}" var="i">⭐</c:forEach>
                </span>
            </div>
            <c:if test="${not empty r.comment}">
                <p class="mb-0 small"><c:out value="${r.comment}"/></p>
            </c:if>
            <small class="text-muted"><c:out value="${r.createdAt}"/></small>
        </div>
    </c:forEach>
</div>
</c:if>

<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
