<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h2 class="card-title mb-4 text-center">Login</h2>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger">
                        <c:out value="${error}"/>
                    </div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/auth/login" novalidate>
                    <input type="hidden" name="_csrf" value="${csrfToken}">

                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               required autocomplete="username" autofocus>
                    </div>

                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password"
                               required autocomplete="current-password">
                    </div>

                    <div class="d-grid mb-3">
                        <button type="submit" class="btn btn-primary">Login</button>
                    </div>
                </form>

                <hr>

                <div class="text-center small">
                    <a href="${pageContext.request.contextPath}/auth/forgot-password">Forgot your password?</a>
                    &nbsp;|&nbsp;
                    <a href="${pageContext.request.contextPath}/auth/register">Create an account</a>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
