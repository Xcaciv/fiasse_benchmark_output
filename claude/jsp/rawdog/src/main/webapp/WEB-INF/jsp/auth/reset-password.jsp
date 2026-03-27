<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Reset Password - Loose Notes" scope="request"/>
<%@ include file="../layout/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow">
            <div class="card-header bg-dark text-white">
                <h4 class="mb-0"><i class="bi bi-shield-lock"></i> Reset Password</h4>
            </div>
            <div class="card-body">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i> ${error}</div>
                </c:if>
                <c:choose>
                    <c:when test="${not empty token}">
                        <form method="post" action="${pageContext.request.contextPath}/reset-password">
                            <input type="hidden" name="token" value="${token}">
                            <div class="mb-3">
                                <label for="password" class="form-label">New Password</label>
                                <input type="password" class="form-control" id="password" name="password"
                                       minlength="6" required autofocus>
                            </div>
                            <div class="mb-3">
                                <label for="confirmPassword" class="form-label">Confirm New Password</label>
                                <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" required>
                            </div>
                            <div class="d-grid">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-check-lg"></i> Reset Password
                                </button>
                            </div>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${empty error}">
                            <div class="alert alert-warning">No valid reset token provided.</div>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="card-footer text-center text-muted">
                <a href="${pageContext.request.contextPath}/login">Back to Login</a>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
