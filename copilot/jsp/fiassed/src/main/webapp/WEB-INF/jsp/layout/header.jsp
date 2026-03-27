<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="_csrf" content="${sessionScope.csrfToken}">
    <title><c:out value="${pageTitle != null ? pageTitle : 'Loose Notes'}"/></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<nav class="navbar">
    <div class="nav-brand"><a href="${pageContext.request.contextPath}/notes/list">📝 Loose Notes</a></div>
    <div class="nav-links">
        <c:choose>
            <c:when test="${sessionScope.userId != null}">
                <a href="${pageContext.request.contextPath}/notes/list">My Notes</a>
                <a href="${pageContext.request.contextPath}/notes/create">New Note</a>
                <a href="${pageContext.request.contextPath}/notes/search">Search</a>
                <a href="${pageContext.request.contextPath}/top-rated">Top Rated</a>
                <a href="${pageContext.request.contextPath}/profile">Profile (<c:out value="${sessionScope.username}"/>)</a>
                <c:if test="${sessionScope.role == 'ADMIN'}">
                    <a href="${pageContext.request.contextPath}/admin/dashboard">Admin</a>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/auth/logout" style="display:inline">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <button type="submit" class="btn-link">Logout</button>
                </form>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/auth/login">Login</a>
                <a href="${pageContext.request.contextPath}/auth/register">Register</a>
                <a href="${pageContext.request.contextPath}/top-rated">Top Rated</a>
            </c:otherwise>
        </c:choose>
    </div>
</nav>
<main class="container">
