<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Login – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="row justify-content-center">
  <div class="col-md-5">
    <div class="card shadow">
      <div class="card-body p-4">
        <h2 class="card-title mb-4">Sign In</h2>

        <c:if test="${param.registered == '1'}">
          <div class="alert alert-success">Account created — please log in.</div>
        </c:if>
        <c:if test="${param.reset == '1'}">
          <div class="alert alert-success">Password reset successfully — please log in.</div>
        </c:if>
        <c:if test="${not empty error}">
          <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/login" autocomplete="off">
          <%-- CSRF not required on login form (user has no session yet);
               CsrfFilter exempts /login POST --%>
          <div class="mb-3">
            <label for="username" class="form-label">Username</label>
            <input type="text" id="username" name="username" class="form-control"
                   required maxlength="50" autocomplete="username">
          </div>
          <div class="mb-3">
            <label for="password" class="form-label">Password</label>
            <input type="password" id="password" name="password" class="form-control"
                   required autocomplete="current-password">
          </div>
          <button type="submit" class="btn btn-primary w-100">Login</button>
        </form>

        <hr>
        <p class="text-center mb-1">
          <a href="${pageContext.request.contextPath}/register">Create an account</a>
        </p>
        <p class="text-center">
          <a href="${pageContext.request.contextPath}/password-reset">Forgot password?</a>
        </p>
      </div>
    </div>
  </div>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
