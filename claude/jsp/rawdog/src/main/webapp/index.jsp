<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
    <c:when test="${not empty sessionScope.currentUser}">
        <jsp:forward page="/notes"/>
    </c:when>
    <c:otherwise>
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Loose Notes - Note Taking Platform</title>
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
        </head>
        <body>
        <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
            <div class="container">
                <a class="navbar-brand" href="${pageContext.request.contextPath}/">
                    <i class="bi bi-journal-text"></i> Loose Notes
                </a>
                <div class="navbar-nav ms-auto">
                    <a class="nav-link" href="${pageContext.request.contextPath}/login">Login</a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/register">Register</a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/top-rated">Top Rated</a>
                </div>
            </div>
        </nav>
        <div class="container py-5">
            <div class="row justify-content-center text-center">
                <div class="col-lg-8">
                    <i class="bi bi-journal-text display-1 text-primary"></i>
                    <h1 class="mt-3 mb-3">Welcome to Loose Notes</h1>
                    <p class="lead text-muted mb-4">
                        Your personal note-taking platform. Create, organize, share and rate notes.
                    </p>
                    <div class="d-flex justify-content-center gap-3">
                        <a href="${pageContext.request.contextPath}/login" class="btn btn-primary btn-lg">
                            <i class="bi bi-box-arrow-in-right"></i> Login
                        </a>
                        <a href="${pageContext.request.contextPath}/register" class="btn btn-outline-primary btn-lg">
                            <i class="bi bi-person-plus"></i> Register
                        </a>
                    </div>
                    <div class="row mt-5 g-4">
                        <div class="col-md-4">
                            <div class="card h-100">
                                <div class="card-body text-center">
                                    <i class="bi bi-pencil-square display-4 text-primary"></i>
                                    <h5 class="mt-2">Create Notes</h5>
                                    <p class="text-muted">Write and organize your notes with file attachments.</p>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="card h-100">
                                <div class="card-body text-center">
                                    <i class="bi bi-share display-4 text-success"></i>
                                    <h5 class="mt-2">Share Notes</h5>
                                    <p class="text-muted">Share notes via unique links, no login required to view.</p>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="card h-100">
                                <div class="card-body text-center">
                                    <i class="bi bi-star-fill display-4 text-warning"></i>
                                    <h5 class="mt-2">Rate Notes</h5>
                                    <p class="text-muted">Rate and discover the best public notes from the community.</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
        </body>
        </html>
    </c:otherwise>
</c:choose>
