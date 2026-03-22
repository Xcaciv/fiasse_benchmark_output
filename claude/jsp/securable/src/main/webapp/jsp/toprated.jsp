<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Top Rated – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<h2 class="mb-4">Top Rated Notes</h2>
<p class="text-muted">Public notes with at least 3 ratings, sorted by average rating.</p>

<c:if test="${empty topNotes}">
  <div class="alert alert-info">No notes have enough ratings yet.</div>
</c:if>

<c:forEach var="note" items="${topNotes}" varStatus="s">
  <div class="card mb-3 shadow-sm">
    <div class="card-body">
      <div class="d-flex justify-content-between align-items-start">
        <div>
          <h5 class="card-title">
            <span class="text-muted me-2">#${s.index + 1}</span>
            <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}">
              <c:out value="${note.title}"/>
            </a>
          </h5>
          <p class="card-text text-muted small"><c:out value="${note.excerpt}"/></p>
          <small>By <c:out value="${note.authorUsername}"/></small>
        </div>
        <div class="text-end text-nowrap ms-3">
          <span class="fs-4 text-warning">&#9733;</span>
          <span class="fs-5">
            <c:out value="${fn:substringBefore(note.averageRating.toString(),'.')}"/>.<c:out value="${fn:substring(note.averageRating.toString(),fn:indexOf(note.averageRating.toString(),'.')+1,fn:indexOf(note.averageRating.toString(),'.')+2)}"/>
          </span>
          <div class="text-muted small"><c:out value="${note.ratingCount}"/> ratings</div>
        </div>
      </div>
    </div>
  </div>
</c:forEach>

<%@ include file="/jsp/includes/footer.jsp" %>
