<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${pageTitle != null ? pageTitle : 'Loose Notes'}"/></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="wrapper">
    <nav class="navbar">
        <a class="nav-logo" href="${pageContext.request.contextPath}/notes">Loose Notes</a>
        <div class="nav-links">
            <c:choose>
                <c:when test="${not empty sessionScope.userId}">
                    <a href="${pageContext.request.contextPath}/notes">Notes</a>
                    <a href="${pageContext.request.contextPath}/search">Search</a>
                    <a href="${pageContext.request.contextPath}/notes/top-rated">Top Rated</a>
                    <c:if test="${sessionScope.userRole == 'ADMIN'}">
                        <a href="${pageContext.request.contextPath}/admin">Admin</a>
                    </c:if>
                    <span class="nav-username">
                        <c:out value="${sessionScope.username}"/>
                    </span>
                    <a href="${pageContext.request.contextPath}/profile">Profile</a>
                    <form method="post" action="${pageContext.request.contextPath}/auth/logout" class="nav-logout-form">
                        <input type="hidden" name="_csrf" value="${csrfToken}"/>
                        <button type="submit" class="btn-link">Logout</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/auth/login">Login</a>
                    <a href="${pageContext.request.contextPath}/auth/register">Register</a>
                </c:otherwise>
            </c:choose>
        </div>
    </nav>

    <main class="main-content">
        <c:if test="${not empty error}">
            <div class="alert alert-error">
                <c:out value="${error}"/>
            </div>
        </c:if>
        <c:if test="${not empty message}">
            <div class="alert alert-info">
                <c:out value="${message}"/>
            </div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success">
                <c:out value="${success}"/>
            </div>
        </c:if>
