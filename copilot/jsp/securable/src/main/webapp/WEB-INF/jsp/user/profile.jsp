<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
    <jsp:param name="pageTitle" value="My Profile"/>
</jsp:include>

<h1 class="h3 mb-4">👤 My Profile</h1>

<c:if test="${param.updated == '1'}">
    <div class="alert alert-success">Profile updated.</div>
</c:if>
<c:if test="${param.pwchanged == '1'}">
    <div class="alert alert-success">Password changed successfully.</div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<div class="row">
    <div class="col-md-6">
        <div class="card mb-4">
            <div class="card-header">Profile Details</div>
            <div class="card-body">
                <form method="post" action="${pageContext.request.contextPath}/profile/update">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" id="username" name="username" class="form-control"
                               maxlength="50" required
                               value="<c:out value='${user.username}'/>">
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">Email Address</label>
                        <input type="email" id="email" name="email" class="form-control"
                               maxlength="255" required
                               value="<c:out value='${user.email}'/>">
                    </div>
                    <button type="submit" class="btn btn-primary">Save Changes</button>
                </form>
            </div>
        </div>
    </div>

    <div class="col-md-6">
        <div class="card">
            <div class="card-header">Change Password</div>
            <div class="card-body">
                <c:if test="${not empty pwError}">
                    <div class="alert alert-danger"><c:out value="${pwError}"/></div>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/profile/password">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
                    <div class="mb-3">
                        <label for="currentPassword" class="form-label">Current Password</label>
                        <input type="password" id="currentPassword" name="currentPassword"
                               class="form-control" required autocomplete="current-password">
                    </div>
                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <input type="password" id="newPassword" name="newPassword"
                               class="form-control" minlength="8" required autocomplete="new-password">
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm New Password</label>
                        <input type="password" id="confirmPassword" name="confirmPassword"
                               class="form-control" minlength="8" required autocomplete="new-password">
                    </div>
                    <button type="submit" class="btn btn-warning">Change Password</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="mt-3">
    <small class="text-muted">
        Member since: <c:out value="${user.createdAt}"/> &bull;
        Role: <span class="badge ${user.role == 'ADMIN' ? 'bg-danger' : 'bg-secondary'}">
            <c:out value="${user.role}"/>
        </span>
    </small>
</div>

<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
