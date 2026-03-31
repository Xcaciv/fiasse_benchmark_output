<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp"/>

<div class="container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="h2">My Notes</h1>
        <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary">
            <i class="bi bi-plus-lg"></i> New Note
        </a>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <c:out value="${successMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <c:out value="${errorMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${empty notes}">
            <div class="text-center py-5">
                <p class="text-muted fs-5">You have no notes yet.</p>
                <a href="${pageContext.request.contextPath}/notes/create" class="btn btn-primary mt-2">Create your first note</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead class="table-light">
                        <tr>
                            <th scope="col">Title</th>
                            <th scope="col">Visibility</th>
                            <th scope="col">Created</th>
                            <th scope="col" class="text-end">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="note" items="${notes}">
                            <tr>
                                <td>
                                    <a href="${pageContext.request.contextPath}/notes/${note.id}" class="text-decoration-none fw-semibold">
                                        <c:out value="${note.title}"/>
                                    </a>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${note.visibility == 'PUBLIC'}">
                                            <span class="badge bg-success">PUBLIC</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary">PRIVATE</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <fmt:formatDate value="${note.createdAt}" pattern="MMM dd, yyyy"/>
                                </td>
                                <td class="text-end">
                                    <a href="${pageContext.request.contextPath}/notes/${note.id}/edit"
                                       class="btn btn-sm btn-outline-primary me-1">Edit</a>
                                    <form method="post"
                                          action="${pageContext.request.contextPath}/notes/${note.id}/delete"
                                          class="d-inline"
                                          onsubmit="return confirm('Are you sure you want to delete this note? This action cannot be undone.');">
                                        <input type="hidden" name="_csrf" value="${csrfToken}">
                                        <button type="submit" class="btn btn-sm btn-outline-danger">Delete</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp"/>
