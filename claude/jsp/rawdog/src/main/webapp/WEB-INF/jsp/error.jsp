<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error - Loose Notes</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<div class="container py-5 text-center">
    <h1 class="display-1 text-danger">
        ${requestScope['jakarta.servlet.error.status_code'] != null ?
          requestScope['jakarta.servlet.error.status_code'] : '500'}
    </h1>
    <h3 class="text-muted">
        <c:choose>
            <c:when test="${requestScope['jakarta.servlet.error.status_code'] == 404}">Page Not Found</c:when>
            <c:when test="${requestScope['jakarta.servlet.error.status_code'] == 403}">Access Denied</c:when>
            <c:otherwise>Server Error</c:otherwise>
        </c:choose>
    </h3>
    <p class="text-muted">
        ${not empty requestScope['jakarta.servlet.error.message'] ?
          requestScope['jakarta.servlet.error.message'] : 'An unexpected error occurred.'}
    </p>
    <a href="${pageContext.request.contextPath}/" class="btn btn-primary mt-3">
        Go to Home
    </a>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
