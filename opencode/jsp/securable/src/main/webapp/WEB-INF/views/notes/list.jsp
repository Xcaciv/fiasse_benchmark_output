<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:include page="../layout.jsp">
    <jsp:param name="pageTitle" value="My Notes"/>
</jsp:include>
<jsp:body>
<div class="row mb-4">
    <div class="col-md-8">
        <h2>My Notes</h2>
    </div>
    <div class="col-md-4">
        <form method="get" action="${pageContext.request.contextPath}/notes" class="d-flex">
            <input type="hidden" name="action" value="search">
            <input type="text" class="form-control me-2" name="keyword" placeholder="Search notes..." 
                   value="${param.keyword}">
            <button type="submit" class="btn btn-outline-primary">Search</button>
        </form>
    </div>
</div>

<c:if test="${empty notes}">
    <div class="text-center py-5">
        <i class="bi bi-journal-text" style="font-size: 4rem; color: #6c757d;"></i>
        <h4 class="mt-3 text-muted">No notes yet</h4>
        <p class="text-muted">Create your first note to get started!</p>
        <a href="${pageContext.request.contextPath}/notes?action=new" class="btn btn-primary">
            <i class="bi bi-plus-circle"></i> Create Note
        </a>
    </div>
</c:if>

<div class="row">
    <c:forEach var="note" items="${notes}">
        <div class="col-md-6 col-lg-4 mb-4">
            <div class="card note-card h-100">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <h5 class="card-title mb-0">
                            <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" 
                               class="text-decoration-none text-dark">${note.title}</a>
                        </h5>
                        <c:if test="${note.public}">
                            <span class="badge bg-success">Public</span>
                        </c:if>
                    </div>
                    <p class="note-excerpt">${note.excerpt}</p>
                    <div class="d-flex justify-content-between align-items-center text-muted small">
                        <span><i class="bi bi-calendar"></i> 
                            <fmt:formatDate value="${note.createdAt}" pattern="MMM d, yyyy"/></span>
                        <c:if test="${note.ratingCount > 0}">
                            <span class="star-rating">
                                <i class="bi bi-star-fill"></i> ${String.format("%.1f", note.averageRating)}
                            </span>
                        </c:if>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0">
                    <div class="btn-group btn-group-sm w-100">
                        <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" 
                           class="btn btn-outline-primary">View</a>
                        <a href="${pageContext.request.contextPath}/notes?action=edit&id=${note.id}" 
                           class="btn btn-outline-secondary">Edit</a>
                        <a href="${pageContext.request.contextPath}/notes?action=delete&id=${note.id}" 
                           class="btn btn-outline-danger"
                           onclick="return confirm('Are you sure you want to delete this note?')">Delete</a>
                    </div>
                </div>
            </div>
        </div>
    </c:forEach>
</div>
</jsp:body>
