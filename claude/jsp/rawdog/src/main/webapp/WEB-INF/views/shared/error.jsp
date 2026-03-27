<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Error" scope="request"/>
<%@ include file="header.jsp" %>

<div class="row justify-content-center">
    <div class="col-md-6 text-center">
        <div class="py-5">
            <i class="bi bi-exclamation-triangle-fill text-warning" style="font-size: 4rem;"></i>
            <h1 class="display-4 mt-3">
                <c:choose>
                    <c:when test="${pageContext.errorData.statusCode == 404}">404</c:when>
                    <c:when test="${pageContext.errorData.statusCode == 403}">403</c:when>
                    <c:otherwise>Error</c:otherwise>
                </c:choose>
            </h1>
            <p class="lead text-muted">
                <c:choose>
                    <c:when test="${not empty errorMessage}">${errorMessage}</c:when>
                    <c:when test="${pageContext.errorData.statusCode == 404}">The page you're looking for doesn't exist.</c:when>
                    <c:when test="${pageContext.errorData.statusCode == 403}">You don't have permission to access this resource.</c:when>
                    <c:otherwise>Something went wrong. Please try again.</c:otherwise>
                </c:choose>
            </p>
            <a href="${pageContext.request.contextPath}/" class="btn btn-primary mt-3">
                <i class="bi bi-house"></i> Go Home
            </a>
        </div>
    </div>
</div>

<%@ include file="footer.jsp" %>
