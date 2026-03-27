<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Reset Password - Loose Notes"/>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4">
                <h2 class="card-title mb-4">Set New Password</h2>

                <c:if test="${error != null}">
                    <div class="alert alert-danger"><c:out value="${error}"/></div>
                </c:if>

                <form method="post"
                      action="${pageContext.request.contextPath}/auth/forgot-password">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <%-- Pass the token through the form, not the URL, to avoid logging --%>
                    <input type="hidden" name="token" value="<c:out value='${token}'/>">

                    <div class="mb-3">
                        <label for="newPassword" class="form-label">New Password</label>
                        <input type="password" class="form-control" id="newPassword"
                               name="newPassword" required minlength="8" maxlength="128"
                               autocomplete="new-password">
                        <div class="form-text">
                            Min 8 chars with uppercase, lowercase, digit, and special character.
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label">Confirm New Password</label>
                        <input type="password" class="form-control" id="confirmPassword"
                               name="confirmPassword" required maxlength="128"
                               autocomplete="new-password">
                    </div>
                    <button type="submit" class="btn btn-primary w-100">Reset Password</button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
