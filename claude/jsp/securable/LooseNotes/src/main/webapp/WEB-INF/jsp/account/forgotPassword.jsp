<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Forgot Password - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h2 class="card-title mb-4">Reset Password</h2>

                <c:choose>
                    <c:when test="${emailSent}">
                        <div class="alert alert-success">
                            If an account exists with that email address, you will receive
                            a password reset link shortly.
                        </div>
                        <a href="${pageContext.request.contextPath}/auth/login"
                           class="btn btn-primary">Back to Login</a>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${error != null}">
                            <div class="alert alert-danger"><c:out value="${error}"/></div>
                        </c:if>
                        <p>Enter your email address and we'll send you a reset link.</p>
                        <form method="post"
                              action="${pageContext.request.contextPath}/auth/forgot-password">
                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                            <div class="mb-3">
                                <label for="email" class="form-label">Email Address</label>
                                <input type="email" class="form-control" id="email" name="email"
                                       required maxlength="255" autocomplete="email">
                            </div>
                            <button type="submit" class="btn btn-primary w-100">Send Reset Link</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
