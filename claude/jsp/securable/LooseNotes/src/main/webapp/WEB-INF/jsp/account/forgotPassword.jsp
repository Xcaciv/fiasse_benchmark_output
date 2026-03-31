<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h2 class="card-title mb-2 text-center">Forgot Password</h2>
                <p class="text-muted text-center mb-4 small">
                    Enter your email address and we will send you a link to reset your password.
                </p>

                <c:if test="${not empty success}">
                    <div class="alert alert-success">
                        <c:out value="${success}"/>
                    </div>
                </c:if>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger">
                        <c:out value="${error}"/>
                    </div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/auth/forgot-password" novalidate>
                    <input type="hidden" name="_csrf" value="${csrfToken}">

                    <div class="mb-3">
                        <label for="email" class="form-label">Email Address</label>
                        <input type="email" class="form-control" id="email" name="email"
                               required autocomplete="email" autofocus>
                    </div>

                    <div class="d-grid mb-3">
                        <button type="submit" class="btn btn-primary">Send Reset Link</button>
                    </div>
                </form>

                <hr>

                <div class="text-center small">
                    <a href="${pageContext.request.contextPath}/auth/login">Back to Login</a>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
