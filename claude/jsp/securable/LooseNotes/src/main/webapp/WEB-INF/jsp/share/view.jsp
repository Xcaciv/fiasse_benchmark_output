<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title><c:out value="${note.title}"/> &mdash; LooseNotes</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
          integrity="sha384-9ndCyUaIbzAi2FUVXJi0CjmCapSmO7SnpJef0486qhLnuZ2cdeRhO02iuK6FUUVM"
          crossorigin="anonymous">
    <style>
        body {
            background-color: #f8f9fa;
        }
        .note-card {
            max-width: 800px;
            margin: 0 auto;
        }
        .note-content {
            white-space: pre-wrap;
            word-wrap: break-word;
            font-family: inherit;
            line-height: 1.7;
        }
        .brand-link {
            text-decoration: none;
            color: inherit;
        }
    </style>
</head>
<body>
    <nav class="navbar navbar-light bg-white border-bottom">
        <div class="container">
            <a class="navbar-brand brand-link fw-bold" href="${pageContext.request.contextPath}/">
                LooseNotes
            </a>
            <a href="${pageContext.request.contextPath}/account/login" class="btn btn-outline-primary btn-sm">Sign In</a>
        </div>
    </nav>

    <main class="container py-5">
        <div class="note-card">
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger" role="alert">
                    <c:out value="${errorMessage}"/>
                </div>
            </c:if>

            <c:choose>
                <c:when test="${empty note}">
                    <div class="text-center py-5">
                        <h2 class="h4 text-muted">Note not found</h2>
                        <p class="text-muted">This share link may have expired or been revoked.</p>
                        <a href="${pageContext.request.contextPath}/" class="btn btn-outline-primary mt-2">Go Home</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="card shadow-sm mb-4">
                        <div class="card-body p-4 p-md-5">
                            <h1 class="h2 mb-3"><c:out value="${note.title}"/></h1>

                            <div class="d-flex align-items-center gap-3 text-muted small mb-4 pb-3 border-bottom">
                                <span>By <strong><c:out value="${note.authorUsername}"/></strong></span>
                                <span>&middot;</span>
                                <span><fmt:formatDate value="${note.createdAt}" pattern="MMMM dd, yyyy"/></span>
                                <span class="badge bg-success ms-auto">PUBLIC</span>
                            </div>

                            <div class="note-content">
                                <c:out value="${note.content}"/>
                            </div>
                        </div>
                    </div>

                    <%-- Attachments --%>
                    <c:if test="${not empty attachments}">
                        <div class="card shadow-sm mb-4">
                            <div class="card-header bg-white">
                                <h5 class="card-title mb-0">Attachments</h5>
                            </div>
                            <ul class="list-group list-group-flush">
                                <c:forEach var="attachment" items="${attachments}">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span class="text-muted">
                                            <c:out value="${attachment.originalFilename}"/>
                                        </span>
                                        <a href="${pageContext.request.contextPath}/attachments/${attachment.id}"
                                           class="btn btn-sm btn-outline-secondary">Download</a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>

                    <div class="text-center mt-4">
                        <p class="text-muted small">
                            Shared via <a href="${pageContext.request.contextPath}/" class="text-decoration-none">LooseNotes</a>.
                            <a href="${pageContext.request.contextPath}/account/register" class="ms-1">Create your own notes &rarr;</a>
                        </p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </main>

    <footer class="border-top bg-white mt-5 py-3">
        <div class="container text-center text-muted small">
            &copy; LooseNotes &mdash; <a href="${pageContext.request.contextPath}/" class="text-muted">Home</a>
        </div>
    </footer>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"
            integrity="sha384-geWF76RCwLtnZ8qwWowPQNguL3RmwHVBC9FhGdlKrxdiJJigb/j/68SIy3Te4Bkz"
            crossorigin="anonymous"></script>
</body>
</html>
