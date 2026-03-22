<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Search – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<h2 class="mb-4">Search Notes</h2>

<form method="get" action="${pageContext.request.contextPath}/search" class="mb-4">
  <div class="input-group">
    <input type="text" name="q" class="form-control" placeholder="Search by title or content…"
           value="<c:out value='${keyword}'/>" maxlength="200">
    <button type="submit" class="btn btn-primary">Search</button>
  </div>
</form>

<c:if test="${not empty error}">
  <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<c:if test="${not empty keyword}">
  <p class="text-muted">
    Found <strong><c:out value="${fn:length(results)}"/></strong> result(s) for
    "<c:out value="${keyword}"/>".
  </p>
</c:if>

<c:if test="${empty results && not empty keyword}">
  <div class="alert alert-info">No notes match your search.</div>
</c:if>

<div class="list-group">
  <c:forEach var="note" items="${results}">
    <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"
       class="list-group-item list-group-item-action">
      <div class="d-flex w-100 justify-content-between">
        <h6 class="mb-1">
          <c:out value="${note.title}"/>
          <c:if test="${note.public}">
            <span class="badge bg-success ms-1" style="font-size:.7em">Public</span>
          </c:if>
        </h6>
        <small class="text-muted"><c:out value="${note.createdAt}"/></small>
      </div>
      <p class="mb-1 text-muted small"><c:out value="${note.excerpt}"/></p>
      <small>By <c:out value="${note.authorUsername}"/></small>
    </a>
  </c:forEach>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
