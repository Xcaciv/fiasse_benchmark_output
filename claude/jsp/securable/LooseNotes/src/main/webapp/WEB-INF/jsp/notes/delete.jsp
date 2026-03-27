<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Delete Note - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card border-danger shadow-sm">
            <div class="card-header bg-danger text-white">Confirm Deletion</div>
            <div class="card-body">
                <p>Are you sure you want to permanently delete this note?</p>
                <p class="fw-semibold"><c:out value="${note.title}"/></p>
                <p class="text-muted small">
                    This will also remove all attachments, ratings, and share links.
                    This action cannot be undone.
                </p>

                <form method="post"
                      action="${pageContext.request.contextPath}/notes/delete/<c:out value='${note.id}'/>">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-danger">Delete Permanently</button>
                        <a href="${pageContext.request.contextPath}/notes/view/<c:out value='${note.id}'/>"
                           class="btn btn-outline-secondary">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
