<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><c:out value="${pageTitle != null ? pageTitle : 'Loose Notes'}"/></title>
  <!-- Bootstrap 5 CSS from CDN (allowed by CSP) -->
  <link rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
        integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"
        crossorigin="anonymous">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  <div class="container">
    <a class="navbar-brand" href="${pageContext.request.contextPath}/home">Loose Notes</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
            data-bs-target="#navMenu" aria-controls="navMenu"
            aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navMenu">
      <ul class="navbar-nav me-auto">
        <li class="nav-item">
          <a class="nav-link" href="${pageContext.request.contextPath}/home">My Notes</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="${pageContext.request.contextPath}/notes/create">New Note</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="${pageContext.request.contextPath}/search">Search</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="${pageContext.request.contextPath}/top-rated">Top Rated</a>
        </li>
        <c:if test="${sessionScope.role == 'ADMIN'}">
          <li class="nav-item">
            <a class="nav-link text-warning" href="${pageContext.request.contextPath}/admin/dashboard">Admin</a>
          </li>
        </c:if>
      </ul>
      <ul class="navbar-nav ms-auto">
        <c:choose>
          <c:when test="${not empty sessionScope.userId}">
            <li class="nav-item">
              <a class="nav-link" href="${pageContext.request.contextPath}/profile">
                <c:out value="${sessionScope.username}"/>
              </a>
            </li>
            <li class="nav-item">
              <form method="post" action="${pageContext.request.contextPath}/logout" class="d-inline">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                <button class="btn btn-sm btn-outline-light nav-link">Logout</button>
              </form>
            </li>
          </c:when>
          <c:otherwise>
            <li class="nav-item">
              <a class="nav-link" href="${pageContext.request.contextPath}/login">Login</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="${pageContext.request.contextPath}/register">Register</a>
            </li>
          </c:otherwise>
        </c:choose>
      </ul>
    </div>
  </div>
</nav>
<main class="container mt-4">
