<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Reassign Note" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="d-flex align-items-center mb-4">
            <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary btn-sm me-3">
                <i class="bi bi-arrow-left"></i> Back
            </a>
            <h2 class="mb-0"><i class="bi bi-arrow-left-right text-primary"></i> Reassign Note</h2>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> ${error}
            </div>
        </c:if>

        <div class="card shadow-sm">
            <div class="card-header">
                <strong>Note:</strong> ${note.title}
            </div>
            <div class="card-body">
                <p class="text-muted">Current owner: <strong>${note.ownerUsername}</strong></p>
                <form method="post" action="${pageContext.request.contextPath}/admin/reassign">
                    <input type="hidden" name="noteId" value="${note.id}">
                    <div class="mb-3">
                        <label for="newUserId" class="form-label fw-semibold">Assign to User</label>
                        <select class="form-select" id="newUserId" name="newUserId" required>
                            <option value="">-- Select a user --</option>
                            <c:forEach var="user" items="${users}">
                                <c:if test="${user.id != note.userId}">
                                    <option value="${user.id}">${user.username} (${user.email})</option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary"
                                onclick="return confirm('Are you sure you want to reassign this note?')">
                            <i class="bi bi-arrow-left-right"></i> Reassign
                        </button>
                        <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
