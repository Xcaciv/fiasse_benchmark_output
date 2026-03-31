<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h2 class="card-title mb-4 text-center">Reset Password</h2>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger">
                        <c:out value="${error}"/>
                    </div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/auth/reset-password" novalidate>
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="token" value="<c:out value="${token}"/>">

                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <input type="password" class="form-control" id="newPassword" name="newPassword"
                               required autocomplete="new-password" autofocus>
                    </div>

                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                               required autocomplete="new-password">
                    </div>

                    <div class="d-grid mb-3">
                        <button type="submit" class="btn btn-primary">Reset Password</button>
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
