<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${note.title}"/> — Loose Notes Shared View</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="wrapper">
    <nav class="navbar navbar-minimal">
        <span class="nav-logo">Loose Notes</span>
    </nav>

    <main class="main-content">
        <div class="alert alert-info shared-note-banner">
            This note was shared via a public link.
        </div>

        <article class="note-view">
            <div class="note-header">
                <h1><c:out value="${note.title}"/></h1>
            </div>

            <div class="note-content">
                <pre><c:out value="${note.content}"/></pre>
            </div>

            <c:if test="${not empty attachments}">
                <section class="note-section">
                    <h2>Attachments</h2>
                    <ul class="attachment-list">
                        <c:forEach var="attachment" items="${attachments}">
                            <li>
                                <a href="${pageContext.request.contextPath}/share/${shareToken}/attachment/${attachment.id}">
                                    <c:out value="${attachment.originalFilename}"/>
                                </a>
                            </li>
                        </c:forEach>
                    </ul>
                </section>
            </c:if>
        </article>
    </main>
</div>
<footer class="site-footer">
    <p>&copy; 2026 Loose Notes</p>
</footer>
</body>
</html>
