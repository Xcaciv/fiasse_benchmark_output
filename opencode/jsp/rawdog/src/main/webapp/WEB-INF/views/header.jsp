<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:choose><c:when test="${not empty pageTitle}">${pageTitle} - Loose Notes</c:when><c:otherwise>Loose Notes</c:otherwise></c:choose></title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" href="${pageContext.request.contextPath}/">
                <i class="fas fa-sticky-note"></i> Loose Notes
            </a>
            <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav ml-auto">
                    <c:if test="${not empty sessionScope.loggedInUser}">
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/notes"><i class="fas fa-file-alt"></i> My Notes</a></li>
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/search"><i class="fas fa-search"></i> Search</a></li>
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/top-rated"><i class="fas fa-star"></i> Top Rated</a></li>
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle" href="#" id="userDropdown" data-toggle="dropdown">
                                <i class="fas fa-user"></i> ${sessionScope.loggedInUser.username}
                            </a>
                            <div class="dropdown-menu dropdown-menu-right">
                                <a class="dropdown-item" href="${pageContext.request.contextPath}/profile"><i class="fas fa-cog"></i> Profile</a>
                                <c:if test="${sessionScope.userRole == 'ADMIN'}">
                                    <a class="dropdown-item" href="${pageContext.request.contextPath}/admin"><i class="fas fa-shield-alt"></i> Admin</a>
                                </c:if>
                                <div class="dropdown-divider"></div>
                                <a class="dropdown-item" href="${pageContext.request.contextPath}/auth?action=logout"><i class="fas fa-sign-out-alt"></i> Logout</a>
                            </div>
                        </li>
                    </c:if>
                    <c:if test="${empty sessionScope.loggedInUser}">
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/"><i class="fas fa-home"></i> Home</a></li>
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/top-rated"><i class="fas fa-star"></i> Top Rated</a></li>
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/auth"><i class="fas fa-sign-in-alt"></i> Login</a></li>
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/auth?action=register"><i class="fas fa-user-plus"></i> Register</a></li>
                    </c:if>
                </ul>
            </div>
        </div>
    </nav>
    <main class="container mt-4">
