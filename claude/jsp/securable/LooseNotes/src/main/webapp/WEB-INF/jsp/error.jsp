<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error &mdash; LooseNotes</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
          integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"
          crossorigin="anonymous">
</head>
<body class="bg-light">
<div class="container mt-5">
    <div class="row justify-content-center">
        <div class="col-md-6 text-center">
            <h1 class="display-1 fw-bold text-danger">
                <c:out value="${pageContext.errorData.statusCode}"/>
            </h1>
            <h2 class="mb-3">Something went wrong</h2>
            <p class="text-muted mb-4">
                <c:choose>
                    <c:when test="${pageContext.errorData.statusCode == 404}">
                        The page you are looking for could not be found.
                    </c:when>
                    <c:when test="${pageContext.errorData.statusCode == 403}">
                        You do not have permission to access this resource.
                    </c:when>
                    <c:when test="${pageContext.errorData.statusCode == 500}">
                        An internal server error occurred. Please try again later.
                    </c:when>
                    <c:otherwise>
                        An unexpected error occurred. Please try again later.
                    </c:otherwise>
                </c:choose>
            </p>
            <a href="${pageContext.request.contextPath}/" class="btn btn-primary">Go to Home</a>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL"
        crossorigin="anonymous"></script>
</body>
</html>
