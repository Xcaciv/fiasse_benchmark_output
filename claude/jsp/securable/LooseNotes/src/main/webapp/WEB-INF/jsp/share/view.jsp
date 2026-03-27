<%--
  Public share view - no login required.
  SSEM: Confidentiality - no user-specific information leaked here.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="${note.title} - Shared Note"/>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${pageTitle}"/></title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
          integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"
          crossorigin="anonymous">
    <%-- No-index to prevent search engine caching of private shared notes --%>
    <meta name="robots" content="noindex, nofollow">
</head>
<body>
<nav class="navbar navbar-dark bg-dark">
    <div class="container">
        <span class="navbar-brand">📝 Loose Notes &mdash; Shared Note</span>
        <a href="${pageContext.request.contextPath}/auth/login" class="btn btn-outline-light btn-sm">
            Sign In
        </a>
    </div>
</nav>
<div class="container mt-4">
    <div class="row justify-content-center">
        <div class="col-md-9">
            <h1 class="h2 mb-1"><c:out value="${note.title}"/></h1>
            <p class="text-muted mb-3">
                Shared by <strong><c:out value="${note.ownerUsername}"/></strong>
            </p>

            <div class="card mb-4">
                <div class="card-body">
                    <pre style="white-space:pre-wrap;font-family:inherit;"><c:out value="${note.content}"/></pre>
                </div>
            </div>

            <c:if test="${not empty note.attachments}">
                <h4 class="h6">Attachments</h4>
                <ul class="list-group mb-4">
                    <c:forEach var="att" items="${note.attachments}">
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            <span>
                                <c:out value="${att.originalFilename}"/>
                                <small class="text-muted">(<c:out value="${att.formattedFileSize}"/>)</small>
                            </span>
                            <a href="${pageContext.request.contextPath}/attachments/download/<c:out value='${att.id}'/>?share=true"
                               class="btn btn-sm btn-outline-secondary">Download</a>
                        </li>
                    </c:forEach>
                </ul>
            </c:if>

            <div class="alert alert-info small">
                <strong>Note:</strong> This is a shared view. Sign in to create your own notes,
                rate notes, and access more features.
            </div>
        </div>
    </div>
</div>
</body>
</html>
