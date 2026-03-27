<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Reassign Note - Admin" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-dark text-white">
                <h4 class="mb-0"><i class="bi bi-arrow-left-right"></i> Reassign Note</h4>
            </div>
            <div class="card-body">
                <div class="mb-3">
                    <label class="form-label text-muted">Note</label>
                    <div class="card bg-light">
                        <div class="card-body p-2">
                            <strong>${note.title}</strong>
                            <br><small class="text-muted">Current owner: ${note.ownerUsername}</small>
                        </div>
                    </div>
                </div>
                <form method="post" action="${pageContext.request.contextPath}/admin/reassign">
                    <input type="hidden" name="noteId" value="${note.id}">
                    <div class="mb-3">
                        <label for="newUserId" class="form-label">New Owner</label>
                        <select class="form-select" id="newUserId" name="newUserId" required>
                            <option value="">-- Select User --</option>
                            <c:forEach var="user" items="${users}">
                                <c:if test="${user.id != note.userId}">
                                    <option value="${user.id}">${user.username} (${user.email})</option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-check-lg"></i> Reassign Note
                        </button>
                        <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary">
                            <i class="bi bi-x-lg"></i> Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
