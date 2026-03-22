<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="${note.title} – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<%-- Error / info alerts --%>
<c:if test="${param.error == 'cannotRateOwn'}">
  <div class="alert alert-warning">You cannot rate your own note.</div>
</c:if>

<div class="d-flex justify-content-between align-items-start mb-2">
  <div>
    <h2><c:out value="${note.title}"/></h2>
    <small class="text-muted">
      By <strong><c:out value="${note.authorUsername}"/></strong>
      &middot; <c:out value="${note.createdAt}"/>
      <c:if test="${note.public}">
        <span class="badge bg-success ms-1">Public</span>
      </c:if>
      <c:if test="${!note.public}">
        <span class="badge bg-secondary ms-1">Private</span>
      </c:if>
    </small>
  </div>

  <c:if test="${isOwner}">
    <div class="d-flex gap-2">
      <a href="${pageContext.request.contextPath}/notes/edit?id=${note.id}"
         class="btn btn-sm btn-outline-primary">Edit</a>
      <button class="btn btn-sm btn-outline-danger"
              data-bs-toggle="modal" data-bs-target="#deleteModal">Delete</button>
    </div>
  </c:if>
</div>

<hr>

<div class="note-content mb-4">
  <%-- Output encoded to prevent XSS; newlines preserved --%>
  <pre class="bg-light p-3 rounded" style="white-space:pre-wrap;word-break:break-word;"><c:out value="${note.content}"/></pre>
</div>

<%-- ── Attachments ──────────────────────────────────────────────────────── --%>
<c:if test="${not empty attachments}">
  <h5>Attachments</h5>
  <ul class="list-group mb-4">
    <c:forEach var="a" items="${attachments}">
      <li class="list-group-item d-flex justify-content-between align-items-center">
        <a href="${pageContext.request.contextPath}/attachments/download?id=${a.id}">
          <c:out value="${a.originalFilename}"/>
        </a>
        <span class="text-muted small">
          <c:out value="${a.fileSize}"/> bytes
          <c:if test="${isOwner}">
            <form method="post"
                  action="${pageContext.request.contextPath}/attachments/delete"
                  class="d-inline ms-2">
              <input type="hidden" name="csrfToken"     value="<c:out value='${csrfToken}'/>">
              <input type="hidden" name="attachmentId"  value="${a.id}">
              <button type="submit" class="btn btn-sm btn-link text-danger p-0">Remove</button>
            </form>
          </c:if>
        </span>
      </li>
    </c:forEach>
  </ul>
</c:if>

<%-- Add attachment (owner only) --%>
<c:if test="${isOwner}">
  <div class="card mb-4">
    <div class="card-body">
      <h6>Add Attachment</h6>
      <form method="post"
            action="${pageContext.request.contextPath}/attachments/upload"
            enctype="multipart/form-data">
        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
        <input type="hidden" name="noteId"    value="${note.id}">
        <div class="input-group">
          <input type="file" name="attachment" class="form-control"
                 accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg" required>
          <button type="submit" class="btn btn-secondary">Upload</button>
        </div>
        <div class="form-text">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Max 10 MB.</div>
      </form>
    </div>
  </div>
</c:if>

<%-- ── Share Link ───────────────────────────────────────────────────────── --%>
<c:if test="${isOwner}">
  <div class="card mb-4">
    <div class="card-body">
      <h6>Share Link</h6>
      <c:choose>
        <c:when test="${not empty shareLink}">
          <div class="input-group mb-2">
            <input type="text" class="form-control" readonly
                   value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/share/<c:out value='${shareLink.token}'/>">
            <button class="btn btn-outline-secondary" type="button"
                    onclick="navigator.clipboard.writeText(this.previousElementSibling.value)">Copy</button>
          </div>
          <form method="post" action="${pageContext.request.contextPath}/share/generate"
                class="d-inline">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="noteId"    value="${note.id}">
            <button type="submit" class="btn btn-sm btn-warning">Regenerate</button>
          </form>
          <form method="post" action="${pageContext.request.contextPath}/share/revoke"
                class="d-inline ms-2">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="noteId"    value="${note.id}">
            <button type="submit" class="btn btn-sm btn-outline-danger">Revoke</button>
          </form>
        </c:when>
        <c:otherwise>
          <p class="text-muted small mb-2">No share link created yet.</p>
          <form method="post" action="${pageContext.request.contextPath}/share/generate">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="noteId"    value="${note.id}">
            <button type="submit" class="btn btn-sm btn-primary">Generate Share Link</button>
          </form>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</c:if>

<%-- ── Ratings ──────────────────────────────────────────────────────────── --%>
<h5>Ratings
  <c:if test="${note.ratingCount > 0}">
    <span class="badge bg-warning text-dark">
      &#9733; ${fn:substringBefore(note.averageRating.toString(),'.')}.${fn:substring(note.averageRating.toString(),fn:indexOf(note.averageRating.toString(),'.')+1,fn:indexOf(note.averageRating.toString(),'.')+2)}
      / 5 (<c:out value="${note.ratingCount}"/> ratings)
    </span>
  </c:if>
</h5>

<%-- Rate form (authenticated non-owners) --%>
<c:if test="${not empty sessionScope.userId && !isOwner}">
  <div class="card mb-3">
    <div class="card-body">
      <h6>${userRating != null ? 'Update Your Rating' : 'Rate This Note'}</h6>
      <form method="post" action="${pageContext.request.contextPath}/ratings/submit">
        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
        <input type="hidden" name="noteId"    value="${note.id}">
        <div class="mb-2">
          <label class="form-label">Stars</label>
          <select name="rating" class="form-select form-select-sm w-auto d-inline-block ms-2" required>
            <c:forEach begin="1" end="5" var="i">
              <option value="${i}" ${userRating != null && userRating.rating == i ? 'selected' : ''}>
                <c:out value="${i}"/> &#9733;
              </option>
            </c:forEach>
          </select>
        </div>
        <div class="mb-2">
          <textarea name="comment" class="form-control" rows="2"
                    maxlength="1000" placeholder="Optional comment…"><c:if test="${userRating != null}"><c:out value="${userRating.comment}"/></c:if></textarea>
        </div>
        <button type="submit" class="btn btn-sm btn-primary">Submit Rating</button>
      </form>
    </div>
  </div>
</c:if>

<c:if test="${empty ratings}">
  <p class="text-muted">No ratings yet.</p>
</c:if>

<c:forEach var="r" items="${ratings}">
  <div class="card mb-2">
    <div class="card-body py-2">
      <div class="d-flex justify-content-between">
        <strong><c:out value="${r.raterUsername}"/></strong>
        <span>
          <c:forEach begin="1" end="5" var="s">
            <c:choose>
              <c:when test="${s <= r.rating}">&#9733;</c:when>
              <c:otherwise>&#9734;</c:otherwise>
            </c:choose>
          </c:forEach>
        </span>
      </div>
      <c:if test="${not empty r.comment}">
        <p class="mb-0 mt-1 small"><c:out value="${r.comment}"/></p>
      </c:if>
      <small class="text-muted"><c:out value="${r.createdAt}"/></small>
    </div>
  </div>
</c:forEach>

<%-- ── Delete Modal ─────────────────────────────────────────────────────── --%>
<c:if test="${isOwner}">
  <div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="deleteModalLabel">Confirm Delete</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          Are you sure you want to permanently delete
          "<strong><c:out value="${note.title}"/></strong>"?
          This action cannot be undone.
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
          <form method="post" action="${pageContext.request.contextPath}/notes/delete">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="noteId"    value="${note.id}">
            <button type="submit" class="btn btn-danger">Delete</button>
          </form>
        </div>
      </div>
    </div>
  </div>
</c:if>

<%@ include file="/jsp/includes/footer.jsp" %>
