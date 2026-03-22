<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Set New Password – Loose Notes"/>
<%@ include file="/jsp/includes/header.jsp" %>

<div class="row justify-content-center">
  <div class="col-md-5">
    <div class="card shadow">
      <div class="card-body p-4">
        <h2 class="card-title mb-4">Set New Password</h2>

        <c:if test="${not empty error}">
          <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/password-reset"
              autocomplete="off">
          <%-- Exempt from CSRF filter (unauthenticated) --%>
          <input type="hidden" name="action"     value="reset">
          <input type="hidden" name="resetToken" value="<c:out value='${resetToken}'/>">
          <div class="mb-3">
            <label for="password" class="form-label">New Password</label>
            <input type="password" id="password" name="password" class="form-control"
                   required minlength="8" autocomplete="new-password">
            <div class="form-text">Min 8 characters, 1 uppercase, 1 digit.</div>
          </div>
          <div class="mb-3">
            <label for="confirmPassword" class="form-label">Confirm Password</label>
            <input type="password" id="confirmPassword" name="confirmPassword"
                   class="form-control" required autocomplete="new-password">
          </div>
          <button type="submit" class="btn btn-primary w-100">Set Password</button>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/jsp/includes/footer.jsp" %>
