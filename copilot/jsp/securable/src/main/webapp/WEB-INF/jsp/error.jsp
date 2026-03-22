<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Error - Loose Notes</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container mt-5 text-center">
    <div class="display-1 text-muted">
        ${requestScope['javax.servlet.error.status_code'] != null
            ? requestScope['javax.servlet.error.status_code'] : '500'}
    </div>
    <h2 class="mb-3">Something went wrong</h2>
    <%-- Error message shown without stack traces (Confidentiality: no internals leaked) --%>
    <c:choose>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 403}">
            <p class="text-muted">You don't have permission to access this resource.</p>
        </c:when>
        <c:when test="${requestScope['javax.servlet.error.status_code'] == 404}">
            <p class="text-muted">The requested page could not be found.</p>
        </c:when>
        <c:otherwise>
            <p class="text-muted">An unexpected error occurred. Please try again later.</p>
        </c:otherwise>
    </c:choose>
    <a href="${pageContext.request.contextPath}/notes" class="btn btn-primary">Go Home</a>
</div>
</body>
</html>
