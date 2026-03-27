<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${not empty pageTitle ? pageTitle : 'Loose Notes'}</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}/">
            <i class="bi bi-journal-text"></i> Loose Notes
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav me-auto">
                <c:if test="${not empty sessionScope.currentUser}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/notes">
                            <i class="bi bi-journals"></i> My Notes
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/search">
                            <i class="bi bi-search"></i> Search
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/top-rated">
                            <i class="bi bi-star-fill"></i> Top Rated
                        </a>
                    </li>
                    <c:if test="${sessionScope.role == 'ADMIN'}">
                        <li class="nav-item">
                            <a class="nav-link" href="${pageContext.request.contextPath}/admin">
                                <i class="bi bi-shield-check"></i> Admin
                            </a>
                        </li>
                    </c:if>
                </c:if>
                <c:if test="${empty sessionScope.currentUser}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/top-rated">
                            <i class="bi bi-star-fill"></i> Top Rated
                        </a>
                    </li>
                </c:if>
            </ul>
            <ul class="navbar-nav">
                <c:if test="${not empty sessionScope.currentUser}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/profile">
                            <i class="bi bi-person-circle"></i> ${sessionScope.username}
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/logout">
                            <i class="bi bi-box-arrow-right"></i> Logout
                        </a>
                    </li>
                </c:if>
                <c:if test="${empty sessionScope.currentUser}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/login">
                            <i class="bi bi-box-arrow-in-right"></i> Login
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/register">
                            <i class="bi bi-person-plus"></i> Register
                        </a>
                    </li>
                </c:if>
            </ul>
        </div>
    </div>
</nav>

<div class="container mt-4">
    <c:if test="${not empty sessionScope.flash_success}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <i class="bi bi-check-circle"></i> ${sessionScope.flash_success}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <c:remove var="flash_success" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.flash_error}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="bi bi-exclamation-triangle"></i> ${sessionScope.flash_error}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <c:remove var="flash_error" scope="session"/>
    </c:if>
