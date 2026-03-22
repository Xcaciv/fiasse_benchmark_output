<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Loose Notes</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body class="bg-light">
<div class="container mt-5">
    <div class="row justify-content-center">
        <div class="col-md-4">
            <div class="card shadow-sm">
                <div class="card-body p-4">
                    <h2 class="card-title text-center mb-4">📝 Loose Notes</h2>
                    <c:if test="${param.registered == '1'}">
                        <div class="alert alert-success">Account created! Please log in.</div>
                    </c:if>
                    <c:if test="${not empty error}">
                        <div class="alert alert-danger"><c:out value="${error}"/></div>
                    </c:if>
                    <form method="post" action="${pageContext.request.contextPath}/login">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
                        <div class="mb-3">
                            <label for="username" class="form-label">Username</label>
                            <input type="text" id="username" name="username"
                                   class="form-control" required autocomplete="username">
                        </div>
                        <div class="mb-3">
                            <label for="password" class="form-label">Password</label>
                            <input type="password" id="password" name="password"
                                   class="form-control" required autocomplete="current-password">
                        </div>
                        <button type="submit" class="btn btn-primary w-100">Log In</button>
                    </form>
                    <div class="text-center mt-3">
                        <a href="${pageContext.request.contextPath}/register">Create an account</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
