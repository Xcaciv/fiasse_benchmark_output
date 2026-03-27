<%--
  Generic error page.
  SSEM: Confidentiality - no stack traces, internal paths, or error details exposed.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error - Loose Notes</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
          integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"
          crossorigin="anonymous">
</head>
<body>
<div class="container mt-5">
    <div class="row justify-content-center">
        <div class="col-md-6 text-center">
            <h1 class="display-4 text-danger">
                <c:choose>
                    <c:when test="${pageContext.errorData.statusCode == 403}">403</c:when>
                    <c:when test="${pageContext.errorData.statusCode == 404}">404</c:when>
                    <c:otherwise>Error</c:otherwise>
                </c:choose>
            </h1>
            <h2 class="h4 mb-3">
                <c:choose>
                    <c:when test="${pageContext.errorData.statusCode == 403}">Access Denied</c:when>
                    <c:when test="${pageContext.errorData.statusCode == 404}">Page Not Found</c:when>
                    <c:otherwise>Something went wrong</c:otherwise>
                </c:choose>
            </h2>
            <p class="text-muted mb-4">
                <c:choose>
                    <c:when test="${pageContext.errorData.statusCode == 403}">
                        You don't have permission to access this resource.
                    </c:when>
                    <c:when test="${pageContext.errorData.statusCode == 404}">
                        The page you're looking for doesn't exist or has been moved.
                    </c:when>
                    <c:otherwise>
                        An unexpected error occurred. Please try again or contact support.
                    </c:otherwise>
                </c:choose>
            </p>
            <a href="${pageContext.request.contextPath}/notes" class="btn btn-primary">
                Go to My Notes
            </a>
        </div>
    </div>
</div>
</body>
</html>
