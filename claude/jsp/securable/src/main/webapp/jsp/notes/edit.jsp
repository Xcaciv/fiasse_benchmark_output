<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Edit Note – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="row justify-content-center">
  <div class="col-md-8">
    <h2 class="mb-4">Edit Note</h2>

    <c:if test="${not empty error}">
      <div class="alert alert-danger"><c:out value="${error}"/></div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/notes/edit">
      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
      <input type="hidden" name="noteId"    value="${note.id}">

      <div class="mb-3">
        <label for="title" class="form-label">Title <span class="text-danger">*</span></label>
        <input type="text" id="title" name="title" class="form-control"
               required maxlength="255" value="<c:out value='${note.title}'/>">
      </div>
      <div class="mb-3">
        <label for="content" class="form-label">Content <span class="text-danger">*</span></label>
        <textarea id="content" name="content" class="form-control" rows="12" required><c:out value="${note.content}"/></textarea>
      </div>
      <div class="mb-3 form-check">
        <input type="checkbox" id="isPublic" name="isPublic" value="true"
               class="form-check-input" ${note.public ? 'checked' : ''}>
        <label for="isPublic" class="form-check-label">Make public</label>
      </div>
      <div class="d-flex gap-2">
        <button type="submit" class="btn btn-primary">Save Changes</button>
        <a href="${pageContext.request.contextPath}/notes/view?id=${note.id}"
           class="btn btn-outline-secondary">Cancel</a>
      </div>
    </form>
  </div>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
