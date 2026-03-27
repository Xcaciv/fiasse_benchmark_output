<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error - Loose Notes</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
</head>
<body class="bg-light">
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" href="${pageContext.request.contextPath}/">
                <i class="fas fa-sticky-note"></i> Loose Notes
            </a>
        </div>
    </nav>
    
    <main class="container mt-5">
        <div class="row justify-content-center">
            <div class="col-md-6">
                <div class="card border-danger shadow">
                    <div class="card-header bg-danger text-white">
                        <h4 class="mb-0"><i class="fas fa-exclamation-triangle"></i> Error</h4>
                    </div>
                    <div class="card-body text-center">
                        <i class="fas fa-exclamation-circle fa-5x text-danger mb-4"></i>
                        <c:choose>
                            <c:when test="${not empty error}">
                                <h5>${error}</h5>
                            </c:when>
                            <c:otherwise>
                                <h5>An unexpected error occurred</h5>
                            </c:otherwise>
                        </c:choose>
                        <p class="text-muted">Please try again or go back to the home page.</p>
                        <a href="${pageContext.request.contextPath}/" class="btn btn-primary">
                            <i class="fas fa-home"></i> Go to Home
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </main>
    
    <footer class="mt-5 py-4 bg-light">
        <div class="container text-center">
            <p class="text-muted mb-0">&copy; 2024 Loose Notes. All rights reserved.</p>
        </div>
    </footer>
</body>
</html>
