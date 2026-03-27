<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${not empty pageTitle ? pageTitle.concat(' - Loose Notes') : 'Loose Notes'}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/site.css" rel="stylesheet">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/">
            <i class="bi bi-journal-text"></i> Loose Notes
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/search">
                        <i class="bi bi-search"></i> Search
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/top-rated">
                        <i class="bi bi-star"></i> Top Rated
                    </a>
                </li>
                <c:if test="${not empty sessionScope.currentUser}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/notes">
                            <i class="bi bi-files"></i> My Notes
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/notes/create">
                            <i class="bi bi-plus-circle"></i> New Note
                        </a>
                    </li>
                    <c:if test="${sessionScope.currentUser.admin}">
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
                                <i class="bi bi-shield-check"></i> Admin
                            </a>
                            <ul class="dropdown-menu">
                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin">
                                    <i class="bi bi-speedometer2"></i> Dashboard
                                </a></li>
                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/users">
                                    <i class="bi bi-people"></i> Users
                                </a></li>
                            </ul>
                        </li>
                    </c:if>
                </c:if>
            </ul>
            <ul class="navbar-nav">
                <c:choose>
                    <c:when test="${not empty sessionScope.currentUser}">
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
                                <i class="bi bi-person-circle"></i>
                                ${sessionScope.currentUser.username}
                            </a>
                            <ul class="dropdown-menu dropdown-menu-end">
                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/profile">
                                    <i class="bi bi-person-gear"></i> Profile
                                </a></li>
                                <li><hr class="dropdown-divider"></li>
                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/logout">
                                    <i class="bi bi-box-arrow-right"></i> Logout
                                </a></li>
                            </ul>
                        </li>
                    </c:when>
                    <c:otherwise>
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
                    </c:otherwise>
                </c:choose>
            </ul>
        </div>
    </div>
</nav>
<div class="container mt-4">
