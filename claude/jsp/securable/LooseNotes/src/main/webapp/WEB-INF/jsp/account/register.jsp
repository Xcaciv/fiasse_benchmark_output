<%--
  Registration page.
  SSEM: Integrity - client-side validation complements server-side.
  SSEM: Integrity - CSRF token on form.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Register - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h2 class="card-title mb-4">Create Account</h2>

                <c:if test="${error != null}">
                    <div class="alert alert-danger"><c:out value="${error}"/></div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/auth/register"
                      novalidate>
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">

                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               required minlength="3" maxlength="50"
                               pattern="[a-zA-Z0-9_\-]{3,50}"
                               title="3-50 alphanumeric characters, underscores, or hyphens">
                        <div class="form-text">3-50 characters: letters, numbers, _ or -</div>
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">Email Address</label>
                        <input type="email" class="form-control" id="email" name="email"
                               required maxlength="255">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password"
                               required minlength="8" maxlength="128"
                               autocomplete="new-password">
                        <div class="form-text">
                            Min 8 chars with uppercase, lowercase, digit, and special character.
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm Password</label>
                        <input type="password" class="form-control" id="confirmPassword"
                               name="confirmPassword" required maxlength="128"
                               autocomplete="new-password">
                    </div>
                    <button type="submit" class="btn btn-primary w-100">Create Account</button>
                </form>

                <hr>
                <p class="text-center">
                    Already have an account?
                    <a href="${pageContext.request.contextPath}/auth/login">Sign in</a>
                </p>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
