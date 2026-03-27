<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Create Note - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-9">
        <h1 class="h3 mb-4">Create New Note</h1>

        <c:if test="${error != null}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/notes/create">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

            <div class="mb-3">
                <label for="title" class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="title" name="title"
                       required maxlength="255"
                       value="<c:out value='${title}'/>">
            </div>
            <div class="mb-3">
                <label for="content" class="form-label fw-semibold">Content <span class="text-danger">*</span></label>
                <textarea class="form-control" id="content" name="content"
                          rows="15" required maxlength="50000"><c:out value="${content}"/></textarea>
                <div class="form-text">Max 50,000 characters.</div>
            </div>
            <div class="mb-3 form-check">
                <input type="checkbox" class="form-check-input" id="isPublic"
                       name="isPublic" value="true">
                <label class="form-check-label" for="isPublic">
                    Make this note public (visible in search)
                </label>
            </div>
            <div class="d-flex gap-2">
                <button type="submit" class="btn btn-primary">Create Note</button>
                <a href="${pageContext.request.contextPath}/notes" class="btn btn-outline-secondary">Cancel</a>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
