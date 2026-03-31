<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Loose Notes - Your Personal Note-Taking Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .hero { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 100px 0; }
        .feature-icon { font-size: 3rem; color: #4a6cf7; }
    </style>
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-light bg-white shadow-sm">
        <div class="container">
            <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/">Loose Notes</a>
            <div class="d-flex">
                <c:if test="${empty sessionScope.user}">
                    <a href="${pageContext.request.contextPath}/login" class="btn btn-outline-primary me-2">Login</a>
                    <a href="${pageContext.request.contextPath}/register" class="btn btn-primary">Get Started</a>
                </c:if>
                <c:if test="${not empty sessionScope.user}">
                    <a href="${pageContext.request.contextPath}/notes" class="btn btn-primary">My Notes</a>
                </c:if>
            </div>
        </div>
    </nav>

    <div class="hero text-center">
        <div class="container">
            <h1 class="display-4 fw-bold">Welcome to Loose Notes</h1>
            <p class="lead">Create, share, and discover notes. Your personal knowledge base.</p>
            <c:if test="${empty sessionScope.user}">
                <a href="${pageContext.request.contextPath}/register" class="btn btn-light btn-lg mt-3">Start Free</a>
            </c:if>
        </div>
    </div>

    <div class="container py-5">
        <div class="row text-center">
            <div class="col-md-4 mb-4">
                <i class="bi bi-journal-plus feature-icon"></i>
                <h4 class="mt-3">Create Notes</h4>
                <p class="text-muted">Write and organize your notes with ease. Add attachments and keep everything in one place.</p>
            </div>
            <div class="col-md-4 mb-4">
                <i class="bi bi-share feature-icon"></i>
                <h4 class="mt-3">Share & Collaborate</h4>
                <p class="text-muted">Generate share links to share your notes with anyone. Make them public or private.</p>
            </div>
            <div class="col-md-4 mb-4">
                <i class="bi bi-star feature-icon"></i>
                <h4 class="mt-3">Rate & Discover</h4>
                <p class="text-muted">Rate notes from other users. Discover top-rated content from the community.</p>
            </div>
        </div>
    </div>

    <footer class="bg-light py-4 mt-auto">
        <div class="container text-center text-muted">
            <p class="mb-0">&copy; 2026 Loose Notes. All rights reserved.</p>
        </div>
    </footer>
</body>
</html>
