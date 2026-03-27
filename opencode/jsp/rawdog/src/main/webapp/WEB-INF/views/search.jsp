<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="header.jsp"/>

<h2 class="mb-4"><i class="fas fa-search"></i> Search Notes</h2>

<form action="${pageContext.request.contextPath}/search" method="get" class="mb-4">
    <div class="input-group">
        <input type="text" class="form-control" name="q" placeholder="Search by title or content..." 
               value="${query}" required autofocus>
        <div class="input-group-append">
            <button type="submit" class="btn btn-primary">
                <i class="fas fa-search"></i> Search
            </button>
        </div>
    </div>
</form>

<c:if test="${not empty query}">
    <c:if test="${empty results}">
        <div class="alert alert-info text-center">
            <i class="fas fa-info-circle fa-2x mb-3"></i>
            <h4>No notes found</h4>
            <p>No notes matching "${query}" were found.</p>
        </div>
    </c:if>
    
    <c:if test="${not empty results}">
        <div class="mb-3">
            <p class="text-muted">Found ${results.size()} note(s) matching "${query}"</p>
        </div>
        
        <div class="row">
            <c:forEach var="note" items="${results}">
                <div class="col-md-6 mb-4">
                    <div class="card h-100 shadow-sm">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start">
                                <h5 class="card-title">${note.title}</h5>
                                <span class="badge badge-${note.isPublic ? 'success' : 'secondary'}">
                                    ${note.isPublic ? 'Public' : 'Private'}
                                </span>
                            </div>
                            <p class="card-text text-muted">${note.excerpt}</p>
                            <div class="d-flex justify-content-between align-items-center">
                                <small class="text-muted">
                                    <i class="fas fa-user"></i> ${note.owner.username}
                                </small>
                                <small class="text-muted">
                                    ${note.createdAt}
                                </small>
                            </div>
                            <c:if test="${note.ratingCount > 0}">
                                <div class="mt-2">
                                    <span class="text-warning">
                                        <i class="fas fa-star"></i> ${String.format("%.1f", note.averageRating)} (${note.ratingCount} ratings)
                                    </span>
                                </div>
                            </c:if>
                        </div>
                        <div class="card-footer bg-white">
                            <a href="${pageContext.request.contextPath}/notes?action=view&id=${note.id}" class="btn btn-sm btn-outline-primary btn-block">
                                <i class="fas fa-eye"></i> View Note
                            </a>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:if>
</c:if>

<jsp:include page="footer.jsp"/>
