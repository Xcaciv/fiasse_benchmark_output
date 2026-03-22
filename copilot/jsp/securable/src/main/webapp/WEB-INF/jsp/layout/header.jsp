<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${pageTitle != null ? pageTitle : 'Loose Notes'}"/></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/notes">📝 Loose Notes</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navMenu">
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/notes">My Notes</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/search">Search</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/top-rated">Top Rated</a>
                </li>
                <c:if test="${sessionScope.userRole == 'ADMIN'}">
                <li class="nav-item">
                    <a class="nav-link text-warning" href="${pageContext.request.contextPath}/admin">Admin</a>
                </li>
                </c:if>
            </ul>
            <ul class="navbar-nav">
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/profile">
                        👤 <c:out value="${sessionScope.username}"/>
                    </a>
                </li>
                <li class="nav-item">
                    <form method="post" action="${pageContext.request.contextPath}/logout" class="d-inline">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrf_token}">
                        <button type="submit" class="btn btn-outline-light btn-sm ms-2">Logout</button>
                    </form>
                </li>
            </ul>
        </div>
    </div>
</nav>
<main class="container mt-4">
