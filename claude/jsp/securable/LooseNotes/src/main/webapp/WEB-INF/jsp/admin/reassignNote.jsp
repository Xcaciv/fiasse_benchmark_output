<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Reassign Note - Admin"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <h1 class="h3 mb-4">Reassign Note Ownership</h1>

        <c:if test="${error != null}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <div class="card mb-3">
            <div class="card-body">
                <strong>Note:</strong> <c:out value="${note.title}"/><br>
                <strong>Current Owner:</strong> <c:out value="${note.ownerUsername}"/>
            </div>
        </div>

        <form method="post" action="${pageContext.request.contextPath}/admin/reassign">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="noteId" value="<c:out value='${note.id}'/>">

            <div class="mb-3">
                <label for="newOwnerId" class="form-label fw-semibold">
                    New Owner User ID
                </label>
                <input type="number" class="form-control" id="newOwnerId" name="newOwnerId"
                       required min="1" placeholder="Enter the target user's ID">
                <div class="form-text">
                    Find the user ID from the
                    <a href="${pageContext.request.contextPath}/admin/users">Users list</a>.
                </div>
            </div>
            <div class="d-flex gap-2">
                <button type="submit" class="btn btn-warning">Reassign Note</button>
                <a href="${pageContext.request.contextPath}/admin/users"
                   class="btn btn-outline-secondary">Cancel</a>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
