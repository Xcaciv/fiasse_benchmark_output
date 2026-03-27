<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Reset Password" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h3 class="card-title text-center mb-4">
                    <i class="bi bi-lock text-primary"></i> Set New Password
                </h3>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger">
                        <i class="bi bi-exclamation-triangle"></i> ${error}
                    </div>
                </c:if>

                <c:if test="${not empty token}">
                    <form method="post" action="${pageContext.request.contextPath}/reset-password">
                        <input type="hidden" name="token" value="${token}">
                        <div class="mb-3">
                            <label for="password" class="form-label">New Password</label>
                            <input type="password" class="form-control" id="password" name="password"
                                   required minlength="6" autofocus>
                            <div class="form-text">At least 6 characters.</div>
                        </div>
                        <div class="mb-3">
                            <label for="confirmPassword" class="form-label">Confirm New Password</label>
                            <input type="password" class="form-control" id="confirmPassword"
                                   name="confirmPassword" required minlength="6">
                        </div>
                        <div class="d-grid">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-check-circle"></i> Reset Password
                            </button>
                        </div>
                    </form>
                </c:if>

                <hr>
                <p class="text-center mb-0">
                    <a href="${pageContext.request.contextPath}/login">Back to Sign In</a>
                </p>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
