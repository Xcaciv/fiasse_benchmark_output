<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container mt-4" style="max-width: 600px;">
    <h1 class="h3 mb-4">Edit Profile</h1>

    <%-- Success message --%>
    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                 class="bi bi-check-circle-fill me-2" viewBox="0 0 16 16">
                <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
            </svg>
            <c:out value="${successMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <%-- Error message --%>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                 class="bi bi-exclamation-triangle-fill me-2" viewBox="0 0 16 16">
                <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"/>
            </svg>
            <c:out value="${errorMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <%-- Validation errors list --%>
    <c:if test="${not empty errors}">
        <div class="alert alert-danger" role="alert">
            <strong>Please fix the following errors:</strong>
            <ul class="mb-0 mt-1">
                <c:forEach var="err" items="${errors}">
                    <li><c:out value="${err}"/></li>
                </c:forEach>
            </ul>
        </div>
    </c:if>

    <div class="card border-0 shadow-sm">
        <div class="card-body p-4">
            <form method="post"
                  action="${pageContext.request.contextPath}/profile/edit"
                  novalidate>
                <input type="hidden" name="_csrf" value="${csrfToken}">

                <%-- Account Details --%>
                <h6 class="text-uppercase text-muted letter-spacing-1 mb-3 small fw-semibold">
                    Account Details
                </h6>

                <div class="mb-3">
                    <label for="username" class="form-label fw-semibold">
                        Username <span class="text-danger" aria-hidden="true">*</span>
                    </label>
                    <input type="text"
                           id="username"
                           name="username"
                           class="form-control"
                           value="<c:out value="${user.username}"/>"
                           required
                           maxlength="50"
                           autocomplete="username">
                    <div class="invalid-feedback">Username is required.</div>
                </div>

                <div class="mb-4">
                    <label for="email" class="form-label fw-semibold">
                        Email Address <span class="text-danger" aria-hidden="true">*</span>
                    </label>
                    <input type="email"
                           id="email"
                           name="email"
                           class="form-control"
                           value="<c:out value="${user.email}"/>"
                           required
                           maxlength="255"
                           autocomplete="email">
                    <div class="invalid-feedback">A valid email address is required.</div>
                </div>

                <%-- Password Section --%>
                <hr class="my-4">
                <h6 class="text-uppercase text-muted small fw-semibold mb-1">
                    Change Password
                </h6>
                <p class="text-muted small mb-3">Leave all password fields blank to keep your current password.</p>

                <div class="mb-3">
                    <label for="currentPassword" class="form-label fw-semibold">Current Password</label>
                    <input type="password"
                           id="currentPassword"
                           name="currentPassword"
                           class="form-control"
                           autocomplete="current-password">
                </div>

                <div class="mb-3">
                    <label for="newPassword" class="form-label fw-semibold">New Password</label>
                    <input type="password"
                           id="newPassword"
                           name="newPassword"
                           class="form-control"
                           autocomplete="new-password"
                           minlength="8">
                    <div class="form-text">Minimum 8 characters.</div>
                </div>

                <div class="mb-4">
                    <label for="confirmNewPassword" class="form-label fw-semibold">Confirm New Password</label>
                    <input type="password"
                           id="confirmNewPassword"
                           name="confirmNewPassword"
                           class="form-control"
                           autocomplete="new-password">
                </div>

                <%-- Submit --%>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary">
                        Save Changes
                    </button>
                    <a href="${pageContext.request.contextPath}/notes"
                       class="btn btn-outline-secondary">
                        Cancel
                    </a>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
