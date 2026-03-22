<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Register – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="row justify-content-center">
  <div class="col-md-6">
    <div class="card shadow">
      <div class="card-body p-4">
        <h2 class="card-title mb-4">Create Account</h2>

        <c:if test="${not empty error}">
          <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/register" autocomplete="off">
          <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">

          <div class="mb-3">
            <label for="username" class="form-label">Username</label>
            <input type="text" id="username" name="username" class="form-control"
                   required minlength="3" maxlength="50" pattern="[a-zA-Z0-9_\-]+"
                   autocomplete="username">
            <div class="form-text">3–50 alphanumeric, underscore, or hyphen characters.</div>
          </div>
          <div class="mb-3">
            <label for="email" class="form-label">Email address</label>
            <input type="email" id="email" name="email" class="form-control"
                   required maxlength="100" autocomplete="email">
          </div>
          <div class="mb-3">
            <label for="password" class="form-label">Password</label>
            <input type="password" id="password" name="password" class="form-control"
                   required minlength="8" autocomplete="new-password">
            <div class="form-text">Min 8 characters, at least 1 uppercase letter and 1 digit.</div>
          </div>
          <div class="mb-3">
            <label for="confirmPassword" class="form-label">Confirm Password</label>
            <input type="password" id="confirmPassword" name="confirmPassword"
                   class="form-control" required autocomplete="new-password">
          </div>
          <button type="submit" class="btn btn-success w-100">Register</button>
        </form>

        <hr>
        <p class="text-center">
          Already have an account? <a href="${pageContext.request.contextPath}/login">Login</a>
        </p>
      </div>
    </div>
  </div>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
