<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Profile – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="row">
  <div class="col-md-6">
    <h2 class="mb-4">My Profile</h2>

    <c:if test="${not empty success}">
      <div class="alert alert-success"><c:out value="${success}"/></div>
    </c:if>
    <c:if test="${not empty error}">
      <div class="alert alert-danger"><c:out value="${error}"/></div>
    </c:if>

    <%-- Profile info form --%>
    <div class="card mb-4">
      <div class="card-header">Account Details</div>
      <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/profile">
          <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
          <input type="hidden" name="action"    value="updateProfile">
          <div class="mb-3">
            <label for="username" class="form-label">Username</label>
            <input type="text" id="username" name="username" class="form-control"
                   required minlength="3" maxlength="50"
                   value="<c:out value='${user.username}'/>">
          </div>
          <div class="mb-3">
            <label for="email" class="form-label">Email</label>
            <input type="email" id="email" name="email" class="form-control"
                   required maxlength="100"
                   value="<c:out value='${user.email}'/>">
          </div>
          <button type="submit" class="btn btn-primary">Update Profile</button>
        </form>
      </div>
    </div>

    <%-- Change password form --%>
    <div class="card">
      <div class="card-header">Change Password</div>
      <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/profile"
              autocomplete="off">
          <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
          <input type="hidden" name="action"    value="updatePassword">
          <div class="mb-3">
            <label for="currentPassword" class="form-label">Current Password</label>
            <input type="password" id="currentPassword" name="currentPassword"
                   class="form-control" required autocomplete="current-password">
          </div>
          <div class="mb-3">
            <label for="newPassword" class="form-label">New Password</label>
            <input type="password" id="newPassword" name="newPassword"
                   class="form-control" required minlength="8"
                   autocomplete="new-password">
          </div>
          <div class="mb-3">
            <label for="confirmPassword" class="form-label">Confirm New Password</label>
            <input type="password" id="confirmPassword" name="confirmPassword"
                   class="form-control" required autocomplete="new-password">
          </div>
          <button type="submit" class="btn btn-warning">Change Password</button>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
