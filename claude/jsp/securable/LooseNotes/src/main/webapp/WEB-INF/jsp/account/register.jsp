<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h2 class="card-title mb-4 text-center">Create Account</h2>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger">
                        <c:out value="${error}"/>
                    </div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/auth/register" novalidate>
                    <input type="hidden" name="_csrf" value="${csrfToken}">

                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               value="<c:out value="${username}"/>"
                               required autocomplete="username" autofocus>
                    </div>

                    <div class="mb-3">
                        <label for="email" class="form-label">Email Address</label>
                        <input type="email" class="form-control" id="email" name="email"
                               value="<c:out value="${email}"/>"
                               required autocomplete="email">
                    </div>

                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password"
                               required autocomplete="new-password">
                    </div>

                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                               required autocomplete="new-password">
                    </div>

                    <div class="d-grid mb-3">
                        <button type="submit" class="btn btn-primary">Register</button>
                    </div>
                </form>

                <hr>

                <div class="text-center small">
                    Already have an account?
                    <a href="${pageContext.request.contextPath}/auth/login">Login</a>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
