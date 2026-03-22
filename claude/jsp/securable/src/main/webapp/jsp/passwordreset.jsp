<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Password Reset – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="row justify-content-center">
  <div class="col-md-5">
    <div class="card shadow">
      <div class="card-body p-4">
        <h2 class="card-title mb-4">Reset Password</h2>

        <c:if test="${not empty info}">
          <div class="alert alert-success"><c:out value="${info}"/></div>
        </c:if>
        <c:if test="${not empty error}">
          <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <p class="text-muted">Enter your email address and we'll send you a reset link.</p>

        <form method="post" action="${pageContext.request.contextPath}/password-reset">
          <%-- Exempt from CSRF filter (unauthenticated) --%>
          <div class="mb-3">
            <label for="email" class="form-label">Email address</label>
            <input type="email" id="email" name="email" class="form-control"
                   required maxlength="100" autocomplete="email">
          </div>
          <button type="submit" class="btn btn-primary w-100">Send Reset Link</button>
        </form>

        <hr>
        <p class="text-center"><a href="${pageContext.request.contextPath}/login">Back to Login</a></p>
      </div>
    </div>
  </div>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
