<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:include page="../layout.jsp">
    <jsp:param name="pageTitle" value="Search Results"/>
</jsp:include>
<jsp:body>
<div class="row mb-4">
    <div class="col-md-8">
        <h2>Search Results</h2>
        <p class="text-muted">Showing results for: "<strong>${keyword}</strong>"</p>
    </div>
    <div class="col-md-4">
        <form method="get" action="${pageContext.request.contextPath}/notes" class="d-flex">
            <input type="hidden" name="action" value="search">
            <input type="text" class="form-control me-2" name="keyword" 
                   value="${keyword}" placeholder="Search notes...">
            <button type="submit" class="btn btn-outline-primary">Search</button>
        </form>
    </div>
</div>

<c:if test="${empty notes}">
    <div class="text-center py-5">
        <i class="bi bi-search" style="font-size: 4rem; color: #6c757d;"></i>
        <h4 class="mt-3 text-muted">No results found</h4>
        <p class="text-muted">Try different keywords or create a new note.</p>
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
                        <span>By ${note.owner.username}</span>
                        <span><fmt:formatDate value="${note.createdAt}" pattern="MMM d, yyyy"/></span>
                    </div>
                </div>
                <div class="card-footer bg-white border-top-0">
                    <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" 
                       class="btn btn-outline-primary btn-sm w-100">View Note</a>
                </div>
            </div>
        </div>
    </c:forEach>
</div>
</jsp:body>
