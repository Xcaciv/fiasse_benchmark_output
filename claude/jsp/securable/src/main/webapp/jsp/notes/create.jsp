<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="New Note – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="row justify-content-center">
  <div class="col-md-8">
    <h2 class="mb-4">Create Note</h2>

    <c:if test="${not empty error}">
      <div class="alert alert-danger"><c:out value="${error}"/></div>
    </c:if>

    <form method="post"
          action="${pageContext.request.contextPath}/notes/create"
          enctype="multipart/form-data">
      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">

      <div class="mb-3">
        <label for="title" class="form-label">Title <span class="text-danger">*</span></label>
        <input type="text" id="title" name="title" class="form-control"
               required maxlength="255">
      </div>
      <div class="mb-3">
        <label for="content" class="form-label">Content <span class="text-danger">*</span></label>
        <textarea id="content" name="content" class="form-control" rows="10" required></textarea>
      </div>
      <div class="mb-3 form-check">
        <input type="checkbox" id="isPublic" name="isPublic" value="true" class="form-check-input">
        <label for="isPublic" class="form-check-label">Make public</label>
      </div>
      <div class="mb-3">
        <label for="attachment" class="form-label">Attachment (optional)</label>
        <input type="file" id="attachment" name="attachment" class="form-control"
               accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg">
        <div class="form-text">Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Max 10 MB.</div>
      </div>
      <div class="d-flex gap-2">
        <button type="submit" class="btn btn-primary">Create Note</button>
        <a href="${pageContext.request.contextPath}/home" class="btn btn-outline-secondary">Cancel</a>
      </div>
    </form>
  </div>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
