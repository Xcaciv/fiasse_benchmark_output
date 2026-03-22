<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Error - Loose Notes" />
<%@ include file="includes/header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6 text-center py-5">
        <c:choose>
            <c:when test="${pageContext.errorData.statusCode == 404}">
                <i class="bi bi-search display-1 text-muted"></i>
                <h2 class="mt-3">Page Not Found</h2>
                <p class="text-muted">The page you're looking for doesn't exist.</p>
            </c:when>
            <c:when test="${pageContext.errorData.statusCode == 403}">
                <i class="bi bi-lock display-1 text-muted"></i>
                <h2 class="mt-3">Access Denied</h2>
                <p class="text-muted">You don't have permission to access this page.</p>
            </c:when>
            <c:otherwise>
                <i class="bi bi-exclamation-triangle display-1 text-danger"></i>
                <h2 class="mt-3">
                    <c:choose>
                        <c:when test="${not empty errorMessage}">${errorMessage}</c:when>
                        <c:otherwise>Something went wrong</c:otherwise>
                    </c:choose>
                </h2>
                <p class="text-muted">
                    <c:choose>
                        <c:when test="${not empty pageContext.errorData.throwable}">
                            ${pageContext.errorData.throwable.message}
                        </c:when>
                        <c:otherwise>An unexpected error occurred. Please try again.</c:otherwise>
                    </c:choose>
                </p>
            </c:otherwise>
        </c:choose>

        <div class="mt-4 d-flex justify-content-center gap-3">
            <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary">
                <i class="bi bi-house me-2"></i>Go to Dashboard
            </a>
            <a href="javascript:history.back()" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-2"></i>Go Back
            </a>
        </div>
    </div>
</div>

<%@ include file="includes/footer.jsp" %>
