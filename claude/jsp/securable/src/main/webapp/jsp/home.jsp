<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="My Notes – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
  <h2>My Notes</h2>
  <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">+ New Note</a>
</div>

<c:if test="${empty notes}">
  <div class="alert alert-info">
    You have no notes yet. <a href="${pageContext.request.contextPath}/notes/create">Create your first note</a>.
  </div>
</c:if>

<div class="row row-cols-1 row-cols-md-2 g-4">
  <c:forEach var="note" items="${notes}">
    <div class="col">
      <div class="card h-100 shadow-sm">
        <div class="card-body">
          <h5 class="card-title">
            <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}">
              <c:out value="${note.title}"/>
            </a>
            <c:if test="${note.public}">
              <span class="badge bg-success ms-1" style="font-size:.65em">Public</span>
            </c:if>
            <c:if test="${!note.public}">
              <span class="badge bg-secondary ms-1" style="font-size:.65em">Private</span>
            </c:if>
          </h5>
          <p class="card-text text-muted small"><c:out value="${note.excerpt}"/></p>
        </div>
        <div class="card-footer text-muted small d-flex justify-content-between">
          <span>Updated: <c:out value="${note.updatedAt}"/></span>
          <span>
            <c:if test="${note.ratingCount > 0}">
              &#9733; <c:out value="${fn:substringBefore(note.averageRating.toString(),'.')}"/>.<c:out value="${fn:substring(note.averageRating.toString(),fn:indexOf(note.averageRating.toString(),'.')+1,fn:indexOf(note.averageRating.toString(),'.')+2)}"/>
              (<c:out value="${note.ratingCount}"/>)
            </c:if>
          </span>
        </div>
      </div>
    </div>
  </c:forEach>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
