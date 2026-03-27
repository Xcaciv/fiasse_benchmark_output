<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Edit Note - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-9">
        <h1 class="h3 mb-4">Edit Note</h1>

        <c:if test="${error != null}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <form method="post"
              action="${pageContext.request.contextPath}/notes/edit/<c:out value='${note.id}'/>">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

            <div class="mb-3">
                <label for="title" class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="title" name="title"
                       required maxlength="255"
                       value="<c:out value='${note.title}'/>">
            </div>
            <div class="mb-3">
                <label for="content" class="form-label fw-semibold">Content <span class="text-danger">*</span></label>
                <textarea class="form-control" id="content" name="content"
                          rows="15" required maxlength="50000"><c:out value="${note.content}"/></textarea>
            </div>
            <div class="mb-3 form-check">
                <input type="checkbox" class="form-check-input" id="isPublic"
                       name="isPublic" value="true"
                       ${note.public ? 'checked' : ''}>
                <label class="form-check-label" for="isPublic">
                    Make this note public
                </label>
            </div>
            <div class="d-flex gap-2">
                <button type="submit" class="btn btn-primary">Save Changes</button>
                <a href="${pageContext.request.contextPath}/notes/view/<c:out value='${note.id}'/>"
                   class="btn btn-outline-secondary">Cancel</a>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
