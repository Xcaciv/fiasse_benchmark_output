<%--
  Common page header and navbar.
  SSEM: Integrity - CSRF token injected into all state-changing forms.
  SSEM: Analyzability - single header template for consistency.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${pageTitle != null ? pageTitle : 'Loose Notes'}"/></title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
          integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"
          crossorigin="anonymous">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/site.css">
    <!-- SSEM: Authenticity - CSRF meta tag for AJAX requests -->
    <meta name="csrf-token" content="<c:out value='${csrfToken}'/>">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}/notes">
            📝 Loose Notes
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav me-auto">
                <c:if test="${currentUser != null}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/notes">My Notes</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/notes/create">New Note</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/notes/search">Search</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/notes/top-rated">Top Rated</a>
                    </li>
                    <c:if test="${currentUser.admin}">
                        <li class="nav-item">
                            <a class="nav-link text-warning" href="${pageContext.request.contextPath}/admin">Admin</a>
                        </li>
                    </c:if>
                </c:if>
            </ul>
            <ul class="navbar-nav">
                <c:choose>
                    <c:when test="${currentUser != null}">
                        <li class="nav-item">
                            <a class="nav-link" href="${pageContext.request.contextPath}/profile/edit">
                                <c:out value="${currentUser.username}"/>
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="${pageContext.request.contextPath}/auth/logout">Logout</a>
                        </li>
                    </c:when>
                    <c:otherwise>
                        <li class="nav-item">
                            <a class="nav-link" href="${pageContext.request.contextPath}/auth/login">Login</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="${pageContext.request.contextPath}/auth/register">Register</a>
                        </li>
                    </c:otherwise>
                </c:choose>
            </ul>
        </div>
    </div>
</nav>
<div class="container mt-4">
