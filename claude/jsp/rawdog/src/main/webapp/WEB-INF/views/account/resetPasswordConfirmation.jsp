<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Password Reset" scope="request"/>
<%@ include file="../shared/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-5">
        <div class="card shadow-sm">
            <div class="card-body p-4 text-center">
                <i class="bi bi-check-circle-fill text-success" style="font-size: 3rem;"></i>
                <h3 class="mt-3">Password Reset Successful</h3>
                <p class="text-muted">Your password has been reset. You can now sign in with your new password.</p>
                <a href="${pageContext.request.contextPath}/login" class="btn btn-primary">
                    <i class="bi bi-box-arrow-in-right"></i> Sign In
                </a>
            </div>
        </div>
    </div>
</div>

<%@ include file="../shared/footer.jsp" %>
