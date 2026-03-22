<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${note.title}"/> - Shared Note</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<nav class="navbar navbar-dark bg-primary">
    <div class="container">
        <span class="navbar-brand">📝 Loose Notes — Shared Note</span>
        <a href="${pageContext.request.contextPath}/login" class="btn btn-outline-light btn-sm">Log In</a>
    </div>
</nav>
<main class="container mt-4">
    <h1 class="h2 mb-1"><c:out value="${note.title}"/></h1>
    <small class="text-muted">
        By <strong><c:out value="${note.authorUsername}"/></strong>
        &bull; <c:out value="${note.createdAt}"/>
    </small>

    <div class="card mt-3 mb-4">
        <div class="card-body">
            <pre class="note-content"><c:out value="${note.content}"/></pre>
        </div>
    </div>

    <c:if test="${not empty attachments}">
    <h5>Attachments</h5>
    <ul class="list-group mb-4">
        <c:forEach var="a" items="${attachments}">
            <li class="list-group-item">
                📎 <c:out value="${a.originalFilename}"/>
                <small class="text-muted">(login required to download)</small>
            </li>
        </c:forEach>
    </ul>
    </c:if>

    <c:if test="${not empty ratings}">
    <h5>Rating</h5>
    <div class="list-group mb-3">
        <c:forEach var="r" items="${ratings}">
            <div class="list-group-item">
                <div class="d-flex justify-content-between">
                    <strong><c:out value="${r.raterUsername}"/></strong>
                    <span><c:forEach begin="1" end="${r.stars}" var="i">⭐</c:forEach></span>
                </div>
                <c:if test="${not empty r.comment}">
                    <small><c:out value="${r.comment}"/></small>
                </c:if>
            </div>
        </c:forEach>
    </div>
    </c:if>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
